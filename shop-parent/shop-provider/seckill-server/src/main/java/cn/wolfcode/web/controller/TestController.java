/**
 * author:sj
 */

package cn.wolfcode.web.controller;

import cn.wolfcode.common.constants.CommonConstants;
import cn.wolfcode.common.web.anno.RequireLogin;
import cn.wolfcode.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController

public class TestController {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @RequestMapping("/test")
    @RequireLogin
    public String test(HttpServletRequest httpServletRequest){
        String header = httpServletRequest.getHeader(CommonConstants.TOKEN_NAME);
        String userPhone = UserUtil.getUserPhone(redisTemplate, header);
        System.out.println(userPhone);
        return "ok";
    }
}
