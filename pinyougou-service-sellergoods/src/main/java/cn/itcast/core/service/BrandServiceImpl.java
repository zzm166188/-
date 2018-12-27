package cn.itcast.core.service;

import cn.itcast.core.mapper.good.BrandDao;
import cn.itcast.core.pojo.good.Brand;
import cn.itcast.core.pojo.good.BrandQuery;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 品牌管理
 */
@Service
@Transactional
public class BrandServiceImpl implements  BrandService {

    @Autowired
    private BrandDao brandDao;
    //查询
    public List<Brand> findAll(){
        return brandDao.selectByExample(null);
    }

    //查询分页对象
    public PageResult findPage(Integer pageNum, Integer pageSize) {
        //分页插件
        PageHelper.startPage(pageNum,pageSize);
        //查询
        Page<Brand> p = (Page<Brand>) brandDao.selectByExample(null);
        return new PageResult(p.getTotal(), p.getResult());
    }
    //查询分页对象
    public PageResult search(Integer pageNum, Integer pageSize,Brand brand) {
        //分页插件
        PageHelper.startPage(pageNum,pageSize);
        //创建品牌条件对象
        BrandQuery brandQuery = new BrandQuery();

        BrandQuery.Criteria criteria = brandQuery.createCriteria();

        //判断名称
        if(null != brand.getName() && !"".equals(brand.getName().trim())){
            criteria.andNameLike("%" + brand.getName().trim() +"%");
        }
        //判断首字母
        if(null != brand.getFirstChar() && !"".equals(brand.getFirstChar().trim())){
            criteria.andFirstCharEqualTo(brand.getFirstChar().trim());
        }
        //查询
        Page<Brand> p = (Page<Brand>) brandDao.selectByExample(brandQuery);

        return new PageResult(p.getTotal(), p.getResult());

    }

    //保存
    @Override
    public void add(Brand brand) {
        brandDao.insertSelective(brand);

    }

    @Override
    public Brand findOne(Long id) {
        return brandDao.selectByPrimaryKey(id);
    }

    @Override
    public void update(Brand brand) {
        brandDao.updateByPrimaryKeySelective(brand);
    }

    @Override
    public void delete(Long[] ids) {
    /*    for (Long id : ids) {
          brandDao.deleteByPrimaryKey(id);
        }*/
        //delete from tb_brand where id in (1,2,3);
        BrandQuery brandQuery = new BrandQuery();
        //将数组转成集合  Arrays.asList(ids)
        //品牌条件对象 设置where 后面的条件
        brandQuery.createCriteria().andIdIn(Arrays.asList(ids));
        //动态Sql   无敌
        //select * from tb_brand where id in (1,2,3) and  name like ...
        brandDao.deleteByExample(brandQuery);

    }

    @Override
    public List<Map> selectOptionList() {
        return brandDao.selectOptionList();

    }
}
