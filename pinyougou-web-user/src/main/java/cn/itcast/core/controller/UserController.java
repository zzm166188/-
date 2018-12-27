package cn.itcast.core.controller;

import cn.itcast.common.utils.PhoneFormatCheckUtils;
import cn.itcast.core.pojo.user.User;
import cn.itcast.core.service.UserService;
import com.alibaba.dubbo.config.annotation.Reference;
import entity.Result;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.PatternSyntaxException;

/**
 * 用户管理
 * 注册
 * 登陆
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Reference
    private UserService userService;
    //获取验证码
    @RequestMapping("/sendCode")
    public Result sendCode(String phone){
        //判断手机号格式是否合法
        try {
            if(PhoneFormatCheckUtils.isPhoneLegal(phone)){
                //发送验证码
                userService.sendCode(phone);
                return new Result(true,"发送成功");
            }else{
                return new Result(false,"手机不合法");
            }
        } catch (PatternSyntaxException e) {
            e.printStackTrace();
            return new Result(false,"服务器异常");
        }
    }

    //添加
    @RequestMapping("/add")
    public Result add(@RequestBody User user, String smscode){
        try {
            userService.add(user,smscode);
            return new Result(true,"注册成功");
        } catch (RuntimeException e) {
            return new Result(false,e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false,"注册失败");
        }
    }

}
