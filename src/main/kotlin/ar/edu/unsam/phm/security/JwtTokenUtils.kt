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
    @Value("\${jwt.expiration}") private val expiration: Long,
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

    fun validateToken(token: String): Boolean = runCatching { getClaims(token) }.isSuccess

    fun extractEmail(token: String): String = getClaims(token).subject

    @Suppress("UNCHECKED_CAST")
    fun extractRoles(token: String): List<String> =
        getClaims(token).get("roles", List::class.java) as List<String>

    private fun getClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
}
