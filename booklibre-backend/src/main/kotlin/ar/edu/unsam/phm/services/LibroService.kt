package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.Libro
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.repository.LibroRepository
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class LibroService(val libroRepository: LibroRepository) {
    fun getLibro(id: Int): Libro = libroRepository.getById(id)

    fun obtenerPrestamos(usuarioId: Int, tipo: String): List<LibroDTO> {
        val libros = libroRepository.getLibros()

        val filtrados = when (tipo) {
            "prestados-a-mi" -> libros.filter { libro ->
                libro.reservas.any { it.usuario.id == usuarioId }
            }
            "prestados-por-mi" -> libros.filter { it.duenio.id == usuarioId && it.reservas.isNotEmpty() }
            else -> emptyList()
        }

        return filtrados.map { it.toDTO(usuarioId) }
    }

    // Extension function para mapear el Dominio al DTO
    private fun Libro.toDTO(usuarioConsultaId: Int): LibroDTO {
        val ultimaReserva = this.reservas.lastOrNull()
        return LibroDTO(
            id = this.id,
            titulo = this.titulo,
            autor = this.autor,
            tipo = this.javaClass.simpleName.replace("Libro", ""),
            paginas = this.cantidadPaginas,
            estado = this.estado.name,
            propietarioId = this.duenio.id,
            alquiladoPorId = ultimaReserva?.usuario?.id,
            fechaDevolucion = ultimaReserva?.fechaHasta?.format(DateTimeFormatter.ISO_DATE),
            bibliokarma = ultimaReserva?.let {
                this.calcularBiblioKarmaTotal(it.usuario, it.fechaDesde, it.fechaHasta)
            }
        )
    }
}