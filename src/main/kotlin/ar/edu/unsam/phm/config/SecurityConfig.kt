package ar.edu.unsam.phm.config

import ar.edu.unsam.phm.exceptions.ErrorResponse
import ar.edu.unsam.phm.security.JwtAuthFilter
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Configuración de seguridad stateless con JWT.
 *
 * Encoder: BCrypt (incluido en spring-security-crypto, sin dependencias extra).
 * CORS: configurable vía la propiedad cors.allowed-origins o la variable de entorno
 *       CORS_ALLOWED_ORIGINS (lista separada por comas).
 *
 * Roles: hasRole("X") verifica la authority "ROLE_X". Las authorities se almacenan
 * con el prefijo ROLE_ tanto en UserDetailsServiceImpl como en el claim "roles" del JWT.
 * La convención es uniforme en toda la aplicación.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter,
    @Value("\${cors.allowed-origins}") private val allowedOriginsRaw: String,
) {
    private val objectMapper = ObjectMapper()

    @Bean
    @Order(2)
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.POST, "/api/login", "/api/register", "/api/auth/token", "/api/refresh", "/api/logout").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/libros/home", "/api/libros/populares").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/libros/mas-clickeado").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/libros/mejor-calificados").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/libros/*").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/libros/*/calificaciones").permitAll()
                    .requestMatchers("/api/graphql").hasRole("ADMIN")
                    .requestMatchers("/api/graphiql/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/usuarios/*/clicks").hasRole("PUBLICADOR")
                    .requestMatchers(HttpMethod.GET, "/api/reservas").authenticated()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/error").permitAll()
                    // Swagger rutas manejadas por swaggerFilterChain (Order 1)
                    // hasRole("PUBLICADOR") verifica ROLE_PUBLICADOR.
                    // Tanto PUBLICADOR como LECTOR_PUBLICADOR tienen esta authority.
                    .requestMatchers(HttpMethod.POST, "/api/libros/nuevo").hasRole("PUBLICADOR")
                    .requestMatchers(HttpMethod.PUT, "/api/libros/**").hasRole("PUBLICADOR")
                    .requestMatchers(HttpMethod.DELETE, "/api/libros/**").hasRole("PUBLICADOR")
                    //reserva controller
                    .requestMatchers(HttpMethod.POST, "/api/reservas/crear").hasRole("LECTOR")
                    .requestMatchers(HttpMethod.GET, "/api/reservas/usuarios-con-devoluciones").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/reservas/usuarios-con-mas-de-n-reservas").hasRole("ADMIN")
                    //libro controller
                    .requestMatchers(HttpMethod.POST, "/api/libros/*/calificar").hasRole("LECTOR")
                    .requestMatchers(HttpMethod.PUT, "/api/libros/*").hasRole("PUBLICADOR")
                    .requestMatchers(HttpMethod.POST, "/api/libros/nuevo").hasRole("PUBLICADOR")
                    .requestMatchers(HttpMethod.PUT, "/api/libros/baja").hasRole("PUBLICADOR")
                    .requestMatchers(HttpMethod.PUT, "/api/libros/reactivar").hasRole("PUBLICADOR")
                    .requestMatchers(HttpMethod.GET, "/api/libros/coleccionables/count").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/libros/reservas-cumplidas").hasRole("ADMIN")
                    // usuario controller
                    .requestMatchers(HttpMethod.GET, "/api/usuarios/*/reservas/anuales").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/usuarios/*/libros").hasRole("PUBLICADOR")
                    .requestMatchers(HttpMethod.GET, "/api/usuarios/*").authenticated()
                    .requestMatchers(HttpMethod.PUT, "/api/usuarios/*").authenticated()



                    .anyRequest().authenticated()
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint { request, response, _ ->
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    response.writer.write(
                        objectMapper.writeValueAsString(
                            ErrorResponse(status = 401, error = "Unauthorized", message = "Autenticación requerida", path = request.requestURI)
                        )
                    )
                }
                ex.accessDeniedHandler { request, response, _ ->
                    response.status = HttpServletResponse.SC_FORBIDDEN
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    response.writer.write(
                        objectMapper.writeValueAsString(
                            ErrorResponse(status = 403, error = "Forbidden", message = "No tenés permisos para realizar esta acción", path = request.requestURI)
                        )
                    )
                }
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOriginPatterns = allowedOriginsRaw.split(",").map { it.trim() }
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
