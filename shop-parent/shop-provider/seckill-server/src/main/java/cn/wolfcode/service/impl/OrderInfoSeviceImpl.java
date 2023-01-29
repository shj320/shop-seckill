package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.*;
import cn.wolfcode.mapper.OrderInfoMapper;
import cn.wolfcode.mapper.PayLogMapper;
import cn.wolfcode.mapper.RefundLogMapper;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.util.IdGenerateUtil;
import cn.wolfcode.web.feign.IntegralFeignApi;
import cn.wolfcode.web.feign.PayFeignApi;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Created by wolfcode-lanxw
 */
@Service
public class OrderInfoSeviceImpl implements IOrderInfoService {
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private PayLogMapper payLogMapper;
    @Autowired
    private RefundLogMapper refundLogMapper;
    @Qualifier("cn.wolfcode.web.feign.IntegralFeignApi")
    @Autowired
    private IntegralFeignApi integralFeignApi;
    @Qualifier("cn.wolfcode.web.feign.PayFeignApi")
    @Autowired
    private PayFeignApi payFeignApi;

    @Override
    public OrderInfo findByPhoneAndSeckillId(String phone, Long seckillId) {
        return orderInfoMapper.findByPhoneAndSeckillId(phone,seckillId);
    }

    @Override
    @Transactional
    public OrderInfo doSeckill(String phone, SeckillProductVo seckillProductVo) {
        //扣减库存
        int count = seckillProductService.decrStockCount(seckillProductVo.getId());
        if(count==0){
            throw new BusinessException(SeckillCodeMsg.SECKILL_STOCK_OVER);
        }
        //订单创建
        OrderInfo orderInfo = creatOrderInfo(phone,seckillProductVo);
        //在redis中设置set集合来存储抢到秒杀商品的电话
        String realKey = SeckillRedisKey.SECKILL_ORDER_SET.getRealKey(String.valueOf(seckillProductVo.getId()));
        redisTemplate.opsForSet().add(realKey,phone);
        return orderInfo;
    }

    @Override
    public OrderInfo findByOrderNo(String orderNo) {

        return orderInfoMapper.find(orderNo);
    }

    @Override
    @Transactional
    public void cancelOrder(String orderNo) {
        OrderInfo orderInfo = orderInfoMapper.find(orderNo);
        //判断订单是否未付款
        if(OrderInfo.STATUS_ARREARAGE.equals(orderInfo.getStatus())){
            //修改订单状态
            int i = orderInfoMapper.updateCancelStatus(orderNo, OrderInfo.STATUS_TIMEOUT);
            if(i==0){
                return;
            }
            //真实库存回补
            seckillProductService.incrStockCount(orderInfo.getSeckillId());
            //预库存回补
            seckillProductService.syncStockToRedis(orderInfo.getSeckillTime(),orderInfo.getSeckillId());
            //redis删除

        }
        System.out.println("傻逼结束");
    }

    @Value("${pay.returnUrl}")
    private String returnUrl;
    @Value("${pay.notifyUrl}")
    private String notifyUrl;
    @Override
    public Result<String> payOnline(String orderNo) {
        OrderInfo byOrderNo = this.findByOrderNo(orderNo);
        if(OrderInfo.STATUS_ARREARAGE.equals(byOrderNo.getStatus())){
            PayVo payVo = new PayVo();
            payVo.setOutTradeNo(orderNo);
            payVo.setTotalAmount(String.valueOf(byOrderNo.getSeckillPrice()));
            payVo.setSubject(byOrderNo.getProductName());
            payVo.setBody(byOrderNo.getProductName());
            payVo.setNotifyUrl(notifyUrl);
            payVo.setReturnUrl(returnUrl);
            Result<String> result = payFeignApi.payOnline(payVo);
            return result;
        }

        return  Result.error(SeckillCodeMsg.NOT_MANY_PAY);
    }

    @Override
    public int changePayStatus(String orderNo, Integer status, int payType) {
        int i = orderInfoMapper.changePayStatus(orderNo, status, payType);
        return i;
    }

