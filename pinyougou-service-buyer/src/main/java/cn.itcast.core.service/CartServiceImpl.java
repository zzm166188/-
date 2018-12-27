package cn.itcast.core.service;

import cn.itcast.core.mapper.item.ItemDao;
import cn.itcast.core.pojo.Cart;
import cn.itcast.core.pojo.item.Item;
import cn.itcast.core.pojo.order.OrderItem;
import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车管理
 */
@Service
public class CartServiceImpl implements CartService {


    @Autowired
    private ItemDao itemDao;
    ////根据库存ID查询库存对象
    public Item findItemById(Long id){
        return itemDao.selectByPrimaryKey(id);
    }

    //将购物车装满
    @Override
    public List<Cart> findCartList(List<Cart> cartList) {


        for (Cart cart : cartList) {
            //商家ID  有了
            //商家名称  没有

            List<OrderItem> orderItemList = cart.getOrderItemList();
            for (OrderItem orderItem : orderItemList) {
                //库存ID
                Item item = findItemById(orderItem.getItemId());
                //数量
                //图片
                orderItem.setPicPath(item.getImage());
                //标题
                orderItem.setTitle(item.getTitle());
                //单价
                orderItem.setPrice(item.getPrice());
                //小计
                orderItem.setTotalFee(new BigDecimal(orderItem.getPrice().doubleValue()*orderItem.getNum()));
                //商家名称
                cart.setSellerName(item.getSeller());
            }

        }
        return cartList;
    }

    @Autowired
    private RedisTemplate redisTemplate;
    //合并到Redis中
    @Override
    public void merge(List<Cart> newCartList,String name) {

        //1:获取出原来的购物车结果集
        List<Cart> oldCartList = (List<Cart>) redisTemplate.boundHashOps("cartList").get(name);
        //2:新老车大合并
        oldCartList = merge1(newCartList,oldCartList);
        //3:合并后的老车结果集保存进缓存中
        redisTemplate.boundHashOps("cartList").put(name,oldCartList);
    }
    //新老车合并
    public List<Cart> merge1(List<Cart> newCartList,List<Cart> oldCartList){
        //判断如果老车
        if(null != oldCartList){
            if(null != newCartList){
                for (Cart newCart : newCartList) {
                    //1:判断当前款的商家ID  商家结果集中是否有
                    int newIndex = oldCartList.indexOf(newCart);//结果集 indexOf 一个 是否存在 如果存在 返回值 角标 -1
                    if(newIndex != -1){
                        //-有 从商家结果集中找出 跟当前款的商家ID是一致的那个购物车对象
                        Cart oldCart = oldCartList.get(newIndex);
                        //此购物车对象中商品结果集中是否包含当前款商品
                        List<OrderItem> oldOrderItemList = oldCart.getOrderItemList();//

                        List<OrderItem> newOrderItemList = newCart.getOrderItemList();
                        for (OrderItem newOrderItem : newOrderItemList) {
                            int newOrderItemIndexOf = oldOrderItemList.indexOf(newOrderItem);
                            if(newOrderItemIndexOf != -1){
                                //--有  追加数量
                                OrderItem oldOrderItem = oldOrderItemList.get(newOrderItemIndexOf);
                                oldOrderItem.setNum(newOrderItem.getNum() + oldOrderItem.getNum());
                            }else{
                                //--没有  当新款添加一个
                                oldOrderItemList.add(newOrderItem);
                            }
                        }
                    }else{
                        //-没有 作为新的商家的商品添加到购物车集合中
                        oldCartList.add(newCart);
                    }
                }

            }
        }else{
            //返回新车
            return newCartList;
        }
        //返回老车
        return oldCartList;
    }

    @Override
    public List<Cart> findCartListFromRedis(String name) {
        return (List<Cart>) redisTemplate.boundHashOps("cartList").get(name);
    }
}
