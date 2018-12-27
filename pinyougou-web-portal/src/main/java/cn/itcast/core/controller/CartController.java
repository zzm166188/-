package cn.itcast.core.controller;

import cn.itcast.core.pojo.Cart;
import cn.itcast.core.pojo.item.Item;
import cn.itcast.core.pojo.order.OrderItem;
import cn.itcast.core.service.CartService;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import entity.Result;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 购物车管理
 */
@RestController
@RequestMapping("/cart")
public class CartController {


    @Reference
    private CartService cartService;
    //加入购物车
    @RequestMapping("/addGoodsToCartList")
   /* @CrossOrigin(origins = {"http://localhost:9003"},allowCredentials = "true")*/
    @CrossOrigin(origins = {"http://localhost:9003"})
    public Result addGoodsToCartList(Long itemId, Integer num, HttpServletResponse response, HttpServletRequest request){

        try {
            //购物车结果集
            List<Cart> cartList = null;


            /*1:获取Cookie 数组*/
            Cookie[] cookies = request.getCookies();
            if(null != cookies && cookies.length > 0){
                /*2:从Cookie数组中获取购物车*/
                for (Cookie cookie : cookies) {
                    //判断是不是购物车  K:CART V:[{},{}]
                    if("CART".equals(cookie.getName())){
                        cartList = JSON.parseArray(cookie.getValue(),Cart.class);
                        break;
                    }
                }
            }

            /*3:没有 创建购物车*/
            if(null == cartList){
                cartList = new ArrayList<>();
            }

            /*4;追加当前款*/

            Cart newCart = new Cart();//当前款购物车(商家ID,库存Id ,数量)
            //根据库存ID查询库存对象
            Item item = cartService.findItemById(itemId);
            //商家ID
            newCart.setSellerId(item.getSellerId());
            //商家名称 (不写)

            //商品结果集
            List<OrderItem> newOrderItemList = new ArrayList<>();
            //商品对象
            OrderItem newOrderItem = new OrderItem();
            //库存ID
            newOrderItem.setItemId(itemId);
            //数量
            newOrderItem.setNum(num);

            //添加到结果集中
            newOrderItemList.add(newOrderItem);
            //添加到购物车中
            newCart.setOrderItemList(newOrderItemList);
            //1:判断当前款的商家ID  商家结果集中是否有
            int newIndex = cartList.indexOf(newCart);//结果集 indexOf 一个 是否存在 如果存在 返回值 角标 -1
            if(newIndex != -1){
                //-有 从商家结果集中找出 跟当前款的商家ID是一致的那个购物车对象
                Cart oldCart = cartList.get(newIndex);
                //此购物车对象中商品结果集中是否包含当前款商品
                List<OrderItem> oldOrderItemList = oldCart.getOrderItemList();//
                int newOrderItemIndexOf = oldOrderItemList.indexOf(newOrderItem);
                if(newOrderItemIndexOf != -1){
                    //--有  追加数量
                    OrderItem oldOrderItem = oldOrderItemList.get(newOrderItemIndexOf);
                    oldOrderItem.setNum(newOrderItem.getNum() + oldOrderItem.getNum());
                }else{
                    //--没有  当新款添加一个
                    oldOrderItemList.add(newOrderItem);
                }
            }else{
                //-没有 作为新的商家的商品添加到购物车集合中
                cartList.add(newCart);
            }

            //判断是否登陆 解决这件事? 会选择 用抛异常来解决 下下策
            String name = SecurityContextHolder.getContext().getAuthentication().getName();//空指针
            if(!"anonymousUser".equals(name)){
                //登陆了
                //5:将当前购物车合并到Redis中
                cartService.merge(cartList,name);
                //6:清空Cookie
                Cookie cookie = new Cookie("CART",null);
                cookie.setMaxAge(0);
                cookie.setPath("/");
                response.addCookie(cookie);
            }else{
                //未登陆
                /* 5:将当前购物车再添加到Cookie*/
                Cookie cookie = new Cookie("CART",JSON.toJSONString(cartList));
                cookie.setMaxAge(60*60*24);
                //cookie.setDomain("jd.com");
                //  http://www.jd.com/cart/addGoodsToCart.do
                //  http://search.jd.com/shopping/addGoodsToCart.do
                cookie.setPath("/");
                /* 6:回写浏览器*/
                response.addCookie(cookie);



            }
            return new Result(true,"加入购物车成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false,"加入购物车失败");
        }

    }
    //查询购物车结果集
    @RequestMapping("/findCartList")
    public List<Cart> findCartList(HttpServletRequest request,HttpServletResponse response){


        List<Cart> cartList = null;

        //1:获取Cookie 数组
        Cookie[] cookies = request.getCookies();
        if(null != cookies && cookies.length > 0){
            //2:获取Cookie中的购物车
            for (Cookie cookie : cookies) {
                //判断是不是购物车  K:CART V:[{},{}]
                if("CART".equals(cookie.getName())){
                    cartList = JSON.parseArray(cookie.getValue(),Cart.class);
                    break;
                }
            }
        }


        //判断是否登陆
        String name = SecurityContextHolder.getContext().getAuthentication().getName();//空指针
        if(!"anonymousUser".equals(name)){
            //登陆了
            //3:有 将Cookie中购物车合并到Redis中
            if(null != cartList){
                cartService.merge(cartList,name);
                //清空Cookie
                Cookie cookie = new Cookie("CART",null);
                cookie.setMaxAge(0);
                cookie.setPath("/");
                response.addCookie(cookie);
            }
            //4:从Redis缓存中查出购物车
            cartList = cartService.findCartListFromRedis(name);

        }
        //5:有 将此购物车装满
        if(null != cartList){
            cartList = cartService.findCartList(cartList);
        }
        //6:回显
        return cartList;
    }
}
