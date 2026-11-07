package com.halcyon.uniplace.config;

import com.halcyon.uniplace.security.LoginAttemptService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, LoginAttemptService attempts) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/coordinator/settings/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/coordinator/settings/**", "/coordinator/audit/**", "/coordinator/students/**",
                        "/coordinator/companies/**", "/coordinator/users/**", "/coordinator/applications/**",
                        "/coordinator/offers/**", "/coordinator/outbox/**", "/coordinator/tools/**")
                    .hasRole("COORDINATOR")
                .requestMatchers("/recruiter/**").hasRole("RECRUITER")
                .requestMatchers("/student/**").hasRole("STUDENT")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler((request, response, authentication) -> {
                    attempts.succeeded(authentication.getName());
                    response.sendRedirect(request.getContextPath() + "/dashboard");
                })
                .failureHandler((request, response, exception) -> {
                    String username = request.getParameter("username");
                    if (username != null && !(exception instanceof LockedException)) {
                        attempts.failed(username);
                    }
                    String reason = exception instanceof LockedException ? "locked" : "bad";
                    response.sendRedirect(request.getContextPath() + "/login?error=" + reason);
                })
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"))
            .exceptionHandling(ex -> ex.accessDeniedPage("/denied"));
        return http.build();
    }
}
