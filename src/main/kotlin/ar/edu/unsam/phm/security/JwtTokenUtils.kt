package ar.edu.unsam.phm.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenUtils(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration:900000}") private val expiration: Long, // 15 min por defecto
    @Value("\${jwt.refreshExpiration:604800000}") private val refreshExpiration: Long, // 7 días por defecto
) {
    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))
    }

    fun generateToken(email: String, roles: List<String>): String =
        Jwts.builder()
            .subject(email)
            .claim("roles", roles)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(signingKey)
            .compact()

    fun generateRefreshToken(email: String): String =
        Jwts.builder()
            .subject(email)
            .claim("type", "refresh") // Para distinguirlo de un access token
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + refreshExpiration))
            .signWith(signingKey)
            .compact()

    fun validateToken(token: String): Boolean = runCatching { getClaims(token) }.isSuccess

    fun extractEmail(token: String): String = getClaims(token).subject

    @Suppress("UNCHECKED_CAST")
    fun extractRoles(token: String): List<String> {
        val claims = getClaims(token)
        if (claims.get("type", String::class.java) == "refresh") {
            throw IllegalArgumentException("Un refresh token no puede ser usado como access token")
        }
        return claims.get("roles", List::class.java) as List<String>
    }

    private fun getClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
}
