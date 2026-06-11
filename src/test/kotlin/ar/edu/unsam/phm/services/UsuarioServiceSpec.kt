package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Credencial
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.domain.UsuarioLector
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.security.UserDetailsServiceImpl
import ar.edu.unsam.phm.testing.inmemory.repository.LibroRepository
import ar.edu.unsam.phm.testing.inmemory.repository.ReservaRepository
import ar.edu.unsam.phm.testing.inmemory.repository.UsuarioRepository
import ar.edu.unsam.phm.testing.inmemory.services.UsuarioServiceInMemory
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
        email, "1234",
        nombre = nombre,
        desc = "desc",
        celular = celular,
        ciudad = "CABA",
        tipoUsuario = "LECTOR"
    )

    fun buildService(repo: UsuarioRepository): UsuarioServiceInMemory {
        val lr = LibroRepository()
        return UsuarioServiceInMemory(repo, lr, ReservaRepository(lr))
    }

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

            response.email shouldBe "juan.nuevo@mail.com"
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

        it("un admin que cambia su tipo conserva el rol de admin y suma los roles del nuevo tipo") {
            val admin = buildUsuario("Admin", "admin@booklibre.com", "00000000")
            admin.tipoUsuario = "ADMIN"
            admin.esAdmin = true
            val repo = buildRepo(admin)
            val service = buildService(repo)
            val idAdmin = repo.lista.first().id

            val response = service.actualizarUsuario(
                idAdmin,
                UsuarioUpdateRequest(
                    nombre = "Admin",
                    desc = "desc",
                    email = "admin@booklibre.com",
                    celular = "00000000",
                    ciudad = "CABA",
                    tipoUsuario = "LECTOR_PUBLICADOR"
                )
            )

            response.tipoUsuario shouldBe "LectorPublicador"
            response.esAdmin shouldBe true
            val actualizado = repo.lista.first { it.id == idAdmin }
            UserDetailsServiceImpl.rolesParaUsuario(actualizado).map { it.authority } shouldBe
                listOf("ROLE_LECTOR", "ROLE_PUBLICADOR", "ROLE_ADMIN")
        }

        it("lanza BusinessException si un usuario no admin intenta asignarse el tipo ADMIN") {
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
                        celular = "11111111",
                        ciudad = "CABA",
                        tipoUsuario = "ADMIN"
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
