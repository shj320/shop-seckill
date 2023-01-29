package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.Product;
import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.mapper.SeckillProductMapper;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.web.feign.ProductFeignApi;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by lanxw
 */
@Service
public class SeckillProductServiceImpl implements ISeckillProductService {
    @Autowired
    private SeckillProductMapper seckillProductMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Qualifier("cn.wolfcode.web.feign.ProductFeignApi")
    @Autowired
    private ProductFeignApi productFeignApi;

    @Override
    public List<SeckillProductVo> queryByTime(Integer time) {
        //查询秒杀商品集合
        List<SeckillProduct> seckillProductsList = seckillProductMapper.queryCurrentlySeckillProduct(time);
        if(seckillProductsList==null||seckillProductsList.size()==0){
            return Collections.EMPTY_LIST;
        }
        //获取商品id集合
        List<Long> productIds = new ArrayList<>();
        for (SeckillProduct seckProduct:seckillProductsList) {
            productIds.add(seckProduct.getProductId());
        }
        //远程调用获取商品集合
        Result<List<Product>> listResult = productFeignApi.queryByIds(productIds);
        if(listResult==null||listResult.hasError()){
            throw new BusinessException(SeckillCodeMsg.PRODUCT_SERVER_ERROR);
        }
        List<Product> productList=listResult.getData();
        Map<Long,Product> productMap=new HashMap<>();
        for (Product product:productList){
            productMap.put(product.getId(),product);
        }
        //封装vo
        List<SeckillProductVo> seckillProductVoList=new ArrayList<>();
        for (SeckillProduct seckProduct:seckillProductsList) {
            SeckillProductVo seckillProductVo = new SeckillProductVo();
            Product product = productMap.get(seckProduct.getProductId());

            BeanUtils.copyProperties(product,seckillProductVo);
            BeanUtils.copyProperties(seckProduct,seckillProductVo);
            seckillProductVo.setCurrentCount(seckProduct.getStockCount());
            seckillProductVoList.add(seckillProductVo);
        }
        return seckillProductVoList;
    }

    @Override
    public SeckillProductVo find(Integer time, Long seckillId) {
        //获取秒杀商品对象
        SeckillProduct seckillProduct= seckillProductMapper.find(seckillId);
        //获取商品对象
        List<Long> productId = new ArrayList<>();
        productId.add(seckillProduct.getProductId());
        Result<List<Product>> listResult = productFeignApi.queryByIds(productId);
        if(listResult==null||listResult.hasError()){
            throw new BusinessException(SeckillCodeMsg.PRODUCT_SERVER_ERROR);
        }
        Product product = listResult.getData().get(0);
        SeckillProductVo seckillProductVo = new SeckillProductVo();
        BeanUtils.copyProperties(product,seckillProductVo);
        BeanUtils.copyProperties(seckillProduct,seckillProductVo);
        seckillProductVo.setCurrentCount(seckillProduct.getStockCount());
        return seckillProductVo;
    }

    @Override
    public int decrStockCount(Long seckillId) {
        int i = seckillProductMapper.decrStock(seckillId);
        return i;
    }

    @Override
    public List<SeckillProductVo> queryByTimeFromCache(Integer time) {
        String key= SeckillRedisKey.SECKILL_PRODUCT_HASH.getRealKey(String.valueOf(time));
        List<Object> values = redisTemplate.opsForHash().values(key);
        List<SeckillProductVo> seckillProductVoList=new ArrayList<>();
        for (Object oj:values){
            seckillProductVoList.add(JSON.parseObject((String) oj,SeckillProductVo.class));
        }
        return seckillProductVoList;
    }

    @Override
    public SeckillProductVo findFromCache(Integer time, Long seckillId) {
        String key= SeckillRedisKey.SECKILL_PRODUCT_HASH.getRealKey(String.valueOf(time));
        Object o = redisTemplate.opsForHash().get(key,String.valueOf(seckillId));
        SeckillProductVo seckillProductVo = JSON.parseObject((String) o, SeckillProductVo.class);
        return seckillProductVo;
    }

    @Override
    public void syncStockToRedis(Integer time, Long seckillId) {
        SeckillProduct seckillProduct = seckillProductMapper.find(seckillId);
        if(seckillProduct.getStockCount()>0){
            String key = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(String.valueOf(time));
            //将数据库库存同步到redis中
            redisTemplate.opsForHash().put(key,String.valueOf(seckillId),String.valueOf(seckillProduct.getStockCount()));
        }
    }

    @Override
    public void incrStockCount(Long seckillId) {
        seckillProductMapper.incrStock(seckillId);
    }


}
