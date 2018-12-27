package cn.itcast.core.service;

import cn.itcast.core.mapper.seller.SellerDao;
import cn.itcast.core.pojo.seller.Seller;
import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商家管理
 */
@Service
@Transactional
public class SellerServiceImpl implements  SellerService {

    @Autowired
    private SellerDao sellerDao;
    //添加
    public void add(Seller seller){

        //登陆名
        //密码
        seller.setPassword(new BCryptPasswordEncoder().encode(seller.getPassword()));
        //店铺
        //公司
        //入驻
        seller.setStatus("0");

        sellerDao.insertSelective(seller);
    }

    @Override
    public Seller findOne(String sellerId) {
        return sellerDao.selectByPrimaryKey(sellerId);
    }
}
