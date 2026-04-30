package ar.edu.unsam.phm.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Lee el header Authorization: Bearer <token>, valida la firma y expiración,
 * y carga el SecurityContext con email + roles extraídos del propio token
 * (sin llamada a base de datos por request).
 *
 * Si el header está ausente o no empieza con "Bearer ", el filtro pasa de largo
 * (request anónima). Si hay Bearer pero el token es inválido o expirado,
 * limpia el contexto y continúa como anónimo — las reglas de autorización
 * de Spring Security decidirán si el endpoint requiere autenticación.
 */
@Component
class JwtAuthFilter(
    private val jwtTokenUtils: JwtTokenUtils,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.removePrefix("Bearer ")

        if (!jwtTokenUtils.validateToken(token)) {
            SecurityContextHolder.clearContext()
            filterChain.doFilter(request, response)
            return
        }

        if (SecurityContextHolder.getContext().authentication == null) {
            val email = jwtTokenUtils.extractEmail(token)
            val roles = jwtTokenUtils.extractRoles(token).map { SimpleGrantedAuthority(it) }
            val auth = UsernamePasswordAuthenticationToken(email, null, roles)
            auth.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = auth
        }

        filterChain.doFilter(request, response)
    }
}
