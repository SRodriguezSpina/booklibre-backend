package ar.edu.unsam.phm.domain

import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.repository.BaseEntity
import java.time.LocalDate

interface TipoUsuario {
}

class Usuario(
    private val credencial: Credencial,
    val nombre: String,
    var desc: String,
    var celular: String = "",
    var ciudad: String = "",
    var tipoUsuario: TipoUsuario = UsuarioLector(),
    var bibliokarmas: Int = 0,
    var imagenUrl: String? = null,
    var fechaRegistro: LocalDate = LocalDate.now(),
) : BaseEntity() {
    companion object {
        private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        private val celularRegex = Regex("^\\+?[0-9]{7,15}$")

        fun normalizarYValidarEmail(raw: String): String {
            val email = raw.trim().lowercase()
            if (email.isBlank() || !emailRegex.matches(email)) {
                throw BusinessException("El email no tiene un formato valido")
            }
            return email
        }

        fun normalizarYValidarCelular(raw: String): String {
            val celular = raw.trim()
            if (celular.isBlank() || !celularRegex.matches(celular)) {
                throw BusinessException("El celular no tiene un formato valido")
            }
            return celular
        }
    }

    fun getCredencial(): Credencial = credencial

    fun validarPassword(password: String) = password == credencial.password
}

class UsuarioLector() : TipoUsuario {}
class UsuarioPublicador() : TipoUsuario {}
class UsuarioLectorPublicador() : TipoUsuario {}