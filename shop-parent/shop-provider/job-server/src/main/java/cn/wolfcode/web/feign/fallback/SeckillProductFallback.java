/**
 * author:sj
 */

package cn.wolfcode.web.feign.fallback;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.web.feign.SeckillProductFeign;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public class SeckillProductFallback implements SeckillProductFeign {
    @Override
    public Result<List<SeckillProductVo>> queryByTimeForJob(Integer time) {
        return null;
    }
}
