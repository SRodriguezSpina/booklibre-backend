package ar.edu.unsam.phm.domain

import ar.edu.unsam.phm.exceptions.BusinessException
import jakarta.persistence.*
import java.time.LocalDate

interface TipoUsuario {
    fun puedePublicar(): Boolean
}

@Entity
@Table(
    name = "usuarios",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_usuarios_email", columnNames = ["email"]),
    ]
)
class Usuario(
    @Column(nullable = false) var email: String,
    @Column(nullable = false) var password: String,
    @Column(nullable = false) var nombre: String,
    @Column(name = "descripcion", nullable = false) var desc: String = "",
    @Column(nullable = true) var celular: String? = null,
    @Column(nullable = false) var ciudad: String = "",
    @Column(nullable = false) var tipoUsuario: String = "LECTOR",
    @Column(name = "es_admin", nullable = false) var esAdmin: Boolean = false,
    @Column(nullable = false) var bibliokarmas: Int = 0,
    @Column(nullable = true)  var imagenUrl: String? = null,
    @Column(nullable = false) var fechaRegistro: LocalDate = LocalDate.now(),
    @Column(nullable = false) var leidos: Int = 0,
    @Column(nullable = false) var reservados: Int = 0,
) : BaseEntity() {

    companion object {
        private val emailRegex   = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        private val celularRegex = Regex("^\\+?[0-9]{7,15}$")

        fun normalizarYValidarEmail(raw: String): String {
            val email = raw.trim().lowercase()
            if (email.isBlank() || !emailRegex.matches(email))
                throw BusinessException("El email no tiene un formato valido")
            return email
        }

        fun normalizarYValidarCelular(raw: String): String? {
            val celular = raw.trim()
            if (celular.isBlank()) return null  // celular opcional: vacío → NULL en BD
            if (!celularRegex.matches(celular))
                throw BusinessException("El celular no tiene un formato valido")
            return celular
        }
    }

    // Lo que el usuario puede cambiar de sí mismo
    fun actualizarDatosPersonales(
        nuevoNombre:    String,
        nuevaDesc:      String,
        nuevoCelular:   String?,
        nuevaCiudad:    String,
        nuevoTipo:      String,
        nuevaImagenUrl: String?,
        nuevoEmail:     String,
    ) {
        nombre    = nuevoNombre
        desc      = nuevaDesc
        celular   = nuevoCelular
        ciudad    = nuevaCiudad
        tipoUsuario = nuevoTipo
        imagenUrl = nuevaImagenUrl ?: imagenUrl
        email     = nuevoEmail
    }

    fun validarPassword(password: String) = password == this.password

    fun tipoUsuarioDomain(): TipoUsuario = when (tipoUsuario.trim().uppercase().replace(" ", "_")) {
        "PUBLICADOR"                              -> UsuarioPublicador()
        "LECTOR_PUBLICADOR", "LECTORPUBLICADOR"   -> UsuarioLectorPublicador()
        else                                      -> UsuarioLector()
    }
    fun sumaLibroReservado() {
        this.reservados += 1
    }
    fun sumaLibroLeido() {
        this.leidos += 1
    }
}

class UsuarioLector() : TipoUsuario {
    override fun puedePublicar(): Boolean = false
}
class UsuarioPublicador() : TipoUsuario {
    override fun puedePublicar(): Boolean = true
}
class UsuarioLectorPublicador() : TipoUsuario {
    override fun puedePublicar(): Boolean = true
}