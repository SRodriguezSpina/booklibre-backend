package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.domain.Calificacion
import ar.edu.unsam.phm.domain.Genero
import ar.edu.unsam.phm.domain.Idioma
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.LibroHomeDTO
import ar.edu.unsam.phm.dtos.PagedResponse
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.services.LibroService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
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

@WebMvcTest(LibroController::class)
@DisplayName("Dado un LibroController")
class LibroControllerTest(
    @Autowired val mockMvc: MockMvc,
    @Autowired val objectMapper: ObjectMapper,
) {
    @MockBean lateinit var libroService: LibroService

    private fun libroDTO(
        id: Int = 1,
        titulo: String = "1984",
        autor: String = "George Orwell"
    ) = LibroDTO(
        id = id, titulo = titulo, tipo = "LibroComun", descripcion = "Distopía",
        genero = Genero.LITERATURA_CLASICA, autor = autor, paginas = 300,
        isbn = "ISBN-001", idioma = Idioma.ESPANOL, editorial = "Debolsillo",
        estado = "EXCELENTE", fechaPublicacion = LocalDate.of(1949, 6, 8),
        propietarioId = 3, imagenUrl = "", alquiladoPorId = null,
        bibliokarma = 1550,
        estoyReservado = false,
        autorImg = "",
        rating = 0.0,
        reservas = emptyList(),
        cantidadReservas = 0
    )

    private fun libroHomeDTO() = LibroHomeDTO(
        id = 1, imagenUrl = "", genero = Genero.LITERATURA_CLASICA,
        titulo = "1984", autor = "George Orwell", calificacion = 4.5,
        isbn = "ISBN-001", idioma = Idioma.ESPANOL, tipo = "LibroComun",
        bibliokarma = 1550, estado = "EXCELENTE", duenio = "Ana García"
    )

    @Test
    @DisplayName("cuando se buscan libros con parámetros mínimos, devuelve 200 con la respuesta paginada")
    fun search_ok() {
        val paged = PagedResponse(content = listOf(libroHomeDTO()), totalPages = 1, page = 0)
        whenever(libroService.search("1984", "", 0, 6, 1, null, null, null, null, null, null)).thenReturn(paged)

        mockMvc.perform(
            get("/api/libros/home")
                .param("query", "1984")
                .param("usuarioId", "1")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].titulo").value("1984"))

        verify(libroService).search("1984", "", 0, 6, 1, null, null, null, null, null, null)
    }

    @Test
    @DisplayName("cuando se buscan libros con filtros opcionales, los pasa correctamente al service")
    fun search_conFiltros() {
        val paged = PagedResponse(content = emptyList<LibroHomeDTO>(), totalPages = 0, page = 0)
        whenever(libroService.search("", "", 0, 6, 1, 100, 500, "2026-01-01", "2026-12-31", null, null)).thenReturn(paged)

        mockMvc.perform(
            get("/api/libros/home")
                .param("usuarioId", "1")
                .param("paginasMin", "100")
                .param("paginasMax", "500")
                .param("fechaDesde", "2026-01-01")
                .param("fechaHasta", "2026-12-31")
        )
            .andExpect(status().isOk)

        verify(libroService).search("", "", 0, 6, 1, 100, 500, "2026-01-01", "2026-12-31", null, null)
    }

    @Test
    @DisplayName("cuando se busca sin resultados, devuelve 200 con lista vacía")
    fun search_sinResultados() {
        val paged = PagedResponse(content = emptyList<LibroHomeDTO>(), totalPages = 0, page = 0)
        whenever(libroService.search("", "", 0, 6, 1, null, null, null, null, null, null)).thenReturn(paged)

        mockMvc.perform(get("/api/libros/home").param("usuarioId", "1"))
            .andExpect(status().isOk)
    }

    @Test
    @DisplayName("cuando se consulta un libro existente, devuelve 200 con el DTO")
    fun getLibro_ok() {
        whenever(libroService.getLibro(1)).thenReturn(libroDTO())

        mockMvc.perform(get("/api/libros/1").param("usuarioId", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.titulo").value("1984"))
            .andExpect(jsonPath("$.autor").value("George Orwell"))
            .andExpect(jsonPath("$.bibliokarma").value(1550))

        verify(libroService).getLibro(1)
    }

    @Test
    @DisplayName("cuando se consulta un libro inexistente, devuelve 404 Not Found")
    fun getLibro_inexistente() {
        whenever(libroService.getLibro(999)).thenThrow(NotFoundException("Libro no encontrado"))

        mockMvc.perform(get("/api/libros/999").param("usuarioId", "1"))
            .andExpect(status().isNotFound)

        verify(libroService).getLibro(999)
    }

    @Test
    @DisplayName("cuando se consulta con un libroId inválido, devuelve 400 Bad Request")
    fun getLibro_idInvalido() {
        mockMvc.perform(get("/api/libros/abc").param("usuarioId", "1"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("cuando se califica un libro correctamente, devuelve 200 OK")
    fun agregarCalificacion_ok() {
        val calificacion = Calificacion(usuarioId = 1, valor = 5, comentario = "Excelente")

        mockMvc.perform(
            post("/api/libros/1/calificar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(calificacion))
        ).andExpect(status().isOk)

        verify(libroService).agregarCalificacion(1, calificacion)
    }

    @Test
    @DisplayName("cuando se califica un libro inexistente, devuelve 404 Not Found")
    fun agregarCalificacion_libroInexistente() {
        val calificacion = Calificacion(usuarioId = 1, valor = 4, comentario = "Bueno")
        whenever(libroService.agregarCalificacion(999, calificacion)).thenThrow(NotFoundException("Libro no encontrado"))

        mockMvc.perform(
            post("/api/libros/999/calificar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(calificacion))
        ).andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("cuando un usuario intenta calificar dos veces el mismo libro, devuelve 400 Bad Request")
    fun agregarCalificacion_duplicada() {
        val calificacion = Calificacion(usuarioId = 1, valor = 3, comentario = "Ya califiqué")
        whenever(libroService.agregarCalificacion(1, calificacion)).thenThrow(BusinessException("Usuario ya calificó este libro"))

        mockMvc.perform(
            post("/api/libros/1/calificar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(calificacion))
        ).andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("cuando se envía body vacío al calificar, devuelve 400 Bad Request")
    fun agregarCalificacion_sinBody() {
        mockMvc.perform(
            post("/api/libros/1/calificar")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("cuando se actualiza un libro existente, devuelve 200 con el DTO actualizado")
    fun updateLibro_ok() {
        val dto = libroDTO(titulo = "1984 - Edición revisada")
        whenever(libroService.actualizarLibro(eq(1), any())).thenReturn(dto)
        mockMvc.perform(
            put("/api/libros/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.titulo").value("1984 - Edición revisada"))

        verify(libroService).actualizarLibro(eq(1), any())
    }

    @Test
    @DisplayName("cuando se actualiza un libro inexistente, devuelve 404 Not Found")
    fun updateLibro_inexistente() {
        val dto = libroDTO()
        whenever(libroService.actualizarLibro(eq(999), any())).thenThrow(NotFoundException("Libro no encontrado"))
        mockMvc.perform(
            put("/api/libros/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        ).andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("cuando se crea un libro con datos válidos, devuelve 200 con el DTO creado")
    fun crearLibro_ok() {
        val dto = libroDTO()
        whenever(libroService.crearLibro(any(), any())).thenReturn(dto)

        mockMvc.perform(
            post("/api/libros/nuevo")
                .param("usuarioId", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.titulo").value("1984"))

        verify(libroService).crearLibro(any(), any())
    }

    @Test
    @DisplayName("cuando se crea un libro sin body, devuelve 400 Bad Request")
    fun crearLibro_sinBody() {
        mockMvc.perform(
            post("/api/libros/nuevo")
                .param("usuarioId", "3")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest)
    }
}