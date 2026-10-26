package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest
import ar.edu.unsam.phm.repository.UsuarioJpaRepository
import ar.edu.unsam.phm.repository.ReservaJpaRepository
import ar.edu.unsam.phm.repository.mongo.LibroMongoRepository
import ar.edu.unsam.phm.repository.redis.TopBibliokarmasCacheRepository
import ar.edu.unsam.phm.security.JwtTokenUtils
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.Optional

class UsuarioServiceSpringSpec : DescribeSpec({
    val usuarioJpa = mockk<UsuarioJpaRepository>()
    val libroRepository = mockk<LibroMongoRepository>()
    val reservaJpa = mockk<ReservaJpaRepository>()
    val cacheRepository = mockk<TopBibliokarmasCacheRepository>()
    val jwtTokenUtils = JwtTokenUtils("test-secret-key-that-is-long-enough", 900000, 604800000)
    val service = UsuarioService(usuarioJpa, libroRepository, reservaJpa, jwtTokenUtils, cacheRepository)

    it("permite editar el perfil con el tipo presentado por la API aunque tenga reservas activas") {
        val usuario = Usuario(
            email = "lector@example.com",
            password = "1234",
            nombre = "Lector",
            celular = "+54123456789",
            ciudad = "CABA",
            tipoUsuario = "LECTOR",
        ).apply { id = 1 }

        every { usuarioJpa.findById(1) } returns Optional.of(usuario)
        every { usuarioJpa.findByEmail("lector@example.com") } returns null
        every { usuarioJpa.findByCelular("+54123456789") } returns null
        every { usuarioJpa.save(usuario) } returns usuario
        every { reservaJpa.existsByUsuarioIdAndFechaHastaAfter(any(), any()) } returns true

        val resultado = service.actualizarUsuario(
            1,
            UsuarioUpdateRequest(
                nombre = "Lector",
                desc = "Descripción actualizada",
                email = "lector@example.com",
                celular = "+54123456789",
                ciudad = "CABA",
                tipoUsuario = "Lector",
            ),
        )

        resultado.usuario.desc shouldBe "Descripción actualizada"
        resultado.usuario.tipoUsuario shouldBe "Lector"
    }
})
