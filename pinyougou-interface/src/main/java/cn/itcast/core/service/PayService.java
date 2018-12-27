package cn.itcast.core.service;

import java.util.Map;

public interface PayService {

    //远程调用腾讯那边服务器
    public Map<String,String> createNative(String name);
}
