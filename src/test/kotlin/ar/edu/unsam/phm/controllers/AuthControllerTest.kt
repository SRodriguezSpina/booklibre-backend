package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.dtos.AuthResponseUsuario
import ar.edu.unsam.phm.dtos.LoginRequest
import ar.edu.unsam.phm.dtos.RegisterRequest
import ar.edu.unsam.phm.dtos.UsuarioResponse
import ar.edu.unsam.phm.dtos.AuthTokens
import ar.edu.unsam.phm.exceptions.AuthExceptionHandler
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.UnauthorizedException
import ar.edu.unsam.phm.security.JwtTokenUtils
import ar.edu.unsam.phm.services.AuthService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AuthExceptionHandler::class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var mapper: ObjectMapper

    @MockBean
    lateinit var authService: AuthService

    @MockBean
    lateinit var jwtTokenUtils: JwtTokenUtils

    // ── Fixtures ──────────────────────────────────────────────────────────

    val usuarioMock = UsuarioResponse(
        id = 1,
        nombre = "Juan Pérez",
        email = "juan@mail.com",
        desc = "",
        fechaRegistro = "2024-01-01",
        celular = "",
        ciudad = "",
        tipoUsuario = "Lector",
        bibliokarmas = 0,
        reservados = 0,
        leidos = 0
    )

    val loginExitoso = AuthResponseUsuario(
        success = true,
        message = "Login exitoso",
        usuario = usuarioMock
    )

    val tokensMock = AuthTokens("access-token", "refresh-token")

    val registroExitoso = AuthResponseUsuario(
        success = true,
        message = "Registro exitoso",
        usuario = usuarioMock
    )

    fun loginBody(email: String = "juan@mail.com", password: String = "123") =
        mapper.writeValueAsString(LoginRequest(email, password))

    fun registerBody(
        nombre: String = "Juan",
        email: String = "juan@mail.com",
        password: String = "123456"
    ) = mapper.writeValueAsString(RegisterRequest(nombre, email, password))

    @Test
    @DisplayName("login exitoso retorna 200 con datos del usuario")
    fun loginExitoso() {
        `when`(authService.login(LoginRequest("juan@mail.com", "123")))
            .thenReturn(Pair(loginExitoso, tokensMock))

        mockMvc.perform(
            post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Login exitoso"))
            .andExpect(jsonPath("$.usuario.id").value(1))
            .andExpect(jsonPath("$.usuario.email").value("juan@mail.com"))
            .andExpect(jsonPath("$.usuario.nombre").value("Juan Pérez"))
            .andExpect(cookie().exists("accessToken"))
            .andExpect(cookie().httpOnly("accessToken", true))
            .andExpect(cookie().exists("refreshToken"))
            .andExpect(cookie().httpOnly("refreshToken", true))
    }

    @Test
    @DisplayName("login con email inexistente retorna 401")
    fun loginEmailInexistente() {
        `when`(authService.login(LoginRequest("noexiste@mail.com", "123")))
            .thenThrow(UnauthorizedException("Usuario o contraseña incorrectos"))

        mockMvc.perform(
            post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(email = "noexiste@mail.com"))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"))
    }

    @Test
    @DisplayName("login con password incorrecta retorna 401")
    fun loginPasswordIncorrecta() {
        `when`(authService.login(LoginRequest("juan@mail.com", "wrongpassword")))
            .thenThrow(UnauthorizedException("Usuario o contraseña incorrectos"))

        mockMvc.perform(
            post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(password = "wrongpassword"))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"))
    }

    @Test
    @DisplayName("login con email y password incorrectos retorna el mismo mensaje que password incorrecta")
    fun loginEmailYPasswordIncorrectos() {
        `when`(authService.login(LoginRequest("noexiste@mail.com", "wrongpassword")))
            .thenThrow(UnauthorizedException("Usuario o contraseña incorrectos"))

        mockMvc.perform(
            post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(email = "noexiste@mail.com", password = "wrongpassword"))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"))
    }

    @Test
    @DisplayName("login llama al servicio con los datos del body")
    fun loginLlamaAlServicio() {
        `when`(authService.login(LoginRequest("ana@mail.com", "mypass")))
            .thenReturn(Pair(loginExitoso, tokensMock))

        mockMvc.perform(
            post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody("ana@mail.com", "mypass"))
        )

        verify(authService).login(LoginRequest("ana@mail.com", "mypass"))
    }

    @Test
    @DisplayName("login retorna 400 si el body esta vacio")
    fun loginBodyVacio() {
        mockMvc.perform(
            post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("login retorna 415 si el content type no es JSON")
    fun loginContentTypeInvalido() {
        mockMvc.perform(
            post("/api/login")
                .contentType(MediaType.TEXT_PLAIN)
                .content("juan@mail.com:123")
        )
            .andExpect(status().isUnsupportedMediaType)
    }

    @Test
    @DisplayName("registro exitoso retorna 200 con datos del usuario")
    fun registroExitoso() {
        `when`(authService.register(RegisterRequest("Juan", "juan@mail.com", "123456")))
            .thenReturn(Pair(registroExitoso, tokensMock))

        mockMvc.perform(
            post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Registro exitoso"))
            .andExpect(jsonPath("$.usuario.email").value("juan@mail.com"))
            .andExpect(cookie().exists("accessToken"))
            .andExpect(cookie().httpOnly("accessToken", true))
            .andExpect(cookie().exists("refreshToken"))
            .andExpect(cookie().httpOnly("refreshToken", true))
    }

    @Test
    @DisplayName("registro con email duplicado retorna 409")
    fun registroEmailDuplicado() {
        `when`(authService.register(RegisterRequest("Juan", "repetido@mail.com", "123456")))
            .thenThrow(ConflictException("El email ya está registrado"))

        mockMvc.perform(
            post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email = "repetido@mail.com"))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("El email ya está registrado"))
    }

    @Test
    @DisplayName("registro llama al servicio con los datos del body")
    fun registroLlamaAlServicio() {
        `when`(authService.register(RegisterRequest("Carlos", "carlos@mail.com", "pass123")))
            .thenReturn(Pair(registroExitoso, tokensMock))

        mockMvc.perform(
            post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("Carlos", "carlos@mail.com", "pass123"))
        )

        verify(authService).register(RegisterRequest("Carlos", "carlos@mail.com", "pass123"))
    }

    @Test
    @DisplayName("registro retorna 400 si el body esta vacio")
    fun registroBodyVacio() {
        mockMvc.perform(
            post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("registro retorna 415 si el content type no es JSON")
    fun registroContentTypeInvalido() {
        mockMvc.perform(
            post("/api/register")
                .contentType(MediaType.TEXT_PLAIN)
                .content("juan:juan@mail.com:123456")
        )
            .andExpect(status().isUnsupportedMediaType)
    }

    @Test
    @DisplayName("registro llama al servicio exactamente una vez")
    fun registroLlamaAlServicioUnaVez() {
        `when`(authService.register(RegisterRequest("Juan", "juan@mail.com", "123456")))
            .thenReturn(Pair(registroExitoso, tokensMock))

        mockMvc.perform(
            post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody())
        )

        verify(authService).register(RegisterRequest("Juan", "juan@mail.com", "123456"))
    }
}