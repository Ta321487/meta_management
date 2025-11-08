package com.metadata.interceptor;

import com.alibaba.fastjson2.JSON;
import com.metadata.common.Result;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录拦截器
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 检查是否已登录
        Object admin = request.getSession().getAttribute("admin");
        if (admin == null) {
            // 未登录，返回JSON响应
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Result result = Result.error(401, "未登录，请先登录");
            response.getWriter().write(JSON.toJSONString(result));
            return false;
        }
        return true;
    }
}

