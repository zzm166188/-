package cn.itcast.core.service;

import cn.itcast.core.pojo.Cart;
import cn.itcast.core.pojo.item.Item;

import java.util.List;

public interface CartService {

    ////根据库存ID查询库存对象
    public Item findItemById(Long id);

    List<Cart> findCartList(List<Cart> cartList);

    void merge(List<Cart> cartList,String name);

    List<Cart> findCartListFromRedis(String name);
}
