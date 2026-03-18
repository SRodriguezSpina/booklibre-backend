package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.dtos.LibroUsuarioItemDTO
import ar.edu.unsam.phm.dtos.UsuarioResponse
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.services.UsuarioService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(UsuarioController::class)
@DisplayName("Dado un UsuarioController")
class UsuarioControllerTest {
    @Autowired lateinit var mockMvc: MockMvc
    @MockBean lateinit var usuarioService: UsuarioService

    @Test
    @DisplayName("cuando existe el usuario, GET /usuarios/{id} devuelve 200 y el json del usuario")
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

        `when`(usuarioService.obtenerUsuario(id)).thenReturn(retorno)

        mockMvc.perform(get("/usuarios/$id"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.nombre").value("Juan"))
            .andExpect(jsonPath("$.tipoUsuario").value("Lector"))

        verify(usuarioService).obtenerUsuario(id)
    }

    @Test
    @DisplayName("cuando no existe el usuario, GET /usuarios/{id} devuelve 404")
    fun getUsuario_notFound() {
        val id = 999
        `when`(usuarioService.obtenerUsuario(id))
            .thenThrow(NotFoundException("Usuario no encontrado"))

        mockMvc.perform(get("/usuarios/$id"))
            .andExpect(status().isNotFound)

        verify(usuarioService).obtenerUsuario(id)
    }

    @Test
    @DisplayName("cuando actualiza ok, PUT /usuarios/{id} devuelve 200 con el usuario actualizado")
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

        `when`(usuarioService.actualizarUsuario(id, request)).thenReturn(retorno)

        mockMvc.perform(
            put("/usuarios/$id")
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
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.nombre").value("Juan Actualizado"))
            .andExpect(jsonPath("$.ciudad").value("Rosario"))
            .andExpect(jsonPath("$.tipoUsuario").value("Publicador"))

        verify(usuarioService).actualizarUsuario(id, request)
    }

    @Test
    @DisplayName("cuando no existe el usuario, PUT /usuarios/{id} devuelve 404")
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

        `when`(usuarioService.actualizarUsuario(id, request))
            .thenThrow(NotFoundException("Usuario no encontrado"))

        mockMvc.perform(
            put("/usuarios/$id")
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
            .andExpect(status().isNotFound)

        verify(usuarioService).actualizarUsuario(id, request)
    }

    @Test
    @DisplayName("GET /usuarios/{id}/libros devuelve 200 con lista de libros")
    fun getLibrosDeUsuario_ok() {
        val id = 1
        val retorno = listOf(
            LibroUsuarioItemDTO(
                id = 10,
                titulo = "El Libro",
                autor = "Autor",
                genero = "NOVELA",
                disponible = true,
                fechaAgregado = null
            )
        )

        `when`(usuarioService.obtenerLibrosDeUsuario(id)).thenReturn(retorno)

        mockMvc.perform(get("/usuarios/$id/libros"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id").value(10))
            .andExpect(jsonPath("$[0].disponible").value(true))

        verify(usuarioService).obtenerLibrosDeUsuario(id)
    }
}

