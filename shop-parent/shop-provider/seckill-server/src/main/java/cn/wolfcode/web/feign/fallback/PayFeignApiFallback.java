/**
 * author:sj
 */

package cn.wolfcode.web.feign.fallback;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.PayVo;
import cn.wolfcode.domain.RefundVo;
import cn.wolfcode.web.feign.PayFeignApi;
import cn.wolfcode.web.feign.ProductFeignApi;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PayFeignApiFallback implements PayFeignApi {
    @Override
    public Result<String> payOnline(PayVo payVo) {
        return null;
    }

    @Override
    public Result<Boolean> rsaCheckV1(Map<String, String> params) {
        return null;
    }

    @Override
    public Result<Boolean> refund(RefundVo refundVo) {
        return null;
    }
}
