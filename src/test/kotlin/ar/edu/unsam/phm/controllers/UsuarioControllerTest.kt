package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.dtos.LibroUsuarioItemDTO
import ar.edu.unsam.phm.dtos.ReservaAnualUsuarioDTO
import ar.edu.unsam.phm.dtos.UsuarioResponse
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.services.ReservaService
import ar.edu.unsam.phm.services.UsuarioService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@WebMvcTest(UsuarioController::class)
@DisplayName("Dado un UsuarioController")
class UsuarioControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc
    @MockBean
    lateinit var usuarioService: UsuarioService
    @MockBean
    lateinit var reservaService: ReservaService

    @Test
    @DisplayName("cuando existe el usuario, GET /api/usuarios/{id} devuelve 200 y el json del usuario")
    fun getUsuario_ok() {
        val id = 1
        val retorno = UsuarioResponse(
            id = 1,
            nombre = "Juan",
            desc = "Desc",
            email = "juan@mail.com",
            fechaRegistro = "2026-03-16",
            celular = "111",
            ciudad = "CABA",
            tipoUsuario = "Lector",
            bibliokarmas = 10
        )

        Mockito.`when`(usuarioService.obtenerUsuario(id)).thenReturn(retorno)

        mockMvc.perform(MockMvcRequestBuilders.get("/api/usuarios/$id"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.nombre").value("Juan"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.tipoUsuario").value("Lector"))

        Mockito.verify(usuarioService).obtenerUsuario(id)
    }

    @Test
    @DisplayName("cuando no existe el usuario, GET /api/usuarios/{id} devuelve 404")
    fun getUsuario_notFound() {
        val id = 999
        Mockito.`when`(usuarioService.obtenerUsuario(id))
            .thenThrow(NotFoundException("Usuario no encontrado"))

        mockMvc.perform(MockMvcRequestBuilders.get("/api/usuarios/$id"))
            .andExpect(MockMvcResultMatchers.status().isNotFound)

        Mockito.verify(usuarioService).obtenerUsuario(id)
    }

    @Test
    @DisplayName("cuando actualiza ok, PUT /api/usuarios/{id} devuelve 200 con el usuario actualizado")
    fun actualizarUsuario_ok() {
        val id = 1
        val request = UsuarioUpdateRequest(
            nombre = "Juan Actualizado",
            desc = "Nueva desc",
            email = "nuevo@mail.com",
            celular = "222",
            ciudad = "Rosario",
            tipoUsuario = "PUBLICADOR"
        )
        val retorno = UsuarioResponse(
            id = 1,
            nombre = "Juan Actualizado",
            desc = "Nueva desc",
            email = "nuevo@mail.com",
            fechaRegistro = "2026-03-16",
            celular = "222",
            ciudad = "Rosario",
            tipoUsuario = "Publicador",
            bibliokarmas = 10
        )

        Mockito.`when`(usuarioService.actualizarUsuario(id, request)).thenReturn(retorno)

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/usuarios/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "nombre": "Juan Actualizado",
                      "desc": "Nueva desc",
                      "email": "nuevo@mail.com",
                      "celular": "222",
                      "ciudad": "Rosario",
                      "tipoUsuario": "PUBLICADOR"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.nombre").value("Juan Actualizado"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.ciudad").value("Rosario"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.tipoUsuario").value("Publicador"))

        Mockito.verify(usuarioService).actualizarUsuario(id, request)
    }

    @Test
    @DisplayName("cuando no existe el usuario, PUT /api/usuarios/{id} devuelve 404")
    fun actualizarUsuario_notFound() {
        val id = 999
        val request = UsuarioUpdateRequest(
            nombre = "X",
            desc = "X",
            email = "x@mail.com",
            celular = "0",
            ciudad = "X",
            tipoUsuario = "LECTOR"
        )

        Mockito.`when`(usuarioService.actualizarUsuario(id, request))
            .thenThrow(NotFoundException("Usuario no encontrado"))

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/usuarios/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "nombre": "X",
                      "desc": "X",
                      "email": "x@mail.com",
                      "celular": "0",
                      "ciudad": "X",
                      "tipoUsuario": "LECTOR"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)

        Mockito.verify(usuarioService).actualizarUsuario(id, request)
    }

    @Test
    @DisplayName("cuando se consultan las reservas anuales del perfil, GET /api/usuarios/{id}/reservas/anuales devuelve 200")
    fun getReservasAnuales_ok() {
        val id = 1
        val retorno = listOf(
            ReservaAnualUsuarioDTO(
                reservaId = 500,
                usuarioId = 1,
                usuarioNombre = "Juan Pérez",
                libroId = 9,
                libroTitulo = "Harry Potter y la Piedra Filosofal",
                libroAutor = "J.K. Rowling",
                fechaDesde = java.time.LocalDate.of(2026, 3, 1),
                fechaHasta = java.time.LocalDate.of(2026, 3, 10),
                anioReserva = 2026,
            )
        )

        Mockito.`when`(reservaService.obtenerReservasAnuales(id)).thenReturn(retorno)

        mockMvc.perform(MockMvcRequestBuilders.get("/api/usuarios/$id/reservas/anuales"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].usuarioId").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].libroTitulo").value("Harry Potter y la Piedra Filosofal"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].anioReserva").value(2026))

        Mockito.verify(reservaService).obtenerReservasAnuales(id)
    }

    //@Test
    //@DisplayName("GET /api/usuarios/{id}/libros devuelve 200 con lista de libros")
    //TODO(REALIZAR TEST)
}