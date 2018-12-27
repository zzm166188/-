package cn.itcast.core.service;

import cn.itcast.core.mapper.good.BrandDao;
import cn.itcast.core.mapper.good.GoodsDao;
import cn.itcast.core.mapper.good.GoodsDescDao;
import cn.itcast.core.mapper.item.ItemCatDao;
import cn.itcast.core.mapper.item.ItemDao;
import cn.itcast.core.mapper.seller.SellerDao;
import cn.itcast.core.pojo.good.Brand;
import cn.itcast.core.pojo.good.Goods;
import cn.itcast.core.pojo.good.GoodsQuery;
import cn.itcast.core.pojo.item.Item;
import cn.itcast.core.pojo.item.ItemCat;
import cn.itcast.core.pojo.item.ItemQuery;
import cn.itcast.core.pojo.seller.Seller;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.transaction.annotation.Transactional;
import pojogroup.GoodsVo;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商品管理
 */
@SuppressWarnings("ALL")
@Service
@Transactional
public class GoodsServiceImpl implements  GoodsService {
    @Autowired
    private GoodsDao goodsDao;
    @Autowired
    private GoodsDescDao goodsDescDao;
    @Autowired
    private ItemDao itemDao;
    @Autowired
    private ItemCatDao itemCatDao;
    @Autowired
    private SellerDao sellerDao;
    @Autowired
    private BrandDao brandDao;

