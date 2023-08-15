package com.solllus.web.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequestInterceptor implements HandlerInterceptor {

    //   /data/user/**/{userId}    :: 주소 마지막 부분에 유저아이디를 넣으면 유저 혼입 방지를 위한 검증을 해준다.
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        UserDetail principal = (UserDetail) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal.hasRole("ADMIN")) return true;

        String uri = request.getRequestURI();
        int end = uri.length(), start;
        if (uri.endsWith("/")) end--;
        start = uri.lastIndexOf("/", end) + 1;
        uri = uri.substring(start, end);

        return principal.getUsername().equals(uri);

    }

    private void out(Object obj) {
        System.out.println(obj);
    }
}
