package com.nexus.nkap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * 基础安全配置（Step 0 阶段占位）。
 *
 * <p>暂时放行所有请求并禁用 CSRF，便于后续接口联调时不被默认登录页/401 拦截；
 * 正式鉴权（JWT/OIDC、文档级 ACL、租户隔离）将在 Step 5 及后续步骤补齐。
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * 响应式安全过滤链：放行全部请求 + 关闭 CSRF。
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
                .build();
    }
}
