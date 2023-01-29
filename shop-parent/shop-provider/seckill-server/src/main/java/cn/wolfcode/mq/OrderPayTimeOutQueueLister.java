/**
 * author:sj
 */

package cn.wolfcode.mq;

import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(consumerGroup = "OrderPayTimeOut",topic = MQConstant.ORDER_PAY_TIMEOUT_TOPIC)
public class OrderPayTimeOutQueueLister implements RocketMQListener<OrderMQResult> {
    @Autowired
    private IOrderInfoService orderInfoService;
    @Override
    public void onMessage(OrderMQResult orderMQResult) {
        System.out.println("lokk我是傻逼");
        orderInfoService.cancelOrder(orderMQResult.getOrderNo());
    }
}
