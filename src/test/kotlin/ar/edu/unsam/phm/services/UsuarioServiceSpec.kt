package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Credencial
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.domain.UsuarioLector
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.ReservaRepository
import ar.edu.unsam.phm.repository.UsuarioRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class UsuarioServiceSpec : DescribeSpec({
    fun buildRepo(vararg usuarios: Usuario): UsuarioRepository {
        val repo = UsuarioRepository()
        usuarios.forEach { repo.create(it) }
        return repo
    }

    fun buildUsuario(
        nombre: String,
        email: String,
        celular: String
    ) = Usuario(
        credencial = Credencial(email, "1234"),
        nombre = nombre,
        desc = "desc",
        celular = celular,
        ciudad = "CABA",
        tipoUsuario = UsuarioLector()
    )

    fun buildService(repo: UsuarioRepository) = UsuarioService(repo, LibroRepository())

    describe("actualizarUsuario") {
        it("actualiza email y celular cuando son validos y no duplicados") {
            val repo = buildRepo(
                buildUsuario("Juan", "juan@mail.com", "11111111"),
                buildUsuario("Ana", "ana@mail.com", "22222222")
            )
            val service = buildService(repo)
            val idJuan = repo.lista.first { it.nombre == "Juan" }.id

            val response = service.actualizarUsuario(
                idJuan,
                UsuarioUpdateRequest(
                    nombre = "Juan Actualizado",
                    desc = "nueva desc",
                    email = "juan.nuevo@mail.com",
                    celular = "33333333",
                    ciudad = "Rosario",
                    tipoUsuario = "PUBLICADOR"
                )
            )

            response.celular shouldBe "33333333"
            response.nombre shouldBe "Juan Actualizado"
        }

        it("lanza ConflictException si el email pertenece a otro usuario") {
            val repo = buildRepo(
                buildUsuario("Juan", "juan@mail.com", "11111111"),
                buildUsuario("Ana", "ana@mail.com", "22222222")
            )
            val service = buildService(repo)
            val idJuan = repo.lista.first { it.nombre == "Juan" }.id

            shouldThrow<ConflictException> {
                service.actualizarUsuario(
                    idJuan,
                    UsuarioUpdateRequest(
                        nombre = "Juan",
                        desc = "desc",
                        email = "ana@mail.com",
                        celular = "11111111",
                        ciudad = "CABA",
                        tipoUsuario = "LECTOR"
                    )
                )
            }
        }

        it("lanza ConflictException si el celular pertenece a otro usuario") {
            val repo = buildRepo(
                buildUsuario("Juan", "juan@mail.com", "11111111"),
                buildUsuario("Ana", "ana@mail.com", "22222222")
            )
            val service = buildService(repo)
            val idJuan = repo.lista.first { it.nombre == "Juan" }.id

            shouldThrow<ConflictException> {
                service.actualizarUsuario(
                    idJuan,
                    UsuarioUpdateRequest(
                        nombre = "Juan",
                        desc = "desc",
                        email = "juan@mail.com",
                        celular = "22222222",
                        ciudad = "CABA",
                        tipoUsuario = "LECTOR"
                    )
                )
            }
        }

        it("lanza BusinessException si el email es invalido") {
            val repo = buildRepo(buildUsuario("Juan", "juan@mail.com", "11111111"))
            val service = buildService(repo)
            val idJuan = repo.lista.first().id

            shouldThrow<BusinessException> {
                service.actualizarUsuario(
                    idJuan,
                    UsuarioUpdateRequest(
                        nombre = "Juan",
                        desc = "desc",
                        email = "no-es-email",
                        celular = "11111111",
                        ciudad = "CABA",
                        tipoUsuario = "LECTOR"
                    )
                )
            }
        }

        it("lanza BusinessException si el celular es invalido") {
            val repo = buildRepo(buildUsuario("Juan", "juan@mail.com", "11111111"))
            val service = buildService(repo)
            val idJuan = repo.lista.first().id

            shouldThrow<BusinessException> {
                service.actualizarUsuario(
                    idJuan,
                    UsuarioUpdateRequest(
                        nombre = "Juan",
                        desc = "desc",
                        email = "juan@mail.com",
                        celular = "abc-123",
                        ciudad = "CABA",
                        tipoUsuario = "LECTOR"
                    )
                )
            }
        }
    }
})
