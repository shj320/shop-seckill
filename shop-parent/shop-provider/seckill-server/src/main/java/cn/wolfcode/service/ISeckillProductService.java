package cn.wolfcode.service;

import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;

import java.util.List;

/**
 * Created by lanxw
 */
public interface ISeckillProductService {
    /**
     * 查询秒杀集合
     * @param time
     * @return
     */
    List<SeckillProductVo> queryByTime(Integer time);

    /**
     * 根据秒杀时间和秒杀商品id查看商品信息
     * @param time
     * @param seckillId
     * @return
     */
    SeckillProductVo find(Integer time, Long seckillId);

    /**
     * 更具秒杀商品扣减库存
     * @param seckillId
     */
    int decrStockCount(Long seckillId);

    /**
     * 缓存中获取秒杀商品的信息
     * @param time
     * @return
     */
    List<SeckillProductVo> queryByTimeFromCache(Integer time);

    /**
     * 根据时间和商品号来获取商品的信息
     * @param time
     * @param seckillId
     * @return
     */
    SeckillProductVo findFromCache(Integer time, Long seckillId);

    /**
     * 秒杀过程失败，redis里面商品数量和数据库同步
     * @param time
     * @param seckillId
     */
    void syncStockToRedis(Integer time, Long seckillId);

    /**
     * 增加库存
     * @param seckillId
     */
    void incrStockCount(Long seckillId);



}
