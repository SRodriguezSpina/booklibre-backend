package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.domain.EstadoLibro
import ar.edu.unsam.phm.domain.Genero
import ar.edu.unsam.phm.domain.Idioma
import ar.edu.unsam.phm.domain.Disponibilidad
import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.domain.LibroComun
import ar.edu.unsam.phm.domain.LibroData
import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.domain.TipoReserva
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.services.ReservaService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate
import kotlin.test.Test

@WebMvcTest(ReservaController::class)
@DisplayName("Dado un ReservaController")
class ReservaControllerTest(
    @Autowired val mockMvc: MockMvc,
    @Autowired val objectMapper: ObjectMapper,
) {
    @MockBean lateinit var reservaService: ReservaService

    private fun usuario(id: Int, nombre: String = "User$id") = Usuario(
        email = "u$id@example.com",
        password = "123",
        nombre = nombre,
        desc = "",
        celular = "+5412345678",
        ciudad = "CABA",
        bibliokarmas = 1000,
    ).apply { this.id = id }

    private fun libroComun(
        id: Int,
        titulo: String,
        autor: String,
        estado: EstadoLibro,
        duenio: Usuario,
        isbn: String = "9780000000001",
    ): LibroComun {
        val data = LibroData(
            duenio = duenio,
            titulo = titulo,
            autor = autor,
            descripcion = "Desc",
            genero = Genero.LITERATURA_CLASICA,
            cantidadPaginas = 300,
            isbn = isbn,
            idioma = Idioma.ESPANOL,
            editorial = "Ed",
            fechaPublicacion = LocalDate.of(1949, 1, 1),
            estado = estado,
            imagenUrl = "",
            fechaAgregado = LocalDate.now(),
            disponibilidad = Disponibilidad.DISPONIBLE,
        )
        return LibroComun(data).apply { this.id = id }
    }

    private fun reservaDominio(
        prestamoId: Int,
        libro: Libro,
        lector: Usuario,
        desde: LocalDate,
        hasta: LocalDate,
        karma: Int,
    ) = Reserva(libro, lector, desde, hasta).apply {
        id = prestamoId
        bibliokarmas = karma
    }

    @Test
    @DisplayName("cuando se consultan las reservas de un usuario existente, devuelve 200 con la lista de DTOs")
    fun getReservas_ok() {
        val duenio = usuario(3, "Ana")
        val lector = usuario(1, "Juan")
        val libro1 = libroComun(1, "1984", "George Orwell", EstadoLibro.EXCELENTE, duenio)
        val libro2 = libroComun(2, "Dune", "Frank Herbert", EstadoLibro.MUY_BUENO, duenio, "9780000000002")
        val reservas = listOf(
            reservaDominio(500, libro1, lector, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), 1550),
            reservaDominio(501, libro2, lector, LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 25), 1200),
        )

        whenever(reservaService.obtenerReservas(1, TipoReserva.HECHAS)).thenReturn(reservas)

        mockMvc.perform(get("/api/reservas?usuarioId=1&tipo=HECHAS"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[1].id").value(2))

        verify(reservaService).obtenerReservas(1, TipoReserva.HECHAS)
    }

    @Test
    @DisplayName("cuando el usuario no tiene reservas, devuelve 200 con lista vacía")
    fun getReservas_listaVacia() {
        whenever(reservaService.obtenerReservas(99, TipoReserva.HECHAS)).thenReturn(emptyList())

        mockMvc.perform(get("/api/reservas?usuarioId=99&tipo=HECHAS"))
            .andExpect(status().isOk)
            .andExpect(content().json("[]"))

        verify(reservaService).obtenerReservas(99, TipoReserva.HECHAS)
    }

    @Test
    @DisplayName("cuando el usuario no existe, devuelve 404 Not Found")
    fun getReservas_usuarioInexistente() {
        whenever(reservaService.obtenerReservas(999, TipoReserva.HECHAS)).thenThrow(NotFoundException("Usuario no encontrado"))

        mockMvc.perform(get("/api/reservas?usuarioId=999&tipo=HECHAS"))
            .andExpect(status().isNotFound)

        verify(reservaService).obtenerReservas(999, TipoReserva.HECHAS)
    }

    @Test
    @DisplayName("cuando se envían datos válidos, POST /api/reservas crea la reserva y devuelve 200")
    fun crearReserva_ok() {
        val dto = ar.edu.unsam.phm.dtos.CrearReservaDTO(
            libroId = 1,
            usuarioId = 2,
            fechaDesde = LocalDate.of(2026, 5, 1),
            fechaHasta = LocalDate.of(2026, 5, 10),
        )

        whenever(reservaService.crearReserva(any())).thenReturn(mockk())

        mockMvc.perform(
            post("/api/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)),
        ).andExpect(status().isOk)

        verify(reservaService).crearReserva(any())
    }
}