    @Override
    public void refundOnline(OrderInfo byOrderNo) {
        RefundVo refundVo = new RefundVo();
        refundVo.setRefundReason("不想要");
        refundVo.setRefundAmount(String.valueOf(byOrderNo.getSeckillPrice()));
        refundVo.setOutTradeNo(byOrderNo.getOrderNo());
        Result<Boolean> result= payFeignApi.refund(refundVo);
        if(result==null || result.hasError() || !result.getData()){
            throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
        }
        orderInfoMapper.changeRefundStatus(byOrderNo.getOrderNo(),OrderInfo.STATUS_REFUND);
    }

    /**
     *
     * @param orderNo
     * @return
     */
    @Override
    @GlobalTransactional
    public void payIntegral(String orderNo) {
        OrderInfo byOrderNo = this.findByOrderNo(orderNo);
        if(OrderInfo.STATUS_ARREARAGE.equals(byOrderNo.getStatus())){
            //插入日志记录
            PayLog payLog = new PayLog();
            payLog.setOrderNo(orderNo);
            payLog.setPayTime(new Date());
            payLog.setPayType(OrderInfo.PAYTYPE_INTERGRAL);
            payLog.setTotalAmount(String.valueOf(byOrderNo.getIntergral()));
            payLogMapper.insert(payLog);
            //远程调用积分服务
            OperateIntergralVo vo = new OperateIntergralVo();
            vo.setUserId(byOrderNo.getUserId());
            vo.setValue(byOrderNo.getIntergral());
            //调用积分服务
            Result result=integralFeignApi.decrIntegral(vo);
            if(result==null || result.hasError()){
                throw new BusinessException(SeckillCodeMsg.INTERGRAL_SERVER_ERROR);
            }
            int i = orderInfoMapper.changePayStatus(orderNo, OrderInfo.STATUS_ACCOUNT_PAID, OrderInfo.PAYTYPE_INTERGRAL);
            if(i==0){
                throw new BusinessException(SeckillCodeMsg.PAY_ERROR);
            }

        }

    }

    @Override
    @GlobalTransactional
    public void refundIntegral(OrderInfo byOrderNo) {
        if(OrderInfo.STATUS_ACCOUNT_PAID.equals(byOrderNo.getStatus())){
            //添加退款记录
            RefundLog refundLog = new RefundLog();
            refundLog.setRefundReason("不想要");
            refundLog.setOrderNo(String.valueOf(byOrderNo.getOrderNo()));
            refundLog.setRefundAmount(byOrderNo.getIntergral());
            refundLog.setRefundTime(new Date());
            refundLog.setRefundType(OrderInfo.PAYTYPE_INTERGRAL);
            refundLogMapper.insert(refundLog);

            OperateIntergralVo vo = new OperateIntergralVo();
            vo.setUserId(byOrderNo.getUserId());
            vo.setValue(byOrderNo.getIntergral());

            //调用积分服务
            Result result=integralFeignApi.incrIntegral(vo);
            if(result==null || result.hasError()){
                throw new BusinessException(SeckillCodeMsg.INTERGRAL_SERVER_ERROR);
            }
            //修改订单状态
            int i = orderInfoMapper.changeRefundStatus(byOrderNo.getOrderNo(),OrderInfo.STATUS_REFUND);
            if(i==0){
                throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
            }
        }
    }

    /**
     * 创建订单
     * @param phone
     * @param seckillProductVo
     * @return
     */
    private OrderInfo creatOrderInfo(String phone, SeckillProductVo seckillProductVo) {
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(seckillProductVo,orderInfo);
        orderInfo.setUserId(Long.parseLong(phone));
        orderInfo.setCreateDate(new Date());
        orderInfo.setSeckillDate(seckillProductVo.getStartDate());
        orderInfo.setSeckillTime(seckillProductVo.getTime());
        orderInfo.setOrderNo(String.valueOf(IdGenerateUtil.get().nextId()));
        orderInfo.setDeliveryAddrId(1L);
        orderInfo.setSeckillId(seckillProductVo.getId());
        orderInfoMapper.insert(orderInfo);
        return orderInfo;
    }
}
