package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.exceptions.UnauthorizedException
import ar.edu.unsam.phm.security.JwtTokenUtils
import ar.edu.unsam.phm.services.LibroService
import ar.edu.unsam.phm.services.ClickLogService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@WebMvcTest(LibroController::class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = ["PUBLICADOR"])
@DisplayName("Dado un LibroController")
class LibroControllerDeleteTest {
    @Autowired
    lateinit var mockMvc: MockMvc
    @MockBean
    lateinit var libroService: LibroService
    @MockBean
    lateinit var clickLogService: ClickLogService
    @MockBean
    lateinit var jwtTokenUtils: JwtTokenUtils

    @Test
    @DisplayName("cuando elimina ok, DELETE /api/libros/{id} devuelve 204")
    fun eliminarLibro_ok() {
        val libroId = 10
        val usuarioId = 1

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/libros/$libroId").param("usuarioId", usuarioId.toString()))
            .andExpect(MockMvcResultMatchers.status().isNoContent)

        Mockito.verify(libroService).eliminarLibro(libroId, usuarioId)
    }

    @Test
    @DisplayName("cuando el libro no existe, DELETE /api/libros/{id} devuelve 404")
    fun eliminarLibro_notFound() {
        val libroId = 999
        val usuarioId = 1

        Mockito.doThrow(NotFoundException("Libro.kt no encontrado"))
            .`when`(libroService).eliminarLibro(libroId, usuarioId)

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/libros/$libroId").param("usuarioId", usuarioId.toString()))
            .andExpect(MockMvcResultMatchers.status().isNotFound)

        Mockito.verify(libroService).eliminarLibro(libroId, usuarioId)
    }

    @Test
    @DisplayName("cuando el usuario no es dueño, DELETE /api/libros/{id} devuelve 401")
    fun eliminarLibro_unauthorized() {
        val libroId = 10
        val usuarioId = 2

        Mockito.doThrow(UnauthorizedException("No autorizado"))
            .`when`(libroService).eliminarLibro(libroId, usuarioId)

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/libros/$libroId").param("usuarioId", usuarioId.toString()))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)

        Mockito.verify(libroService).eliminarLibro(libroId, usuarioId)
    }

    @Test
    @DisplayName("cuando hay reserva activa, DELETE /api/libros/{id} devuelve 409")
    fun eliminarLibro_conflict() {
        val libroId = 10
        val usuarioId = 1

        Mockito.doThrow(ConflictException("No se puede eliminar "))
            .`when`(libroService).eliminarLibro(libroId, usuarioId)

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/libros/$libroId").param("usuarioId", usuarioId.toString()))
            .andExpect(MockMvcResultMatchers.status().isConflict)

        Mockito.verify(libroService).eliminarLibro(libroId, usuarioId)
    }
}