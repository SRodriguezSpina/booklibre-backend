package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.dtos.ReservaDTO
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.services.ReservaService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
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
        val dtos = listOf(
            ReservaDTO(
                id = 1, prestamoId = 500,
                titulo = "1984", autor = "George Orwell",
                estado = "EXCELENTE", propietarioId = 3,
                imagenUrl = "", alquiladoPorId = 1,
                fechaDesde = "2026-03-01", fechaHasta = "2026-03-10",
                rating = 0.0, bibliokarma = 1550
            ),
            ReservaDTO(
                id = 2, prestamoId = 501,
                titulo = "Dune", autor = "Frank Herbert",
                estado = "MUY_BUENO", propietarioId = 3,
                imagenUrl = "", alquiladoPorId = 1,
                fechaDesde = "2026-03-15", fechaHasta = "2026-03-25",
                rating = 4.0, bibliokarma = 1200
            )
        )

        whenever(reservaService.obtenerReservas(1)).thenReturn(dtos)

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

        val reservaMock = mock(ar.edu.unsam.phm.domain.Reserva::class.java)

        whenever(reservaService.crearReserva(any())).thenReturn(reservaMock)

        mockMvc.perform(
            post("/api/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        ).andExpect(status().isOk)

        verify(reservaService).crearReserva(any())
    }
}