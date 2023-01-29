/**
 * author:sj
 */

package cn.wolfcode.mq;

import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(consumerGroup = "peddingGroup",topic = MQConstant.ORDER_PEDDING_TOPIC)
public class OrderPeddingQueueListener implements RocketMQListener<OrderMessage> {
    @Autowired
    private IOrderInfoService orderInfoService;
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Override
    public void onMessage(OrderMessage orderMessage) {
        OrderMQResult orderMQResult = new OrderMQResult();
        orderMQResult.setToken(orderMessage.getToken());
        String tag;
        try {
            SeckillProductVo vo = seckillProductService.find(orderMessage.getTime(), orderMessage.getSeckillId());
            OrderInfo orderInfo = orderInfoService.doSeckill(String.valueOf(orderMessage.getUserPhone()), vo);
            orderMQResult.setOrderNo(orderInfo.getOrderNo());
            tag=MQConstant.ORDER_RESULT_SUCCESS_TAG;
            Message<OrderMQResult> message = MessageBuilder.withPayload(orderMQResult).build();
            rocketMQTemplate.syncSend(MQConstant.ORDER_PAY_TIMEOUT_TOPIC,message,3000,MQConstant.ORDER_PAY_TIMEOUT_DELAY_LEVEL);
        }catch (Exception e){
            e.printStackTrace();
            orderMQResult.setCode(SeckillCodeMsg.SECKILL_ERROR.getCode());
            orderMQResult.setMsg(SeckillCodeMsg.SECKILL_ERROR.getMsg());
            orderMQResult.setTime(orderMessage.getTime());
            orderMQResult.setSeckillId(orderMessage.getSeckillId());
            tag=MQConstant.ORDER_RESULT_FAIL_TAG;
        }
        rocketMQTemplate.syncSend(MQConstant.ORDER_RESULT_TOPIC+":"+tag,orderMQResult);
    }
}
