package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.domain.Credencial
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.domain.UsuarioPublicador
import ar.edu.unsam.phm.dtos.LibroUsuarioItemDTO
import ar.edu.unsam.phm.dtos.ReservaAnualUsuarioDTO
import ar.edu.unsam.phm.dtos.AuthTokens
import ar.edu.unsam.phm.dtos.UsuarioResponse
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.security.JwtTokenUtils
import ar.edu.unsam.phm.services.UsuarioActualizadoConTokens
import ar.edu.unsam.phm.services.ReservaService
import ar.edu.unsam.phm.services.UsuarioService
import ar.edu.unsam.phm.services.ClickLogService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@WebMvcTest(UsuarioController::class)
@WithMockUser(username = "user1@mail.com")
@DisplayName("Dado un UsuarioController")
class UsuarioControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var usuarioService: UsuarioService

    @MockBean
    lateinit var reservaService: ReservaService

    @MockBean
    lateinit var jwtTokenUtils: JwtTokenUtils

    @MockBean
    lateinit var clickLogService: ClickLogService

    // Helper para crear un usuario de dominio rápidamente
    private fun crearUsuarioFake(id: Int, nombre: String): Usuario {
        return Usuario(
            email = "user$id@mail.com",
            password = "pass123",
            nombre = nombre,
            desc = "Desc",
            celular = "111222333",
            ciudad = "CABA"
        ).apply { this.id = id } // Asumiendo que BaseEntity tiene un ID
    }

    @Test
    @DisplayName("cuando existe el usuario, GET /api/usuarios/{id} devuelve 200 y el json del usuario")
    fun getUsuario_ok() {
        val id = 1
        val usuarioDominio = crearUsuarioFake(id, "Juan")

        // El mock ahora devuelve el objeto de dominio
        Mockito.`when`(usuarioService.obtenerUsuario(id)).thenReturn(usuarioDominio)

        mockMvc.perform(MockMvcRequestBuilders.get("/api/usuarios/$id"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.nombre").value("Juan"))

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
    }

    @Test
    @DisplayName("cuando actualiza ok, PUT /api/usuarios/{id} devuelve 200 con el usuario actualizado")
    fun actualizarUsuario_ok() {
        val id = 1
        val request = UsuarioUpdateRequest(
            nombre = "Juan Actualizado",
            desc = "Nueva desc",
            email = "nuevo@mail.com",
            celular = "111222333",
            ciudad = "Rosario",
            tipoUsuario = "PUBLICADOR"
        )

        // El service recibe el DTO de request pero devuelve el objeto de dominio actualizado
        val usuarioActualizado = Usuario(
            "user1@mail.com",
            "pass123",
            nombre = "Juan Actualizado",
            desc = "Nueva desc",
            celular = "111222333",
            ciudad = "Rosario",
            tipoUsuario = "PUBLICADOR"
        ).apply { this.id = id }

        Mockito.`when`(usuarioService.obtenerUsuario(id)).thenReturn(usuarioActualizado)
        Mockito.`when`(usuarioService.actualizarUsuario(id, request)).thenReturn(
            UsuarioActualizadoConTokens(
                usuario = usuarioActualizado,
                tokens = AuthTokens(accessToken = "nuevo-jwt", refreshToken = "nuevo-refresh-jwt")
            )
        )

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/usuarios/$id")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nombre": "Juan Actualizado",
                      "desc": "Nueva desc",
                      "email": "nuevo@mail.com",
                      "celular": "111222333",
                      "ciudad": "Rosario",
                      "tipoUsuario": "PUBLICADOR"
                    }
                """.trimIndent())
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.usuario.nombre").value("Juan Actualizado"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.usuario.ciudad").value("Rosario"))
            .andExpect(MockMvcResultMatchers.cookie().exists("accessToken"))
            .andExpect(MockMvcResultMatchers.cookie().httpOnly("accessToken", true))
            .andExpect(MockMvcResultMatchers.cookie().exists("refreshToken"))
            .andExpect(MockMvcResultMatchers.cookie().httpOnly("refreshToken", true))

        Mockito.verify(usuarioService).actualizarUsuario(id, request)
    }
}
