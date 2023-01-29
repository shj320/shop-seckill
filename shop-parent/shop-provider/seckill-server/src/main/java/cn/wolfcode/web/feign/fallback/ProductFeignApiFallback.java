/**
 * author:sj
 */

package cn.wolfcode.web.feign.fallback;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.Product;
import cn.wolfcode.web.feign.ProductFeignApi;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public class ProductFeignApiFallback implements ProductFeignApi {
    @Override
    public Result<List<Product>> queryByIds(List<Long> productIds) {
        return null;
    }
}
