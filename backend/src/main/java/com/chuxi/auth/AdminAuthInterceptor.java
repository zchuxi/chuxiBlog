package com.chuxi.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 拦截 /api/admin/**，校验 Bearer token */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final TokenStore tokenStore;

    public AdminAuthInterceptor(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 CORS 预检
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        if (tokenStore.resolveBearer(request.getHeader("Authorization")) != null) return true;
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未登录\"}");
        return false;
    }
}
