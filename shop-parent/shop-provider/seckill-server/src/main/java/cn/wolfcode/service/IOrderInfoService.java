package cn.wolfcode.service;


import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProductVo;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * Created by wolfcode-lanxw
 */
public interface IOrderInfoService {

    /**
     * 更具用户手机号码和商品id查看用户订单信息
     *
     * @param phone
     * @param seckillId
     * @return
     */
    OrderInfo findByPhoneAndSeckillId(String phone, Long seckillId);

    /**
     * 创建秒杀订单
     * @param phone
     * @param seckillProductVo
     * @return
     */
    OrderInfo doSeckill(String phone, SeckillProductVo seckillProductVo);

    /**
     * 根据订单号查询订单详情数据
     * @param orderNo
     * @return
     */
    OrderInfo findByOrderNo(String orderNo);

    /**
     * 根据订单号查看是否超时，超时取消
     * @param orderNo
     */
    void cancelOrder(String orderNo);

    /**
     * 获取支付服务返回的字符串
     * @param orderNo
     * @return
     */
    Result<String> payOnline(String orderNo);

    /**
     * 修改支付状态
     * @param orderNo
     * @param status
     * @param payType
     * @return
     */
    int changePayStatus( String orderNo,  Integer status, int payType);

    /**
     * 在线支付退款
     * @param byOrderNo
     */
    void refundOnline(OrderInfo byOrderNo);

    /**
     * 积分支付
     * @param orderNo
     * @return
     */
    void payIntegral(String orderNo);

    /**
     * 积分退款
     * @param byOrderNo
     */
    void refundIntegral(OrderInfo byOrderNo);
}
