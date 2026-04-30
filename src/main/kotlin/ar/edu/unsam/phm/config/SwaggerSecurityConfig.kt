package ar.edu.unsam.phm.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SwaggerSecurityConfig {

    @Bean
    @Order(1)
    fun swaggerFilterChain(
        http: HttpSecurity,
        userDetailsService: UserDetailsService,
        passwordEncoder: PasswordEncoder,
    ): SecurityFilterChain {
        val provider = DaoAuthenticationProvider().apply {
            setUserDetailsService(userDetailsService)
            setPasswordEncoder(passwordEncoder)
        }
        http
            .securityMatcher("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authenticationProvider(provider)
            .authorizeHttpRequests { auth ->
                auth.anyRequest().hasRole("ADMIN")
            }
            .httpBasic { }
        return http.build()
    }
}
