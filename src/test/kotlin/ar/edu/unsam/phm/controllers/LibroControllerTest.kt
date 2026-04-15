package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.domain.Calificacion
import ar.edu.unsam.phm.domain.Credencial
import ar.edu.unsam.phm.domain.Disponibilidad
import ar.edu.unsam.phm.domain.EstadoLibro
import ar.edu.unsam.phm.domain.Genero
import ar.edu.unsam.phm.domain.Idioma
import ar.edu.unsam.phm.domain.LibroComun
import ar.edu.unsam.phm.domain.LibroData
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.dtos.LibroDTO
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

    private fun libroDominio(
        id: Int = 1,
        titulo: String = "1984",
        autor: String = "George Orwell",
    ): LibroComun {
        val duenio = Usuario(
            credencial = Credencial("ana@mail.com", "pass"),
            nombre = "Ana García",
            desc = "Publicadora",
        ).also { it.id = 3 }

        val libro = LibroComun(
            LibroData(
                duenio = duenio,
                titulo = titulo,
                autor = autor,
                descripcion = "Distopía",
                genero = Genero.LITERATURA_CLASICA,
                cantidadPaginas = 300,
                isbn = "9780000000001",
                idioma = Idioma.ESPANOL,
                editorial = "Debolsillo",
                fechaPublicacion = LocalDate.of(1949, 6, 8),
                estado = EstadoLibro.EXCELENTE,
                imagenUrl = "https://example.com/cover.jpg",
                fechaAgregado = LocalDate.now(),
                disponibilidad = Disponibilidad.DISPONIBLE,
            )
        )
        libro.id = id
        return libro
    }

    private fun libroDTO(
        id: Int = 1,
        titulo: String = "1984",
        autor: String = "George Orwell"
    ) = LibroDTO(
        id = id, titulo = titulo, tipo = "LibroComun", descripcion = "Distopía",
        genero = Genero.LITERATURA_CLASICA, autor = autor, paginas = 300,
        isbn = "9780000000001", idioma = Idioma.ESPANOL, editorial = "Debolsillo",
        estado = "EXCELENTE", fechaPublicacion = LocalDate.of(1949, 6, 8),
        propietarioId = 3, imagenUrl = "https://example.com/cover.jpg", alquiladoPorId = null,
        bibliokarma = 1550,
        estoyReservado = false,
        rating = 0.0,
        cantidadReservas = 0,
        ultimasCalificaciones = emptyList(),
        reservas = emptyList(),
        fechaAgregado = LocalDate.now(),
        cantidadCalificaciones = 0
    )


    @Test
    @DisplayName("cuando se buscan libros con parámetros mínimos, devuelve 200 con la respuesta paginada")
    fun search_ok() {
        val libro = libroDominio()
        val paged = PagedResponse<Libro>(content = listOf(libro), totalPages = 1, page = 0)

        whenever(libroService.search("1984", "", 0, 6, 1, null, null, null, null, null, null, "titulo")).thenReturn(paged)
        whenever(libroService.calcularBiblioKarma(any(), eq(1))).thenReturn(1550)

        mockMvc.perform(
            get("/api/libros/home")
                .param("query", "1984")
                .param("usuarioId", "1")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].titulo").value("1984"))
            .andExpect(jsonPath("$.content[0].bibliokarma").value(1550))

        verify(libroService).search("1984", "", 0, 6, 1, null, null, null, null, null, null, "titulo")
    }

    @Test
    @DisplayName("cuando se buscan libros con filtros opcionales, los pasa correctamente al service")
    fun search_conFiltros() {
        val paged = PagedResponse<Libro>(content = emptyList(), totalPages = 0, page = 0)
        whenever(libroService.search("", "", 0, 6, 1, 100, 500, "2026-01-01", "2026-12-31", null, null, "titulo")).thenReturn(paged)

        mockMvc.perform(
            get("/api/libros/home")
                .param("usuarioId", "1")
                .param("paginasMin", "100")
                .param("paginasMax", "500")
                .param("fechaDesde", "2026-01-01")
                .param("fechaHasta", "2026-12-31")
        )
            .andExpect(status().isOk)

        verify(libroService).search("", "", 0, 6, 1, 100, 500, "2026-01-01", "2026-12-31", null, null, "titulo")
    }

    @Test
    @DisplayName("cuando se busca sin resultados, devuelve 200 con lista vacía")
    fun search_sinResultados() {
        val paged = PagedResponse<Libro>(content = emptyList(), totalPages = 0, page = 0)
        whenever(libroService.search("", "", 0, 6, 1, null, null, null, null, null, null, "titulo")).thenReturn(paged)

        mockMvc.perform(get("/api/libros/home").param("usuarioId", "1"))
            .andExpect(status().isOk)
        verify(libroService).search("", "", 0, 6, 1, null, null, null, null, null, null, "titulo")
    }

    @Test
    @DisplayName("cuando se consulta un libro existente, devuelve 200 con el DTO")
    fun getLibro_ok() {
        val libro = libroDominio()
        whenever(libroService.obtenerLibro(1)).thenReturn(libro)
        whenever(libroService.obtenerReservasDeLibro(1)).thenReturn(emptyList())
        whenever(libroService.obtenerNombresUsuariosCalificaciones(libro)).thenReturn(emptyMap())

        mockMvc.perform(get("/api/libros/1").param("usuarioId", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.titulo").value("1984"))
            .andExpect(jsonPath("$.autor").value("George Orwell"))

        verify(libroService).obtenerLibro(1)
        verify(libroService).obtenerReservasDeLibro(1)
        verify(libroService).obtenerNombresUsuariosCalificaciones(libro)
    }

    @Test
    @DisplayName("cuando se consulta un libro inexistente, devuelve 404 Not Found")
    fun getLibro_inexistente() {
        whenever(libroService.obtenerLibro(999)).thenThrow(NotFoundException("Libro no encontrado"))

        mockMvc.perform(get("/api/libros/999").param("usuarioId", "1"))
            .andExpect(status().isNotFound)

        verify(libroService).obtenerLibro(999)
    }

    @Test
    @DisplayName("cuando se consulta con un libroId inválido, devuelve 400 Bad Request")
    fun getLibro_idInvalido() {
        mockMvc.perform(get("/api/libros/abc").param("usuarioId", "1"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("cuando se califica un libro correctamente, devuelve 200 OK y el nuevo rating")
    fun agregarCalificacion_ok() {
        val calificacion = Calificacion(usuarioId = 1, valor = 5, comentario = "Excelente")

        whenever(libroService.getRating(1)).thenReturn(5.0)

        mockMvc.perform(
            post("/api/libros/1/calificar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(calificacion))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.rating").value(5.0))

        verify(libroService).agregarCalificacion(1, calificacion)
        verify(libroService).getRating(1)
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
        whenever(libroService.actualizarLibro(eq(1), any(), any())).thenReturn(libroDominio(titulo = "1984 - Edición revisada"))
        mockMvc.perform(
            put("/api/libros/1")
                .param("usuarioId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.titulo").value("1984 - Edición revisada"))

        verify(libroService).actualizarLibro(eq(1), any(), any())
    }

    @Test
    @DisplayName("cuando se actualiza un libro inexistente, devuelve 404 Not Found")
    fun updateLibro_inexistente() {
        val dto = libroDTO()
        whenever(libroService.actualizarLibro(eq(999), any(), any())).thenThrow(NotFoundException("Libro no encontrado"))
        mockMvc.perform(
            put("/api/libros/999")
                .param("usuarioId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        ).andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("cuando se crea un libro con datos válidos, devuelve 200 con el DTO creado")
    fun crearLibro_ok() {
        val dto = libroDTO()
        whenever(libroService.crearLibro(any(), any())).thenReturn(libroDominio())

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