package cn.itcast.core.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 登陆管理
 */
@RestController
@RequestMapping("/login")
public class LoginController {

    //获取当前登陆人
    @RequestMapping("/showName")
    public Map<String,Object> showName(){

        String name = SecurityContextHolder.getContext().getAuthentication().getName();

        Map<String,Object> map = new HashMap<>();
        map.put("username",name);
        map.put("curTime",new Date());//new Date() 英国:格林威志时间   页面回显时间 字符串 yyyy-MM-dd HH:mm:ss
        // @ResponseBody

        return map;

    }
}
