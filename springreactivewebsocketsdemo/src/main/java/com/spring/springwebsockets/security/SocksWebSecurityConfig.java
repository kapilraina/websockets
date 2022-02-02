package com.spring.springwebsockets.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
public class SocksWebSecurityConfig {

    @Autowired
    CustomLogoutHandler customLogoutHandler;

    @Autowired
    CustomLoginSuccessHandler customLoginSuccessHandler;

    @Autowired
    CustomLogoutSuccessHandler customLogoutSuccessHandler;

    @Autowired
    ExistingAuthPresenceFilter existingAuthPresenceFilter;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

        return http.authorizeExchange()
                .pathMatchers(HttpMethod.DELETE).denyAll()
                .pathMatchers("/login", "/login.html", "/favicon.ico", "/public", "/*.js", "/*.css", "/*.png",
                        "/static/*")
                .permitAll()
                .pathMatchers("/ws/*", "/chat.html", "/rebound", "/chat/initialdata", "/", "/logout", "/logout.html")
                .authenticated()
                .pathMatchers("/chat/broadcast/*").hasAnyAuthority("ROLE_ADMIN")
                .and()
                .formLogin()
                // .authenticationSuccessHandler(new
                // WebFilterChainServerAuthenticationSuccessHandler())
                // .authenticationSuccessHandler(customLoginSuccessHandler)
                .and()
                .logout()
                .logoutUrl("/logout")
                .logoutHandler(customLogoutHandler)
                .logoutSuccessHandler(customLogoutSuccessHandler)
                .and()
                .addFilterAfter(existingAuthPresenceFilter, SecurityWebFiltersOrder.FIRST)
                .build();

    }
}
