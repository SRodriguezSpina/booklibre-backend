package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Credencial
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.domain.UsuarioLector
import ar.edu.unsam.phm.dtos.LoginRequest
import ar.edu.unsam.phm.dtos.RegisterRequest
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.UnauthorizedException
import ar.edu.unsam.phm.testing.inmemory.repository.UsuarioRepository
import ar.edu.unsam.phm.testing.inmemory.services.AuthServiceInMemory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class AuthServiceSpec : DescribeSpec({
    fun buildRepo(vararg usuarios: Usuario): UsuarioRepository {
        val repo = UsuarioRepository()
        usuarios.forEach { repo.create(it) }
        return repo
    }

    fun usuarioExistente(
        email: String = "juan@mail.com",
        password: String = "123"
    ) = Usuario(email, password, "Juan", "", tipoUsuario = "LECTOR")

    fun buildService(repo: UsuarioRepository) = AuthServiceInMemory(repo)

    describe("login") {
        it("retorna AuthResponse exitoso con credenciales correctas") {
            val repo = buildRepo(usuarioExistente())
            val service = buildService(repo)

            val response = service.login(LoginRequest("juan@mail.com", "123"))

            response.success shouldBe true
            response.message shouldBe "Login exitoso"
            response.usuario shouldNotBe null
            response.usuario!!.email shouldBe "juan@mail.com"
        }

        it("lanza UnauthorizedException si el email no existe") {
            val repo = buildRepo(usuarioExistente())
            val service = buildService(repo)

            shouldThrow<UnauthorizedException> {
                service.login(LoginRequest("noexiste@mail.com", "123"))
            }
        }

        it("lanza UnauthorizedException si la password es incorrecta") {
            val repo = buildRepo(usuarioExistente())
            val service = buildService(repo)

            shouldThrow<UnauthorizedException> {
                service.login(LoginRequest("juan@mail.com", "wrongpassword"))
            }
        }

        it("el mensaje de error no distingue entre email y password incorrectos") {
            val repo = buildRepo(usuarioExistente())
            val service = buildService(repo)

            val errorEmailInvalido = shouldThrow<UnauthorizedException> {
                service.login(LoginRequest("noexiste@mail.com", "123"))
            }
            val errorPasswordInvalida = shouldThrow<UnauthorizedException> {
                service.login(LoginRequest("juan@mail.com", "wrongpassword"))
            }

            errorEmailInvalido.message shouldBe errorPasswordInvalida.message
        }

        it("lanza UnauthorizedException si email y password están vacíos") {
            val repo = buildRepo(usuarioExistente())
            val service = buildService(repo)

            shouldThrow<UnauthorizedException> {
                service.login(LoginRequest("", ""))
            }
        }

        it("el login es case-sensitive en la password") {
            val repo = buildRepo(usuarioExistente(password = "Secret"))
            val service = buildService(repo)

            shouldThrow<UnauthorizedException> {
                service.login(LoginRequest("juan@mail.com", "secret"))
            }
        }

        it("retorna los datos del usuario en la respuesta") {
            val repo = buildRepo(
                Usuario("ana@mail.com", "pass", "Ana García", "desc", tipoUsuario = "LECTOR")
            )
            val service = buildService(repo)

            val response = service.login(LoginRequest("ana@mail.com", "pass"))

            response.usuario!!.nombre shouldBe "Ana García"
            response.usuario!!.email shouldBe "ana@mail.com"
        }
    }

    describe("register") {
        it("registra un usuario nuevo correctamente") {
            val repo = buildRepo()
            val service = buildService(repo)

            val response = service.register(
                RegisterRequest("Carlos", "carlos@mail.com", "pass123")
            )

            response.success shouldBe true
            response.message shouldBe "Registro exitoso"
            response.usuario shouldNotBe null
            response.usuario!!.email shouldBe "carlos@mail.com"
        }

        it("lanza ConflictException si el email ya está registrado") {
            val repo = buildRepo(usuarioExistente(email = "repetido@mail.com"))
            val service = buildService(repo)

            shouldThrow<ConflictException> {
                service.register(RegisterRequest("Otro", "repetido@mail.com", "pass"))
            }
        }

        it("permite registrar dos usuarios con emails distintos") {
            val repo = buildRepo(usuarioExistente(email = "primero@mail.com"))
            val service = buildService(repo)

            val response = service.register(
                RegisterRequest("Segundo", "segundo@mail.com", "pass")
            )

            response.success shouldBe true
            repo.lista.size shouldBe 2
        }

        it("el nuevo usuario puede hacer login inmediatamente después del registro") {
            val repo = buildRepo()
            val service = buildService(repo)

            service.register(RegisterRequest("Nuevo", "nuevo@mail.com", "mipass"))

            val loginResponse = service.login(LoginRequest("nuevo@mail.com", "mipass"))
            loginResponse.success shouldBe true
        }
    }
})