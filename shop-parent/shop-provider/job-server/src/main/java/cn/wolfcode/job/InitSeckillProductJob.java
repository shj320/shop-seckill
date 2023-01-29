/**
 * author:sj
 */

package cn.wolfcode.job;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.redis.JobRedisKey;
import cn.wolfcode.web.feign.SeckillProductFeign;
import com.alibaba.fastjson.JSON;
import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Getter
@Setter
@Component
@RefreshScope
@Slf4j
public class InitSeckillProductJob implements SimpleJob {
    @Value("${jobCron.initSeckillProduct}")
    private String cron;

    @Qualifier("cn.wolfcode.web.feign.SeckillProductFeign")
    @Autowired
    private SeckillProductFeign seckillProductFeign;

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Override
    public void execute(ShardingContext shardingContext) {
        //获取秒杀列表集合
        String time = shardingContext.getShardingParameter();
        Result<List<SeckillProductVo>> listResult = seckillProductFeign.queryByTimeForJob(Integer.parseInt(time));
        if(listResult==null||listResult.hasError()){
            return;
        }
        List<SeckillProductVo> data = listResult.getData();
        System.out.println("************"+data);
        //删除之前数据
        //秒杀商品key
        String key= JobRedisKey.SECKILL_PRODUCT_HASH.getRealKey(time);
        //库存数量key
        String key2=JobRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(time);
        redisTemplate.delete(key);
        redisTemplate.delete(key2);
        //存储集合到redis
        for (SeckillProductVo vo:data){
            System.out.println("**8**********1234678");
            redisTemplate.opsForHash().put(key,String.valueOf(vo.getId()), JSON.toJSONString(vo));
            redisTemplate.opsForHash().put(key2,String.valueOf(vo.getId()),String.valueOf(vo.getStockCount()));
        }
    }
}
