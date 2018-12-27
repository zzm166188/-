package cn.itcast.core.service;

import cn.itcast.common.utils.IdWorker;
import cn.itcast.core.mapper.item.ItemDao;
import cn.itcast.core.mapper.log.PayLogDao;
import cn.itcast.core.mapper.order.OrderDao;
import cn.itcast.core.mapper.order.OrderItemDao;
import cn.itcast.core.pojo.Cart;
import cn.itcast.core.pojo.item.Item;
import cn.itcast.core.pojo.log.PayLog;
import cn.itcast.core.pojo.order.Order;
import cn.itcast.core.pojo.order.OrderItem;
import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 订单管理
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderDao orderDao;
    @Autowired
    private OrderItemDao orderItemDao;
    @Autowired
    private IdWorker idWorker;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ItemDao itemDao;
    @Autowired
    private PayLogDao payLogDao;

    //保存 订单表  订单详情表
    @Override
    public void add(Order order) {
        //购物车结果集
        List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps("cartList").get(order.getUserId());

        //总金额
        double tp = 0;

        //订单ID集合
        List<String> ids = new ArrayList<>();

        for (Cart cart : cartList) {
            //1:保存订单表
            //2:订单ID 分布式ID生成器
            long id = idWorker.nextId();
            ids.add(String.valueOf(id));
            order.setOrderId(id);
            //3:实付金额
            double totalPrice = 0;

            //4:支付类型
            order.setPaymentType("1");
            //5:支付状态  未
            order.setStatus("1");

            //创建时间
            order.setCreateTime(new Date());
            //更新时间
            order.setUpdateTime(new Date());
            //订单来源
            order.setSourceType("2");
            //商家ID
            order.setSellerId(cart.getSellerId());

            List<OrderItem> orderItemList = cart.getOrderItemList();
            for (OrderItem orderItem : orderItemList) {

                Item item = itemDao.selectByPrimaryKey(orderItem.getItemId());
                //1:订单详情表
                //2:订单详情表
                long oid = idWorker.nextId();
                orderItem.setId(oid);
                //3:外键订单主表的ID
                orderItem.setOrderId(id);
                //4:商品ID
                orderItem.setGoodsId(item.getGoodsId());
                //5:标题
                orderItem.setTitle(item.getTitle());
                //6:单价
                orderItem.setPrice(item.getPrice());
                //7:数量 已经有了
                //8:小计
                orderItem.setTotalFee(new BigDecimal(orderItem.getPrice().doubleValue()*orderItem.getNum()));
                //上面订单主表总金额的 计算
                totalPrice += orderItem.getTotalFee().doubleValue();

                tp += totalPrice;
                //8:图片
                orderItem.setPicPath(item.getImage());
                //9:商家Id
                orderItem.setSellerId(item.getSellerId());

                orderItemDao.insertSelective(orderItem);
            }
            //设置一下总金额
            order.setPayment(new BigDecimal(totalPrice));
            //保存订单主表
            orderDao.insertSelective(order);
        }
        //清空购物车
        redisTemplate.boundHashOps("cartList").delete(order.getUserId());
        //redisTemplate.boundHashOps("CART").delete("","");

        //支付日志表 (多个订单合并成一个支付日志 目的是为了一次性付款)
        PayLog payLog = new PayLog();
        //ID
        long payLogId = idWorker.nextId();
        payLog.setOutTradeNo(String.valueOf(payLogId));
        //创建时间
        payLog.setCreateTime(new Date());
        //总金额
        payLog.setTotalFee((long)(tp*100));
        //买  东西的用户
        payLog.setUserId(order.getUserId());
        //支付状态
        payLog.setTradeState("0");
        //订单结果集 3424321,2342141,432143214,4321412
        payLog.setOrderList(ids.toString().replace("[","").replace("]",""));

        payLog.setPayType("1");
        //保存Msyql数据一份
        payLogDao.insertSelective(payLog);

        //保存缓存中一份
        redisTemplate.boundHashOps("payLog").put(order.getUserId(),payLog);
    }
}
