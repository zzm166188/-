package cn.itcast.core.service;

import cn.itcast.core.mapper.specification.SpecificationOptionDao;
import cn.itcast.core.mapper.template.TypeTemplateDao;
import cn.itcast.core.pojo.specification.SpecificationOption;
import cn.itcast.core.pojo.specification.SpecificationOptionQuery;
import cn.itcast.core.pojo.template.TypeTemplate;
import cn.itcast.core.pojo.template.TypeTemplateQuery;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 模板管理
 */
@Service
@Transactional
public class TypeTemplateServiceImpl implements TypeTemplateService {


    @Autowired
    private TypeTemplateDao typeTemplateDao;
    @Autowired
    private SpecificationOptionDao specificationOptionDao;
    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public PageResult search(Integer page, Integer rows, TypeTemplate tt) {
        //1:查询所有模板结果集
        List<TypeTemplate> typeTemplates = typeTemplateDao.selectByExample(null);
        //2:保存到缓存中
        for (TypeTemplate typeTemplate : typeTemplates) {

            //[{"id":52,"text":"法拉利"},{"id":53,"text":"特斯拉"},{"id":54,"text":"五凌宏光"}]
            //品牌列表
            redisTemplate.boundHashOps("brandList").put(typeTemplate.getId(),JSON.parseArray(typeTemplate.getBrandIds(), Map.class));
            //规格列表
            //[{"id":44,"text":"汽车颜色123"},{"id":45,"text":"汽车排量"},{"id":46,"text":"汽车坐标"}]
            redisTemplate.boundHashOps("specList").put(typeTemplate.getId(),findBySpecList(typeTemplate.getId()));

        }

        
        //分页插件
        PageHelper.startPage(page,rows);
        //排序
        //PageHelper.orderBy("id desc");
        TypeTemplateQuery query = new TypeTemplateQuery();
        query.setOrderByClause("id desc");

        //分页对象
        Page<TypeTemplate> p = (Page<TypeTemplate>) typeTemplateDao.selectByExample(query);

        return new PageResult(p.getTotal(),p.getResult());
    }

    @Override
    public void add(TypeTemplate tt) {
        typeTemplateDao.insertSelective(tt);
    }

    @Override
    public TypeTemplate findOne(Long id) {
        return typeTemplateDao.selectByPrimaryKey(id);
    }

    @Override
    public void update(TypeTemplate tt) {
        typeTemplateDao.updateByPrimaryKeySelective(tt);
    }

    @Override
    public List<Map> findBySpecList(Long id) {
        //通过模板ID查询模板对象

        TypeTemplate typeTemplate = typeTemplateDao.selectByPrimaryKey(id);
        //规格  [{"id":27,"text":"网络"},{"id":32,"text":"机身内存"}]
        String specIds = typeTemplate.getSpecIds();
        //fastjson
        List<Map> listMap = JSON.parseArray(specIds, Map.class);
        for (Map map : listMap) {

        //  Map1 K:id V:27  K:text V:网络 K:options V:list
            SpecificationOptionQuery query = new SpecificationOptionQuery();
            query.createCriteria().andSpecIdEqualTo((long)(Integer)map.get("id"));//Object -->Integer --> Long
            List<SpecificationOption> specificationOptionList = specificationOptionDao.selectByExample(query);
            map.put("options",specificationOptionList);
            //Map2
        }


        return listMap;
    }
}
