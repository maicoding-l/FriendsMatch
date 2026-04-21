package com.mai.friendsFinder.interceptor;

import com.mai.friendsFinder.constant.UserConstant;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket认证拦截器
 */
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Resource
    private SessionRepository<? extends Session> sessionRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;

            // 首先尝试从 HttpSession 获取（用于非 Redis Session 场景）
            HttpSession httpSession = servletRequest.getServletRequest().getSession(false);
            if (httpSession != null) {
                Object userId = httpSession.getAttribute(UserConstant.USER_LOGIN_STATE);
                if (userId != null) {
                    attributes.put(UserConstant.USER_LOGIN_STATE, userId);
                    return true;
                }
            }

            // 尝试从 Cookie 中获取 Spring Session ID，并从 Redis 加载
            Cookie[] cookies = servletRequest.getServletRequest().getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("SESSION".equals(cookie.getName())) {
                        String sessionId = cookie.getValue();
                        Session session = sessionRepository.findById(sessionId);
                        if (session != null) {
                            Object userId = session.getAttribute(UserConstant.USER_LOGIN_STATE);
                            if (userId != null) {
                                attributes.put(UserConstant.USER_LOGIN_STATE, userId);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
    }
}
