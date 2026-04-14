package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.domain.*
import java.time.LocalDate
import ar.edu.unsam.phm.dtos.ReservaDTO
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.services.ReservaService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import kotlin.test.Test

// ─────────────────────────────────────────────────────────────────────────────
// ReservaController
// ─────────────────────────────────────────────────────────────────────────────

@WebMvcTest(ReservaController::class)
@DisplayName("Dado un ReservaController")
class ReservaControllerTest(
    @Autowired val mockMvc: MockMvc,
    @Autowired val objectMapper: ObjectMapper,
) {
    @MockBean lateinit var reservaService: ReservaService

    @Test
    @DisplayName("cuando se consultan las reservas de un usuario existente, devuelve 200 con la lista de DTOs")
    fun getReservas_ok() {
        val usuario1 = Usuario(
            credencial = Credencial("test@mail.com", "1234"),
            nombre = "Santi",
            desc = "Usuario test",
            bibliokarmas = 500 // importante para cálculos de algunos libros
        )
        val duenio = Usuario(
            credencial = Credencial("duenio@mail.com", "1234"),
            nombre = "Duenio",
            desc = "Propietario"
        )
        val libro1 = LibroComun(
            LibroData(
                duenio = duenio,
                titulo = "1984",
                autor = "George Orwell",
                descripcion = "",
                genero = Genero.CIENCIA_FICCION,
                cantidadPaginas = 300,
                isbn = "1234567890123",
                idioma = Idioma.ESPANOL,
                editorial = "Test",
                fechaPublicacion = LocalDate.of(2000, 1, 1),
                estado = EstadoLibro.EXCELENTE,
                imagenUrl = "",
                disponibilidad = Disponibilidad.DISPONIBLE
            )
        )
        libro1.id = 1

        val libro2 = LibroConDedicatoria(
            LibroData(
                duenio = duenio,
                titulo = "Dune",
                autor = "Frank Herbert",
                descripcion = "",
                genero = Genero.CIENCIA_FICCION,
                cantidadPaginas = 500,
                isbn = "1234567890124",
                idioma = Idioma.ESPANOL,
                editorial = "Test",
                fechaPublicacion = LocalDate.of(2005, 1, 1),
                estado = EstadoLibro.MUY_BUENO,
                imagenUrl = "",
                disponibilidad = Disponibilidad.DISPONIBLE
            )
        )
        libro2.id = 2
        val fechaDesde1 = LocalDate.of(2026, 3, 1)
        val fechaHasta1 = LocalDate.of(2026, 3, 10)
        val fechaDesde2 = LocalDate.of(2026, 3, 15)
        val fechaHasta2 = LocalDate.of(2026, 3, 25)

        val reservas = listOf(
            Reserva(libro1, usuario1, fechaDesde1, fechaHasta1, 1550).apply {
                id = 500
            },
            Reserva(libro2, usuario1, fechaDesde2, fechaHasta2, 1200).apply {
                id = 501
            }
        )

        whenever(reservaService.obtenerReservas(1)).thenReturn(reservas)

        mockMvc.perform(get("/api/reservas/1"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[1].id").value(2))

        verify(reservaService).obtenerReservas(1)
    }

    @Test
    @DisplayName("cuando el usuario no tiene reservas, devuelve 200 con lista vacía")
    fun getReservas_listaVacia() {
        whenever(reservaService.obtenerReservas(99)).thenReturn(emptyList())

        mockMvc.perform(get("/api/reservas/99"))
            .andExpect(status().isOk)
            .andExpect(content().json("[]"))

        verify(reservaService).obtenerReservas(99)
    }

    @Test
    @DisplayName("cuando el usuario no existe, devuelve 404 Not Found")
    fun getReservas_usuarioInexistente() {
        whenever(reservaService.obtenerReservas(999)).thenThrow(NotFoundException("Usuario no encontrado"))

        mockMvc.perform(get("/api/reservas/999"))
            .andExpect(status().isNotFound)

        verify(reservaService).obtenerReservas(999)
    }
    @Test
    @DisplayName("cuando se envían datos válidos, POST /api/reservas crea la reserva y devuelve 200")
    fun crearReserva_ok() {
        val dto = ar.edu.unsam.phm.dtos.CrearReservaDTO(
            libroId = 1,
            usuarioId = 2,
            fechaDesde = java.time.LocalDate.of(2026, 5, 1),
            fechaHasta = java.time.LocalDate.of(2026, 5, 10)
        )

        // Mockeamos que el servicio no devuelva errores
        whenever(reservaService.crearReserva(any())).thenReturn(mockk())

        mockMvc.perform(
            post("/api/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        ).andExpect(status().isOk)

        verify(reservaService).crearReserva(any())
    }
}