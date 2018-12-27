package cn.itcast.core.controller;

import cn.itcast.core.pojo.good.Goods;
import cn.itcast.core.service.GoodsService;
import com.alibaba.dubbo.config.annotation.Reference;
import entity.PageResult;
import entity.Result;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pojogroup.GoodsVo;

/**
 * 商品管理
 */
@SuppressWarnings("all")
@RestController
@RequestMapping("/goods")
public class GoodsController {



    @Reference
    private GoodsService goodsService;
    //商品添加
    @RequestMapping("/add")
    public Result add(@RequestBody GoodsVo vo){
        try {
            //商家ID 主键
            String name = SecurityContextHolder.getContext().getAuthentication().getName();
            vo.getGoods().setSellerId(name);
            goodsService.add(vo);
            return new Result(true,"成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false,"失败");
        }
    }
    //商品添加
    @RequestMapping("/update")
    public Result update(@RequestBody GoodsVo vo){
        try {


            goodsService.update(vo);
            return new Result(true,"成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false,"失败");
        }
    }
    //商品分页查询 条件
    @RequestMapping("/search")
    public PageResult search(Integer page,Integer rows,@RequestBody Goods goods){

        return goodsService.search(page,rows,goods);
    }
    //查询一个包装对象
    @RequestMapping("/findOne")
    public GoodsVo findOne(Long id){
        return goodsService.findOne(id);
    }

    //删除
    @RequestMapping("/delete")
    public Result delete(Long[] ids ){
        try {
            goodsService.delete(ids);
            return new Result(true,"成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false,"失败");
        }

    }

    //开始审核 (1:通过 2:驳回)
    @RequestMapping("/updateStatus")
    public Result updateStatus(Long[] ids ,String status){
        try {
            goodsService.updateStatus(ids,status);
            return new Result(true,"成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false,"失败");
        }
    }

}