    //三个表
    @Override
    public void add(GoodsVo vo) {
        //商品表 返回ID
        //审核状态
        vo.getGoods().setAuditStatus("0");
        //保存
        goodsDao.insertSelective(vo.getGoods());

        //商品详情表 ID
        vo.getGoodsDesc().setGoodsId(vo.getGoods().getId());
        goodsDescDao.insertSelective(vo.getGoodsDesc());

        //判断是否启用规格
        if("1".equals(vo.getGoods().getIsEnableSpec())){
            //启用
            //库存集合  外键
            List<Item> itemList = vo.getItemList();
            for (Item item : itemList) {
                //标题 = SPU名称 + 规格
                String title = vo.getGoods().getGoodsName();
                //{"机身内存":"16G","网络":"联通3G",..,...}
                String spec = item.getSpec();
                Map<String,String> specMap = JSON.parseObject(spec, Map.class);

                Set<Map.Entry<String, String>> entries = specMap.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    title += " " + entry.getValue();
                }
                item.setTitle(title);
                //保存商品第一张图片
                String itemImages = vo.getGoodsDesc().getItemImages();
                List<Map> images = JSON.parseArray(itemImages, Map.class);
                if(null != images && images.size() > 0){
                    item.setImage((String) images.get(0).get("url"));
                }
                //第三个商品分类ID
                item.setCategoryid(vo.getGoods().getCategory3Id());
                //第三个商品分类的名称
                ItemCat itemCat = itemCatDao.selectByPrimaryKey(vo.getGoods().getCategory3Id());
                item.setCategory(itemCat.getName());
                //时间
                item.setCreateTime(new Date());
                item.setUpdateTime(new Date());
                //商品表的ID
                item.setGoodsId(vo.getGoods().getId());
                //商家ID
                item.setSellerId(vo.getGoods().getSellerId());
                //商家名称
                Seller seller = sellerDao.selectByPrimaryKey(vo.getGoods().getSellerId());
                item.setSeller(seller.getName());

                //品牌名称
                Brand brand = brandDao.selectByPrimaryKey(vo.getGoods().getBrandId());
                item.setBrand(brand.getName());
                //库存表
                itemDao.insertSelective(item);
            }
        }else{
            //不启用   默认值              tb_item (默认值 )


        }



    }

    @Override
    public PageResult search(Integer page, Integer rows, Goods goods) {
        //分页插件
        PageHelper.startPage(page,rows);
        //排序
        PageHelper.orderBy("id desc");

        GoodsQuery goodsQuery = new GoodsQuery();
        GoodsQuery.Criteria criteria = goodsQuery.createCriteria();
        //条件
        if(null != goods.getAuditStatus() &&  !"".equals(goods.getAuditStatus())){
            criteria.andAuditStatusEqualTo(goods.getAuditStatus());
        }
        //名称  前端去空格 校验 都不安全 处理值
        if(null != goods.getGoodsName() && !"".equals(goods.getGoodsName().trim())){
            criteria.andGoodsNameLike("%"+goods.getGoodsName().trim()+"%");
        }
        //只查询不删除
        criteria.andIsDeleteIsNull();

        //判断 是否有商家ID　如果有：商家后台要查询商品列表　　　如果没有：运营商后台要查询商品列表
        if(null != goods.getSellerId()){
            //只查询当前商家的
            criteria.andSellerIdEqualTo(goods.getSellerId());

        }

        Page<Goods> p = (Page<Goods>) goodsDao.selectByExample(goodsQuery);

        return new PageResult(p.getTotal(),p.getResult());
    }

    @Override
    public GoodsVo findOne(Long id) {
        GoodsVo vo = new GoodsVo();
        //商品对象
        vo.setGoods(goodsDao.selectByPrimaryKey(id));
        //商品详情对象
        vo.setGoodsDesc(goodsDescDao.selectByPrimaryKey(id));
        //库存对象结果集
        ItemQuery itemQuery = new ItemQuery();
        itemQuery.createCriteria().andGoodsIdEqualTo(id);
        vo.setItemList(itemDao.selectByExample(itemQuery));

        return vo;
    }

    @Override
    public void update(GoodsVo vo) {
        //商品表
        goodsDao.updateByPrimaryKeySelective(vo.getGoods());
        //商品详情表
        goodsDescDao.updateByPrimaryKeySelective(vo.getGoodsDesc());
        //库存表
        //1:先删除
        ItemQuery itemQuery = new ItemQuery();
        itemQuery.createCriteria().andGoodsIdEqualTo(vo.getGoods().getId());
        itemDao.deleteByExample(itemQuery);
        //2:再添加
        //判断是否启用规格
        if("1".equals(vo.getGoods().getIsEnableSpec())){
            //启用
            //库存集合  外键
            List<Item> itemList = vo.getItemList();
            for (Item item : itemList) {
                //标题 = SPU名称 + 规格
                String title = vo.getGoods().getGoodsName();
                //{"机身内存":"16G","网络":"联通3G",..,...}
                String spec = item.getSpec();
                Map<String,String> specMap = JSON.parseObject(spec, Map.class);

                Set<Map.Entry<String, String>> entries = specMap.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    title += " " + entry.getValue();
                }
                item.setTitle(title);
                //保存商品第一张图片
                String itemImages = vo.getGoodsDesc().getItemImages();
                List<Map> images = JSON.parseArray(itemImages, Map.class);
                if(null != images && images.size() > 0){
                    item.setImage((String) images.get(0).get("url"));
                }
                //第三个商品分类ID
                item.setCategoryid(vo.getGoods().getCategory3Id());
                //第三个商品分类的名称
                ItemCat itemCat = itemCatDao.selectByPrimaryKey(vo.getGoods().getCategory3Id());
                item.setCategory(itemCat.getName());
                //时间
                item.setCreateTime(new Date());
                item.setUpdateTime(new Date());
                //商品表的ID
                item.setGoodsId(vo.getGoods().getId());
                //商家ID
                item.setSellerId(vo.getGoods().getSellerId());
                //商家名称
                Seller seller = sellerDao.selectByPrimaryKey(vo.getGoods().getSellerId());
                item.setSeller(seller.getName());

                //品牌名称
                Brand brand = brandDao.selectByPrimaryKey(vo.getGoods().getBrandId());
                item.setBrand(brand.getName());
                //库存表
                itemDao.insertSelective(item);
            }
        }else{
            //不启用   默认值              tb_item (默认值 )


        }

    }

    @Override
    public void delete(Long[] ids) {
        Goods goods = new Goods();
        goods.setIsDelete("1");
        //商品表ID
        for (Long id : ids) {
            goods.setId(id);
            //1:更新商品的删除字段 从null 改为1
            goodsDao.updateByPrimaryKeySelective(goods);
            //2:发消息
            jmsTemplate.send(queueSolrDeleteDestination, new MessageCreator() {
                @Override
                public Message createMessage(Session session) throws JMSException {
                    return session.createTextMessage(String.valueOf(id));
                }
            });
        }


    }



    @Autowired
    private JmsTemplate jmsTemplate;
    @Autowired
    private Destination topicPageAndSolrDestination;   //一发 多接收的   发布 订阅模式 主题模式
    @Autowired
    private Destination queueSolrDeleteDestination;  //一发 一接  点对点  队列模式

    @Override
    public void updateStatus(Long[] ids, String status) {

        Goods goods = new Goods();
        goods.setAuditStatus(status);

        //商品表的ID
        for (Long id : ids) {
            goods.setId(id);
            //1:更新审核状态
            goodsDao.updateByPrimaryKeySelective(goods);
            //只有审核通过才会完成下面二项
            if("1".equals(status)){

                //发消息
                jmsTemplate.send(topicPageAndSolrDestination, new MessageCreator() {
                    @Override
                    public Message createMessage(Session session) throws JMSException {
                        return session.createTextMessage(String.valueOf(id));
                    }
                });
            }

        }

    }

}
