package cn.itcast.core.service;

import cn.itcast.core.mapper.ad.ContentDao;
import cn.itcast.core.pojo.ad.Content;
import cn.itcast.core.pojo.ad.ContentQuery;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class ContentServiceImpl implements ContentService {

    @Autowired
    private ContentDao contentDao;

    @Override
    public List<Content> findAll() {
        List<Content> list = contentDao.selectByExample(null);
        return list;
    }

    @Override
    public PageResult findPage(Content content, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<Content> page = (Page<Content>) contentDao.selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public void add(Content content) {
        contentDao.insertSelective(content);
    }

    @Autowired
    private RedisTemplate redisTemplate;// Map map.put(1,轮播图结果集)



    // Spring支持事务  不支持 利用Mysql的事务 来完成事务管理    Mysql也支持事务
    //之前 先进入切面 对象(DataSourceTransactionManager) dataSource 连接Mysql   begin transation  开启事务
    @Override
    public void edit(Content content) {


        //1:先通过广告Id 查询广告的分类ID(外键)
        Content c = contentDao.selectByPrimaryKey(content.getId());

        //Map
        // 轮播图结果集
        // 今日
        // 活动
        // 商品精选 .
        //5:修改Mysql
        contentDao.updateByPrimaryKeySelective(content);

        //2:判断查询出来原来的广告分类Id与现在的广告分类ID是否相同
        if (!c.getCategoryId().equals(content.getCategoryId())) {
            //3:不相同 删除原来的
            redisTemplate.boundHashOps("content").delete(c.getCategoryId());
        }
        //4:相不相同 都要删除现在的
        redisTemplate.boundHashOps("content").delete(content.getCategoryId());

  /*      findByCategoryId(content.getCategoryId());
        findByCategoryId(c.getCategoryId());*/
        //之后 再次进入切面 对象(DataSourceTransactionManager) dataSource 连接Mysql   rollback  提交事务

    }
    //之后 再次进入切面 对象(DataSourceTransactionManager) dataSource 连接Mysql   commit  提交事务

    @Override
    public Content findOne(Long id) {
        Content content = contentDao.selectByPrimaryKey(id);
        return content;
    }

    @Override
    public void delAll(Long[] ids) {
        if (ids != null) {
            for (Long id : ids) {
                contentDao.deleteByPrimaryKey(id);
            }
        }
    }



    //根据外键 查询轮播图
    @Override
    public List<Content> findByCategoryId(Long categoryId) {
        //1:查询缓存
        List<Content> contentList = (List<Content>) redisTemplate.boundHashOps("content").get(categoryId);
        if (null == contentList || contentList.size() == 0) {
            //3:没有 查询Mysql数据
            ContentQuery contentQuery = new ContentQuery();
            contentQuery.createCriteria().andCategoryIdEqualTo(categoryId);

            contentQuery.setOrderByClause("sort_order desc");

            contentList = contentDao.selectByExample(contentQuery);
            //4:保存缓存一份
            redisTemplate.boundHashOps("content").put(categoryId, contentList);
            redisTemplate.boundHashOps("content").expire(1, TimeUnit.DAYS);
        }
        //5:直接返回
        return contentList;

    }

}
