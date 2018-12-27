package cn.itcast.core.service;

import cn.itcast.core.mapper.specification.SpecificationDao;
import cn.itcast.core.mapper.specification.SpecificationOptionDao;
import cn.itcast.core.pojo.specification.Specification;
import cn.itcast.core.pojo.specification.SpecificationOption;
import cn.itcast.core.pojo.specification.SpecificationOptionQuery;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import pojogroup.SpecificationVo;

import java.util.List;
import java.util.Map;

/**
 * 规格管理
 */
@Service
@Transactional
public class SpecificationServiceImpl implements  SpecificationService {

    @Autowired
    private SpecificationDao specificationDao;
    @Autowired
    private SpecificationOptionDao specificationOptionDao;

    //查询分页
    @Override
    public PageResult search(Integer page, Integer rows, Specification specification) {
        //分页插件
        PageHelper.startPage(page,rows);
        //判断是否有值 (不写了)
        //查询
        Page<Specification> p = (Page<Specification>) specificationDao.selectByExample(null);
        return new PageResult(p.getTotal(),p.getResult());
    }

    //添加
    @Override
    public void add(SpecificationVo vo) {

        //规格表 主键
        specificationDao.insertSelective(vo.getSpecification());
        //规格选项结果集 外键
        List<SpecificationOption> specificationOptionList = vo.getSpecificationOptionList();
        for (SpecificationOption specificationOption : specificationOptionList) {
            //外键
            specificationOption.setSpecId(vo.getSpecification().getId());
            //保存
            specificationOptionDao.insertSelective(specificationOption);
        }

    }

    @Override
    public SpecificationVo findOne(Long id) {

        SpecificationVo vo = new SpecificationVo();

        //规格对象
        Specification specification = specificationDao.selectByPrimaryKey(id);
        vo.setSpecification(specification);
        //规格选项对象结果集
        SpecificationOptionQuery query = new SpecificationOptionQuery();
        query.createCriteria().andSpecIdEqualTo(id);

        List<SpecificationOption> specificationOptions = specificationOptionDao.selectByExample(query);
        vo.setSpecificationOptionList(specificationOptions);
        return vo;
    }
    //Ctrl + i

    @Override
    public void update(SpecificationVo vo) {

        //规格表 修改
        specificationDao.updateByPrimaryKeySelective(vo.getSpecification());

        //规格选项结果集表
        //1:先删除  外键
        SpecificationOptionQuery query = new SpecificationOptionQuery();
        query.createCriteria().andSpecIdEqualTo(vo.getSpecification().getId());
        specificationOptionDao.deleteByExample(query);

        //2:再添加

        //规格选项结果集 外键
        List<SpecificationOption> specificationOptionList = vo.getSpecificationOptionList();
        for (SpecificationOption specificationOption : specificationOptionList) {
            //外键
            specificationOption.setSpecId(vo.getSpecification().getId());
            //保存
            specificationOptionDao.insertSelective(specificationOption);
        }



    }

    @Override
    public List<Map> selectOptionList() {
        return specificationDao.selectOptionList();
    }


}
