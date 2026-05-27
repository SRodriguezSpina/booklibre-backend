package ar.edu.unsam.phm.security

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank

private const val TEST_SECRET = "booklibre-test-secret-clave-de-prueba-larga-suficiente"
private const val ONE_HOUR_MS = 3_600_000L

class JwtTokenUtilsSpec : DescribeSpec({

    val tokenUtils = JwtTokenUtils(secret = TEST_SECRET, expiration = ONE_HOUR_MS, refreshExpiration = 604_800_000L)

    describe("generateToken") {
        it("genera un token no vacío") {
            val token = tokenUtils.generateToken("user@test.com", listOf("ROLE_LECTOR"))
            token.shouldNotBeBlank()
        }

        it("el token tiene formato JWT de tres partes separadas por punto") {
            val token = tokenUtils.generateToken("user@test.com", listOf("ROLE_LECTOR"))
            token.split(".").size shouldBe 3
        }
    }

    describe("validateToken") {
        it("token recién generado es válido") {
            val token = tokenUtils.generateToken("user@test.com", listOf("ROLE_LECTOR"))
            tokenUtils.validateToken(token) shouldBe true
        }

        it("string arbitrario no es un token válido") {
            tokenUtils.validateToken("esto_no_es_un_jwt") shouldBe false
        }

        it("token con firma adulterada no es válido") {
            val token = tokenUtils.generateToken("user@test.com", listOf("ROLE_LECTOR"))
            val partes = token.split(".")
            val tokenAdulterado = "${partes[0]}.${partes[1]}.firma_falsa"
            tokenUtils.validateToken(tokenAdulterado) shouldBe false
        }

        it("token generado con otro secret no es válido") {
            val otroUtils = JwtTokenUtils(secret = "otro-secret-completamente-diferente-y-largo", expiration = ONE_HOUR_MS, refreshExpiration = 604_800_000L)
            val token = otroUtils.generateToken("user@test.com", listOf("ROLE_LECTOR"))
            tokenUtils.validateToken(token) shouldBe false
        }

        it("token expirado no es válido") {
            val expiredUtils = JwtTokenUtils(secret = TEST_SECRET, expiration = -1000L, refreshExpiration = -1000L)
            val token = expiredUtils.generateToken("user@test.com", listOf("ROLE_LECTOR"))
            expiredUtils.validateToken(token) shouldBe false
        }
    }

    describe("extractEmail") {
        it("extrae el email del subject del token") {
            val token = tokenUtils.generateToken("ana@mail.com", listOf("ROLE_LECTOR"))
            tokenUtils.extractEmail(token) shouldBe "ana@mail.com"
        }
    }

    describe("extractRoles") {
        it("extrae una lista con un solo rol") {
            val token = tokenUtils.generateToken("user@test.com", listOf("ROLE_LECTOR"))
            tokenUtils.extractRoles(token) shouldContainExactly listOf("ROLE_LECTOR")
        }

        it("extrae múltiples roles para LECTOR_PUBLICADOR") {
            val roles = listOf("ROLE_LECTOR", "ROLE_PUBLICADOR")
            val token = tokenUtils.generateToken("editor@test.com", roles)
            tokenUtils.extractRoles(token) shouldContainExactly roles
        }
    }

    describe("consistencia email + roles") {
        it("el mismo token devuelve email y roles coherentes") {
            val email = "carlos@test.com"
            val roles = listOf("ROLE_PUBLICADOR")
            val token = tokenUtils.generateToken(email, roles)
            tokenUtils.extractEmail(token) shouldBe email
            tokenUtils.extractRoles(token) shouldContainExactly roles
        }
    }
})
