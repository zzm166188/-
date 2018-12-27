package cn.itcast.core.service;

import cn.itcast.core.pojo.item.Item;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;

import java.util.*;

/**
 * 搜索管理
 */
@Service
public class ItemsearchServiceImpl implements ItemsearchService {

    @Autowired
    private SolrTemplate solrTemplate;//
    @Autowired
    private RedisTemplate redisTemplate;

    //开始搜索
    @Override
    public Map<String, Object> search(Map<String, String> searchMap) {

        //关键词处理一下
        searchMap.put("keywords",searchMap.get("keywords").replaceAll(" ",""));



        //4:结果集
        //5:总条数
        Map<String, Object> map = searchHighlightPage(searchMap);
        //1:商品分类
        List<String> categoryList = searchCategoryByKeywords(searchMap);
        map.put("categoryList", categoryList);

        if (null != categoryList && categoryList.size() > 0) {
            //第一个商品分类
            Object typeId = redisTemplate.boundHashOps("itemCat").get(categoryList.get(0));
            //2:品牌列表
            List<Map> brandList = (List<Map>) redisTemplate.boundHashOps("brandList").get(typeId);
            //3:规格列表
            List<Map> specList = (List<Map>) redisTemplate.boundHashOps("specList").get(typeId);

            map.put("brandList", brandList);
            map.put("specList", specList);
        }

        return map;
    }
    //定义搜索对象的结构  category:商品分类
    // $scope.searchMap={'keywords':'','category':'','brand':'','spec':{},'price':'','pageNo':1,'pageSize':40,'sort':'','sortField':''};

    //通过关键词查询商品分类(分组方式)
    public List<String> searchCategoryByKeywords(Map<String, String> searchMap) {
        //关键词
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        Query query = new SimpleQuery(criteria);

        //分组设置
        GroupOptions groupOptions = new GroupOptions();
        groupOptions.addGroupByField("item_category");//group by item_category
        query.setGroupOptions(groupOptions);

        //执行查询
        GroupPage<Item> page = solrTemplate.queryForGroupPage(query, Item.class);

        List<String> categorys = new ArrayList<>();

        GroupResult<Item> categoryList = page.getGroupResult("item_category");//select item_category
        System.out.println();
        Page<GroupEntry<Item>> groupEntries = categoryList.getGroupEntries();
        List<GroupEntry<Item>> content = groupEntries.getContent();
        for (GroupEntry<Item> itemGroupEntry : content) {
            categorys.add(itemGroupEntry.getGroupValue());
        }
        return categorys;

    }

    //查询结果集  总条数
    public Map<String, Object> searchHighlightPage(Map<String, String> searchMap) {
        Map<String, Object> map = new HashMap<>();

        //关键词
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        HighlightQuery query = new SimpleHighlightQuery(criteria);

        //TODO 过滤条件

        //商品分类
        if (null != searchMap.get("category") && !"".equals(searchMap.get("category").trim())) {

            FilterQuery filterQuery = new
                    SimpleFilterQuery(new Criteria("item_category").is(searchMap.get("category").trim()));
            query.addFilterQuery(filterQuery);
        }
        //品牌
        if (null != searchMap.get("brand") && !"".equals(searchMap.get("brand").trim())) {

            FilterQuery filterQuery = new
                    SimpleFilterQuery(new Criteria("item_brand").is(searchMap.get("brand").trim()));
            query.addFilterQuery(filterQuery);
        }

        //规格
  /*      "item_spec_网络": "联通3G",
          "item_spec_机身内存": "16G",*/
        if (null != searchMap.get("spec") && !"".equals(searchMap.get("spec").trim())) {

            //{网络:4G,屏幕:5.0}
            Map<String, String> specMap = JSON.parseObject(searchMap.get("spec"), Map.class);
            Set<Map.Entry<String, String>> entries = specMap.entrySet();
            for (Map.Entry<String, String> entry : entries) {

                FilterQuery filterQuery = new
                        SimpleFilterQuery(
                        new Criteria("item_spec_" + entry.getKey()).is(entry.getValue()));
                query.addFilterQuery(filterQuery);
            }

        }


        //价格区间
        if (null != searchMap.get("price") && !"".equals(searchMap.get("price").trim())) {
            String[] p = searchMap.get("price").trim().split("-");
            FilterQuery filterQuery = null;
            //判断是否包含*
            if (searchMap.get("price").contains("*")) {
                //包含*
                filterQuery = new
                        SimpleFilterQuery(
                        new Criteria("item_price").greaterThanEqual(p[0]));
            } else {
                //不包含*
                filterQuery = new
                        SimpleFilterQuery(
                        new Criteria("item_price").between(p[0], p[1], true, false));
            }
            query.addFilterQuery(filterQuery);

        }
        //TODO 排序
        //定义搜索对象的结构  category:商品分类
        //$scope.searchMap={'sort':'','sortField':''};
        if(null != searchMap.get("sortField") && !"".equals(searchMap.get("sortField").trim())){

            if("DESC".equals(searchMap.get("sort"))){

                query.addSort(new Sort(Sort.Direction.DESC,"item_" + searchMap.get("sortField").trim()));
            }else{
                query.addSort(new Sort(Sort.Direction.ASC,"item_" + searchMap.get("sortField").trim()));

            }


        }

        //开启高亮
        HighlightOptions options = new HighlightOptions();
        //需要高亮的域
        options.addField("item_title");
        //前缀
        options.setSimplePrefix("<span style='color:red'>");
        //后缀
        options.setSimplePostfix("</span>");

        query.setHighlightOptions(options);

        //分页
        String pageNo = searchMap.get("pageNo");
        String pageSize = searchMap.get("pageSize");

        query.setOffset((Integer.parseInt(pageNo) - 1) * Integer.parseInt(pageSize));
        //每页数
        query.setRows(Integer.parseInt(pageSize));
        //执行查询
        HighlightPage<Item> page = solrTemplate.queryForHighlightPage(query, Item.class);
        //总条数
        map.put("total", page.getTotalElements());
        //总页数
        map.put("totalPages", page.getTotalPages());

        List<HighlightEntry<Item>> highlighted = page.getHighlighted();
        for (HighlightEntry<Item> itemHighlightEntry : highlighted) {

            Item item = itemHighlightEntry.getEntity();

            List<HighlightEntry.Highlight> highlights = itemHighlightEntry.getHighlights();
            if (null != highlights && highlights.size() > 0) {
                //有高亮 换成高亮的  没有高亮 使用自己原来的名称
                item.setTitle(highlights.get(0).getSnipplets().get(0));
            }
        }
        //结果集
        map.put("rows", page.getContent());
        return map;
    }
}
