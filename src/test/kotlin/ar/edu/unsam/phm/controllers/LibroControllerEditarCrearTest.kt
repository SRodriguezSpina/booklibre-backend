package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.domain.Disponibilidad
import ar.edu.unsam.phm.domain.DuenioRef
import ar.edu.unsam.phm.domain.EstadoLibro
import ar.edu.unsam.phm.domain.Genero
import ar.edu.unsam.phm.domain.Idioma
import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.domain.LibroColeccionable
import ar.edu.unsam.phm.domain.LibroComun
import ar.edu.unsam.phm.domain.LibroConDedicatoria
import ar.edu.unsam.phm.domain.LibroData
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.security.JwtTokenUtils
import ar.edu.unsam.phm.services.LibroService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(LibroController::class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = ["PUBLICADOR"])
@DisplayName("LibroController — crear y editar libros")
class LibroControllerEditarCrearTest(
    @Autowired val mockMvc: MockMvc,
) {
    @MockBean
    lateinit var libroService: LibroService
    @MockBean
    lateinit var clickLogService: ar.edu.unsam.phm.services.ClickLogService
    @MockBean
    lateinit var jwtTokenUtils: JwtTokenUtils
    private val mapper = ObjectMapper().registerModule(JavaTimeModule())

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private fun libroDTO(
        id: Int = 1,
        titulo: String = "Clean Code",
        autor: String = "Robert C. Martin",
        tipo: String = "Comun",
        estado: String = "EXCELENTE",
        paginas: Int = 464,
        propietarioId: Int = 3,
    ) = LibroDTO(
        id = id,
        titulo = titulo,
        tipo = tipo,
        descripcion = "Buenas prácticas de programación",
        genero = Genero.DISENO,
        autor = autor,
        paginas = paginas,
        isbn = "9780132350884",
        idioma = Idioma.INGLES,
        editorial = "Prentice Hall",
        estado = estado,
        fechaPublicacion = LocalDate.of(2008, 8, 1),
        propietarioId = propietarioId,
        imagenUrl = "https://example.com/cover.jpg",
        alquiladoPorId = null,
        bibliokarma = null,
        estoyReservado = false,
        rating = 0.0,
        cantidadReservas = 0,
        ultimasCalificaciones = emptyList(),
        reservas = emptyList(),
        fechaAgregado = LocalDateTime.now(),
        cantidadCalificaciones = 0
    )

    private fun toJson(dto: LibroDTO) = mapper.writeValueAsString(dto)

    private fun duenioFixture(propietarioId: Int) = Usuario(
        email = "owner$propietarioId@example.com",
        password = "123",
        nombre = "Owner $propietarioId",
        desc = "",
        celular = "+5412345678",
        ciudad = "CABA",
        bibliokarmas = 0,
    ).apply { id = propietarioId }

    private fun libroDominio(desde: LibroDTO, id: Int = desde.id): Libro {
        val duenio = duenioFixture(desde.propietarioId)
        val data = LibroData(
            duenio = DuenioRef(id = duenio.id, nombre = duenio.nombre),
            titulo = desde.titulo,
            autor = desde.autor,
            descripcion = desde.descripcion,
            genero = desde.genero,
            cantidadPaginas = desde.paginas,
            isbn = desde.isbn,
            idioma = desde.idioma,
            editorial = desde.editorial,
            fechaPublicacion = desde.fechaPublicacion,
            estado = EstadoLibro.fromString(desde.estado),
            imagenUrl = desde.imagenUrl,
            fechaAgregado = desde.fechaAgregado,
            
        )
        val libro = when (desde.tipo) {
            "Comun" -> LibroComun(data)
            "ConDedicatoria" -> LibroConDedicatoria(data)
            "Coleccionable" -> LibroColeccionable(data)
            else -> LibroComun(data)
        }
        libro.id = id
        return libro
    }

    @Test
    @DisplayName("POST /nuevo con datos válidos devuelve 200 y el DTO creado")
    fun crearLibro_ok() {
        val dto = libroDTO()
        whenever(libroService.crearLibro(any(), any())).thenReturn(libroDominio(dto))

        mockMvc.perform(
            post("/api/libros/nuevo")
                .param("usuarioId", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(dto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.titulo").value("Clean Code"))
            .andExpect(jsonPath("$.autor").value("Robert C. Martin"))
            .andExpect(jsonPath("$.tipo").value("Comun"))
            .andExpect(jsonPath("$.propietarioId").value(3))

        verify(libroService).crearLibro(any(), eq(3))
    }

    @Test
    @DisplayName("POST /nuevo invoca crearLibro con el usuarioId correcto")
    fun crearLibro_pasaUsuarioIdAlServicio() {
        val dto = libroDTO()
        whenever(libroService.crearLibro(any(), eq(7))).thenReturn(libroDominio(dto))

        mockMvc.perform(
            post("/api/libros/nuevo")
                .param("usuarioId", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(dto))
        )

        verify(libroService).crearLibro(any(), eq(7))
    }

    @Test
    @DisplayName("POST /nuevo sin body devuelve 400 Bad Request")
    fun crearLibro_sinBody() {
        mockMvc.perform(
            post("/api/libros/nuevo")
                .param("usuarioId", "3")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("POST /nuevo sin parámetro usuarioId devuelve 400 Bad Request")
    fun crearLibro_sinUsuarioId() {
        val dto = libroDTO()

        mockMvc.perform(
            post("/api/libros/nuevo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(dto))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("POST /nuevo con tipo inválido devuelve 400 cuando el servicio lanza BusinessException")
    fun crearLibro_tipoInvalido() {
        val dto = libroDTO(tipo = "TipoInexistente")
        whenever(libroService.crearLibro(any(), any()))
            .thenThrow(BusinessException("Tipo de libro inválido: TipoInexistente"))

        mockMvc.perform(
            post("/api/libros/nuevo")
                .param("usuarioId", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(dto))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("POST /nuevo cuando el usuario propietario no existe devuelve 404")
    fun crearLibro_propietarioInexistente() {
        whenever(libroService.crearLibro(any(), any()))
            .thenThrow(NotFoundException("Usuario no encontrado"))

        mockMvc.perform(
            post("/api/libros/nuevo")
                .param("usuarioId", "9999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(libroDTO()))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("POST /nuevo con content-type incorrecto devuelve 415")
    fun crearLibro_contentTypeInvalido() {
        mockMvc.perform(
            post("/api/libros/nuevo")
                .param("usuarioId", "3")
                .contentType(MediaType.TEXT_PLAIN)
                .content("titulo=Clean Code")
        )
            .andExpect(status().isUnsupportedMediaType)
    }

    @Test
    @DisplayName("POST /nuevo el DTO de respuesta incluye el id asignado")
    fun crearLibro_respuestaConId() {
        whenever(libroService.crearLibro(any(), any())).thenReturn(libroDominio(libroDTO(), id = 42))

        mockMvc.perform(
            post("/api/libros/nuevo")
                .param("usuarioId", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(libroDTO()))
        )
            .andExpect(jsonPath("$.id").value(42))
    }

    @Test
    @DisplayName("PUT /{libroId} con datos válidos devuelve 200 y el DTO actualizado")
    fun editarLibro_ok() {
        val dtoActualizado = libroDTO(titulo = "Clean Code - Edición Revisada")
        whenever(libroService.actualizarLibro(eq(1), any(), any())).thenReturn(libroDominio(dtoActualizado))

        mockMvc.perform(
            put("/api/libros/1")
                .param("usuarioId", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(dtoActualizado))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.titulo").value("Clean Code - Edición Revisada"))

        verify(libroService).actualizarLibro(eq(1), any(), any())
    }

    @Test
    @DisplayName("PUT /{libroId} refleja el autor actualizado en la respuesta")
    fun editarLibro_actualizaAutor() {
        val dto = libroDTO(autor = "Martin Fowler")
        whenever(libroService.actualizarLibro(eq(1), any(), any())).thenReturn(libroDominio(dto))

        mockMvc.perform(
            put("/api/libros/1")
                .param("usuarioId", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(dto))
        )
            .andExpect(jsonPath("$.autor").value("Martin Fowler"))
    }

    @Test
    @DisplayName("PUT /{libroId} refleja el estado actualizado en la respuesta")
    fun editarLibro_actualizaEstado() {
        val dto = libroDTO(estado = "MALO")
        whenever(libroService.actualizarLibro(eq(1), any(), any())).thenReturn(libroDominio(dto))

        mockMvc.perform(
            put("/api/libros/1")
                .param("usuarioId", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(dto))
        )
            .andExpect(jsonPath("$.estado").value("MALO"))
    }

    @Test
    @DisplayName("PUT /{libroId} refleja las páginas actualizadas en la respuesta")
    fun editarLibro_actualizaPaginas() {
        val dto = libroDTO(paginas = 999)
        whenever(libroService.actualizarLibro(eq(1), any(), any())).thenReturn(libroDominio(dto))

        mockMvc.perform(
            put("/api/libros/1")
                .param("usuarioId", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(dto))
        )
            .andExpect(jsonPath("$.paginas").value(999))
    }

    @Test
    @DisplayName("PUT /{libroId} pasa el id de ruta al servicio, no el del body")
    fun editarLibro_usaIdDeRuta() {
        val dto = libroDTO(id = 99)
        whenever(libroService.actualizarLibro(eq(5), any(), any())).thenReturn(libroDominio(dto))

        mockMvc.perform(
            put("/api/libros/5")
                .param("usuarioId", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(dto))
        )

        verify(libroService).actualizarLibro(eq(5), any(), any())
    }

    @Test
    @DisplayName("PUT /{libroId} con libro inexistente devuelve 404")
    fun editarLibro_inexistente() {
        whenever(libroService.actualizarLibro(eq(9999), any(), any()))
            .thenThrow(NotFoundException("Libro.kt no encontrado"))

        mockMvc.perform(
            put("/api/libros/9999")
                .param("usuarioId", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(libroDTO()))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("PUT /{libroId} sin body devuelve 400 Bad Request")
    fun editarLibro_sinBody() {
        mockMvc.perform(
            put("/api/libros/1")
                .param("usuarioId", "3")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("PUT /{libroId} con id de ruta no numérico devuelve 400 Bad Request")
    fun editarLibro_idInvalido() {
        mockMvc.perform(
            put("/api/libros/abc")
                .param("usuarioId", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(libroDTO()))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("PUT /{libroId} invoca el servicio exactamente una vez")
    fun editarLibro_llamaAlServicioUnaVez() {
        whenever(libroService.actualizarLibro(any(), any(), any())).thenReturn(libroDominio(libroDTO()))

        mockMvc.perform(
            put("/api/libros/1")
                .param("usuarioId", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(libroDTO()))
        )

        verify(libroService).actualizarLibro(any(), any(), any())
    }
}