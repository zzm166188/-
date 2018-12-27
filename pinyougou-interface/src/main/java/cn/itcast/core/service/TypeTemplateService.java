package cn.itcast.core.service;

import cn.itcast.core.pojo.template.TypeTemplate;
import entity.PageResult;

import java.util.List;
import java.util.Map;

public interface TypeTemplateService {

    PageResult search(Integer page, Integer rows, TypeTemplate tt);

    void add(TypeTemplate tt);

    TypeTemplate findOne(Long id);

    void update(TypeTemplate tt);

    List<Map> findBySpecList(Long id);
}
