package cn.wolfcode.web.controller;

import cn.wolfcode.common.constants.CommonConstants;
import cn.wolfcode.common.web.CodeMsg;
import cn.wolfcode.common.web.CommonCodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.common.web.anno.RequireLogin;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.mq.MQConstant;
import cn.wolfcode.mq.OrderMessage;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.service.impl.OrderInfoSeviceImpl;
import cn.wolfcode.util.DateUtil;
import cn.wolfcode.util.UserUtil;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import com.netflix.client.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by lanxw
 */
@RestController
@RequestMapping("/order")
@Slf4j
public class OrderInfoController {
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private IOrderInfoService orderInfoService;

    @RequestMapping("/doSeckill")
    @RequireLogin
    public Result<String> doSeckill(Integer time,
                                    Long seckillId,
                                    HttpServletRequest httpServletRequest){
        //判断是否处于抢购时间
        SeckillProductVo seckillProductVo = seckillProductService.findFromCache(time, seckillId);
        //测试阶段先把时间判断注销发布要打开
//        boolean legalTime = DateUtil.isLegalTime(seckillProductVo.getStartDate(), time);
//        if(!legalTime){
//            return Result.error(SeckillCodeMsg.SECKILL_TIME_OUT);
//        }

        //一个用户只能抢购一次
        String header = httpServletRequest.getHeader(CommonConstants.TOKEN_NAME);
        String phone = UserUtil.getUserPhone(redisTemplate, header);

        String realKey = SeckillRedisKey.SECKILL_ORDER_SET.getRealKey(String.valueOf(seckillId));

        if(redisTemplate.opsForSet().isMember(realKey,phone)){
            return Result.error(SeckillCodeMsg.REPEAT_SECKILL);
        }
        //保证库存数量
        Integer currentCount = seckillProductVo.getCurrentCount();
        if(currentCount<=0){
            return Result.error(SeckillCodeMsg.SECKILL_STOCK_OVER);
        }
        //使用redis来控制秒杀请求
        String key = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(String.valueOf(time));
        Long increment = redisTemplate.opsForHash().increment(key, String.valueOf(seckillId), -1);
        if(increment<0){
            return Result.error(SeckillCodeMsg.SECKILL_STOCK_OVER);
        }
        //使用Mq进行异步下单
        OrderMessage orderMessage = new OrderMessage(time,seckillId,header,Long.parseLong(phone));
        rocketMQTemplate.syncSend(MQConstant.ORDER_PEDDING_TOPIC,orderMessage);
        return Result.success("成功进入秒杀队列等待结果");
    }
    @RequestMapping("/find")
    @RequireLogin
    public Result<OrderInfo> find(String orderNo,
                                  HttpServletRequest httpServletRequest){
        OrderInfo orderInfo= orderInfoService.findByOrderNo(orderNo);
        String token = httpServletRequest.getHeader(CommonConstants.TOKEN_NAME);
        String phone = UserUtil.getUserPhone(redisTemplate, token);
        if(!phone.equals(String.valueOf(orderInfo.getUserId()))){
            return Result.error(SeckillCodeMsg.NO_LOOK_OTHER);
        }
        return Result.success(orderInfo);
    }
}
