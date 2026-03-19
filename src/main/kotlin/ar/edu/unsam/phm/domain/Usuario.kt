package ar.edu.unsam.phm

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
    var fechaRegistro: LocalDate = LocalDate.now(),
    val reservas: MutableList<Reserva> = mutableListOf(),
) : BaseEntity() {
    fun todasLasReservas(): List<Reserva> = reservas
    fun getCredencial(): Credencial = credencial
    fun agregarReserva(reserva: Reserva) {
        if (reservas.any { it.id == reserva.id }) throw BusinessException("La reserva ya existe para este usuario")
        reservas.add(reserva)
    }
    fun validarPassword(password: String) = password == credencial.password
}

class UsuarioLector() : TipoUsuario {}
class UsuarioPublicador() : TipoUsuario {}
class UsuarioLectorPublicador() : TipoUsuario {}