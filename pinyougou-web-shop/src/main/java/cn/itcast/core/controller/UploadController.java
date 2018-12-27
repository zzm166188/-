package cn.itcast.core.controller;

import cn.itcast.common.utils.FastDFSClient;
import entity.Result;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 上传图片 管理
 */
@RestController
@RequestMapping("/upload")
public class UploadController {

    //从配置文件 properties文件中 获取K V值  解决硬编码问题
    @Value("${FILE_SERVER_URL}")
    private String url;


    //上传商品图片  Springmvc 接收图片
    @RequestMapping("/uploadFile")
    public Result uploadFile(MultipartFile file){

        try {
            //原始名称
            //file.getOriginalFilename() /dsfsafafads.jpg  png
            //图片的二进制
            //file.getBytes()
            //file.get

            //上传到分布式文件系统上去
            FastDFSClient fastDFSClient = new FastDFSClient("classpath:fastDFS/fdfs_client.conf");

            //扩展名
            String ext = FilenameUtils.getExtension(file.getOriginalFilename());

            //上传图片
            // group1/M00/00/01/wKjIgFWOYc6APpjAAAD-qk29i78248.jpg
            String path = fastDFSClient.uploadFile(file.getBytes(), ext);


            return new Result(true, url + path);
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false,"上传失败");
        }

    }
}
