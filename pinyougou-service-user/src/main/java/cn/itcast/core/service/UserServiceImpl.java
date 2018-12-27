package cn.itcast.core.service;

import cn.itcast.core.mapper.user.UserDao;
import cn.itcast.core.pojo.user.User;
import com.alibaba.dubbo.config.annotation.Service;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import javax.jms.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 用户管理
 */
@Service
public class UserServiceImpl implements  UserService {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
   // private JmsMessagingTemplate jmsMessagingTemplate;
    private JmsTemplate jmsTemplate;
    @Autowired
    private Destination smsDestination;
    //发送短信验证码
    @Override
    public void sendCode(String phone) {
        //1:生成6位验证码
        String randomNumeric = RandomStringUtils.randomNumeric(6);
        //2:
        redisTemplate.boundValueOps(phone).set(randomNumeric);
        redisTemplate.boundValueOps(phone).expire(5, TimeUnit.DAYS);
        //3:发消息

        jmsTemplate.send(smsDestination, new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {

                MapMessage mapMessage = session.createMapMessage();
                mapMessage.setString("phone",phone);
                mapMessage.setString("signName","品优购商城");
                mapMessage.setString("templateCode","SMS_126462276");
                mapMessage.setString("templateParam","{\"number\":\""+randomNumeric+"\"}");
                return mapMessage;
            }
        });
    }

    @Autowired
    private UserDao userDao;
    @Override
    public void add(User user, String smscode) {
        //1:判断验证码是否正确
        String code = (String) redisTemplate.boundValueOps(user.getPhone()).get();
        if(null != code && code.equals(smscode)){
            //验证码是正确的
            //保存
            user.setCreated(new Date());
            user.setUpdated(new Date());
            userDao.insertSelective(user);
        }else{
            //错误
            throw new RuntimeException("验证码不正确");
        }
    }

}
