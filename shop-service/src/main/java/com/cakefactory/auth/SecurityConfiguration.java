package com.cakefactory.auth;

import com.cakefactory.account.Account;
import com.cakefactory.account.AccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository()
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SecurityContextRepository securityContextRepository) throws Exception {
        http.
                oauth2Login((oauth2Login) ->
                        oauth2Login
                                .loginPage("/login")
                )
				.formLogin((formLogin) ->
                        formLogin
                                .loginPage("/login")
                )
                .authorizeHttpRequests((authz) -> authz
                        .requestMatchers("/account/**").authenticated()
                        .anyRequest().permitAll()
                )
                .securityContext((securityContext) -> securityContext
                        .securityContextRepository(securityContextRepository)
                );;
        return http.build();
    }

    @Bean
    protected UserDetailsService userDetailsService(AccountService accountService) {
        return username -> {
            Account account = accountService.find(username);
            return new User(account.getEmail(), account.getPassword(), Collections.emptyList());
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}
