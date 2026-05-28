package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.dtos.RegisterRequest
import ar.edu.unsam.phm.repository.UsuarioJpaRepository
import ar.edu.unsam.phm.security.JwtTokenUtils
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.springframework.security.crypto.password.PasswordEncoder

class AuthServiceHashSpec : DescribeSpec({

    val passwordEncoder = mockk<PasswordEncoder>()
    val usuarioRepository = mockk<UsuarioJpaRepository>()
    val jwtTokenUtils = mockk<JwtTokenUtils>()

    val service = AuthService(usuarioRepository, passwordEncoder, jwtTokenUtils)

    describe("register") {

        it("codifica la password con el encoder antes de persistir") {
            val rawPassword = "mipassword123"
            val hashedPassword = "\$2a\$10\$hashedvalue.placeholder"

            every { passwordEncoder.encode(rawPassword) } returns hashedPassword
            every { usuarioRepository.findByEmail(any()) } returns null
            every { usuarioRepository.save(any()) } answers { firstArg() }
            every { jwtTokenUtils.generateToken(any(), any()) } returns "test_token"
            every { jwtTokenUtils.generateRefreshToken(any()) } returns "refresh_token"

            service.register(RegisterRequest("Test", "test@test.com", rawPassword))

            verify { passwordEncoder.encode(rawPassword) }
        }

        it("no guarda la password en texto plano") {
            val rawPassword = "secreto"
            val hashedPassword = "\$2a\$10\$hash_de_bcrypt"
            val usuarioCapturado = slot<ar.edu.unsam.phm.domain.Usuario>()

            every { passwordEncoder.encode(rawPassword) } returns hashedPassword
            every { usuarioRepository.findByEmail(any()) } returns null
            every { usuarioRepository.save(capture(usuarioCapturado)) } answers { firstArg() }
            every { jwtTokenUtils.generateToken(any(), any()) } returns "token"
            every { jwtTokenUtils.generateRefreshToken(any()) } returns "refresh_token"

            service.register(RegisterRequest("Usuario", "u@test.com", rawPassword))

            usuarioCapturado.captured.password shouldBe hashedPassword
            usuarioCapturado.captured.password shouldNotBe rawPassword
        }

        it("el token devuelto no es nulo") {
            every { passwordEncoder.encode(any()) } returns "\$2a\$10\$hash"
            every { usuarioRepository.findByEmail(any()) } returns null
            every { usuarioRepository.save(any()) } answers { firstArg() }
            every { jwtTokenUtils.generateToken(any(), any()) } returns "mi_token_jwt"
            every { jwtTokenUtils.generateRefreshToken(any()) } returns "refresh_token"

            val (_, tokens) = service.register(RegisterRequest("Ana", "ana@test.com", "pass"))
            tokens.accessToken shouldNotBe null
        }
    }
})
