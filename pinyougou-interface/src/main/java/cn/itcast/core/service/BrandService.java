package cn.itcast.core.service;

import cn.itcast.core.pojo.good.Brand;
import entity.PageResult;

import java.util.List;
import java.util.Map;

public interface BrandService {

    //查询
    public List<Brand> findAll();

    //查询分页对象
    public PageResult findPage(Integer pageNum, Integer pageSize);

    //添加
    public void add(Brand brand);

    public Brand findOne(Long id);

    public void update(Brand brand);

    void delete(Long[] ids);

    PageResult search(Integer pageNum, Integer pageSize, Brand brand);

    List<Map> selectOptionList();
}
