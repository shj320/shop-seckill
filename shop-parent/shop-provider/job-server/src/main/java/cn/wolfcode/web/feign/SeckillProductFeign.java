/**
 * author:sj
 */

package cn.wolfcode.web.feign;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.web.feign.fallback.SeckillProductFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name="seckill-service",fallback = SeckillProductFallback.class)
public interface SeckillProductFeign {
    @RequestMapping("/seckillProduct/queryByTimeForJob")
     Result<List<SeckillProductVo>> queryByTimeForJob(@RequestParam("time") Integer time);
}
