package cn.itcast.core.listener;

import cn.itcast.core.mapper.item.ItemDao;
import cn.itcast.core.pojo.item.Item;
import cn.itcast.core.pojo.item.ItemQuery;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrTemplate;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.List;

/**
 * 消息处理类
 */
public class ItemSearchListener implements MessageListener{


    @Autowired
    private ItemDao itemDao;
    @Autowired
    private SolrTemplate solrTemplate;
    @Override
    public void onMessage(Message message) {
        ActiveMQTextMessage atm = (ActiveMQTextMessage)message;
        //商品ID
        try {
            String id = atm.getText();
            System.out.println("搜索管理项目:接收到的ID:" + id);

            //2:保存商品信息到索引库
            //库存表中数据保存索引
            ItemQuery itemQuery =  new ItemQuery();
            itemQuery.createCriteria().andGoodsIdEqualTo(Long.parseLong(id)).andIsDefaultEqualTo("1");
            List<Item> itemList = itemDao.selectByExample(itemQuery);
            solrTemplate.saveBeans(itemList,1000);

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
