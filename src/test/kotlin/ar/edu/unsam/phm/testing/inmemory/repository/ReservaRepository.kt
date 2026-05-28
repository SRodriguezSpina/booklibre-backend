package ar.edu.unsam.phm.testing.inmemory.repository

import ar.edu.unsam.phm.domain.Reserva

class ReservaRepository(private val libroRepository: LibroRepository) : CommonRepository<Reserva>() {
    fun findByLibroId(libroId: Int): List<Reserva> {
        return lista.filter { reserva -> reserva.libroId == libroId }
    }

    fun getReservasDeUsuario(usuarioId: Int): List<Reserva> {
        return lista.filter { reserva ->
            val libro = try {
                libroRepository.getById(reserva.libroId)
            } catch (e: Exception) {
                null
            }
            val esDuenio = libro?.duenio?.id == usuarioId
            val esSolicitante = reserva.usuario.id == usuarioId

            esDuenio || esSolicitante
        }
    }
}