package com.solllus.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http
                .headers().frameOptions().sameOrigin()
                .and()
                .authorizeRequests()
                .antMatchers("/certify/*/*", "/.well-known/**", "/v/**", "/v2/**",
                        "/dist/**", "/users/**", "/apps/**", "/resources/**",
                        "/data/i/**", "/data/public/**").permitAll()
                .antMatchers("/admin", "/admin/*", "/app/**", "/data/user/**", "/appstore/**").authenticated()
                .antMatchers("**").hasAnyAuthority("ADMIN")
                .anyRequest().authenticated()
                .and()
                .csrf().disable()
                .formLogin().loginPage("/")
                .defaultSuccessUrl("/redirect", true)
                .permitAll()
                .and()
                .exceptionHandling().accessDeniedPage("/errorr")
                .and()
                .logout()
                .logoutUrl("/logout") // 로그아웃 처리 URL, default: /logout, 원칙적으로 post 방식만 지원
                .logoutSuccessUrl("/login") // 로그아웃 성공 후 이동페이지
        ;
    }

    @Bean
    public HttpSessionListener httpSessionListener() {
        return new HttpSessionListener() {
            @Override
            public void sessionCreated(HttpSessionEvent se) {
                se.getSession().setMaxInactiveInterval(60 * 60 * 12);
            }

            @Override
            public void sessionDestroyed(HttpSessionEvent se) {
                HttpSessionListener.super.sessionDestroyed(se);
            }
        };
    }
}
