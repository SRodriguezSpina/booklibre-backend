package ar.edu.unsam.phm.security

import ar.edu.unsam.phm.repository.UsuarioJpaRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(private val usuarioJpa: UsuarioJpaRepository) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails {
        val usuario = usuarioJpa.findByEmail(email)
            ?: throw UsernameNotFoundException("Usuario no encontrado: $email")
        return User(usuario.email, usuario.password, rolesParaTipo(usuario.tipoUsuario))
    }

    companion object {
        fun rolesParaTipo(tipoUsuario: String): List<SimpleGrantedAuthority> =
            when (tipoUsuario.trim().uppercase().replace(" ", "_")) {
                "ADMIN" -> listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
                "PUBLICADOR" -> listOf(SimpleGrantedAuthority("ROLE_PUBLICADOR"))
                "LECTOR_PUBLICADOR", "LECTORPUBLICADOR" -> listOf(
                    SimpleGrantedAuthority("ROLE_LECTOR"),
                    SimpleGrantedAuthority("ROLE_PUBLICADOR"),
                )
                else -> listOf(SimpleGrantedAuthority("ROLE_LECTOR"))
            }
    }
}
