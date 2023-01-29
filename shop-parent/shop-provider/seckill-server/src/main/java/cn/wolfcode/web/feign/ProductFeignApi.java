/**
 * author:sj
 */

package cn.wolfcode.web.feign;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.Product;
import cn.wolfcode.web.feign.fallback.ProductFeignApiFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "product-service",fallback = ProductFeignApiFallback.class)
public interface ProductFeignApi {
    @RequestMapping("/product/queryByIds")
    Result<List<Product>> queryByIds (@RequestParam List<Long> productIds);
}
