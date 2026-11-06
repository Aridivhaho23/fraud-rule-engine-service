package com.example.fraud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Authentication and authorisation: the principle of LEAST PRIVILEGE.
 *
 * <p>Three roles, each allowed to do only what its job needs:
 * <ul>
 *   <li><b>INGESTION</b> - the upstream system that sends transactions (POST only).</li>
 *   <li><b>ANALYST</b> - reads transactions/flags and updates flag status. Cannot create data.</li>
 *   <li><b>ADMIN</b> - everything, including customers and the merchant blacklist.</li>
 * </ul>
 * Everything not explicitly allowed is DENIED ({@code anyRequest().denyAll()}).
 *
 * <p>HTTP Basic over TLS is used to keep the example small. CSRF protection is disabled because the API is
 * stateless (no cookies/sessions), which is the standard setup for token/basic-auth APIs.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public static final String ADMIN = "ADMIN";
    public static final String ANALYST = "ANALYST";
    public static final String INGESTION = "INGESTION";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // public: health probes and API documentation
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // transactions
                        .requestMatchers(HttpMethod.POST, "/api/v1/transactions", "/api/v1/transactions/batch")
                        .hasAnyRole(INGESTION, ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/transactions/**").hasAnyRole(ANALYST, ADMIN)

                        // flags
                        .requestMatchers(HttpMethod.GET, "/api/v1/flags/**").hasAnyRole(ANALYST, ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/flags/*/status").hasAnyRole(ANALYST, ADMIN)

                        // customers
                        .requestMatchers(HttpMethod.GET, "/api/v1/customers/**").hasAnyRole(ANALYST, ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/customers").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/customers/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/customers/**").hasRole(ADMIN)

                        // blacklist
                        // The complete soft-delete history can reveal prior intelligence; ADMIN only.
                        .requestMatchers(HttpMethod.GET, "/api/v1/blacklist/history").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/blacklist/**").hasAnyRole(ANALYST, ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/blacklist/**").hasRole(ADMIN)
                        .requestMatchers("/api/v1/blacklist/**").hasRole(ADMIN)
                        // deny by default
                        .anyRequest().denyAll())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Three in-memory service accounts. Passwords are stored BCrypt-hashed, never in clear text. */
    @Bean
    public UserDetailsService userDetailsService(FraudSecurityProperties props, PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("admin").password(encoder.encode(props.adminPassword())).roles(ADMIN).build(),
                User.withUsername("analyst").password(encoder.encode(props.analystPassword())).roles(ANALYST).build(),
                User.withUsername("ingestion").password(encoder.encode(props.ingestionPassword())).roles(INGESTION).build());
    }
}
