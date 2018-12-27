package cn.itcast.core.service;

import cn.itcast.core.pojo.seller.Seller;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Set;

/**
 * 配置自定义的认证类  连接Mysql数据库 查询用户信息
 */
public class UserDetailServiceImpl implements UserDetailsService{


    private SellerService sellerService;
    public void setSellerService(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    //方法
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //商家名称 查询 商家对象
        Seller seller = sellerService.findOne(username);
        if (null != seller) {
            //2:查询到了 : 此商家名称是存在 的
            if ("1".equals(seller.getStatus())) {
                //           //4:审核通过
                Set<GrantedAuthority> authorities = new HashSet<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_SELLER"));
                return new User(username, seller.getPassword(), authorities);
            }

        }
            return null;
    }
}