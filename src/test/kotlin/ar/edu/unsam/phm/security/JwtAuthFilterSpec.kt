package ar.edu.unsam.phm.security

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthFilterSpec : DescribeSpec({

    beforeEach { SecurityContextHolder.clearContext() }

    val jwtTokenUtils = mockk<JwtTokenUtils>()
    val filter = JwtAuthFilter(jwtTokenUtils)

    fun request(authHeader: String? = null): MockHttpServletRequest {
        val req = MockHttpServletRequest()
        authHeader?.let { req.addHeader("Authorization", it) }
        return req
    }

    describe("sin header Authorization") {
        it("pasa la request al siguiente filtro sin modificar nada") {
            val req = request()
            val res = MockHttpServletResponse()
            val chain = mockk<FilterChain>(relaxed = true)

            filter.doFilter(req, res, chain)

            verify { chain.doFilter(req, res) }
            res.status shouldBe 200
        }
    }

    describe("header Authorization sin prefijo Bearer") {
        it("pasa de largo cuando el esquema es Basic") {
            val req = request("Basic dXNlcjpwYXNz")
            val res = MockHttpServletResponse()
            val chain = mockk<FilterChain>(relaxed = true)

            filter.doFilter(req, res, chain)

            verify { chain.doFilter(req, res) }
        }
    }

    describe("token inválido o expirado") {
        it("continúa la cadena de filtros") {
            val req = request("Bearer token_invalido")
            val res = MockHttpServletResponse()
            val chain = mockk<FilterChain>(relaxed = true)
            every { jwtTokenUtils.validateToken("token_invalido") } returns false

            filter.doFilter(req, res, chain)

            verify { chain.doFilter(req, res) }
        }

        it("limpia el SecurityContext") {
            val req = request("Bearer roto")
            val res = MockHttpServletResponse()
            val chain = mockk<FilterChain>(relaxed = true)
            every { jwtTokenUtils.validateToken("roto") } returns false

            filter.doFilter(req, res, chain)

            SecurityContextHolder.getContext().authentication shouldBe null
        }
    }

    describe("token válido") {
        it("carga la Authentication en el SecurityContext y continúa") {
            val req = request("Bearer token_valido")
            val res = MockHttpServletResponse()
            val chain = mockk<FilterChain>(relaxed = true)
            every { jwtTokenUtils.validateToken("token_valido") } returns true
            every { jwtTokenUtils.extractEmail("token_valido") } returns "user@test.com"
            every { jwtTokenUtils.extractRoles("token_valido") } returns listOf("ROLE_LECTOR")

            filter.doFilter(req, res, chain)

            verify { chain.doFilter(req, res) }
            SecurityContextHolder.getContext().authentication shouldNotBe null
        }

        it("el principal del contexto es el email extraído del token") {
            val req = request("Bearer token_valido")
            val res = MockHttpServletResponse()
            val chain = mockk<FilterChain>(relaxed = true)
            every { jwtTokenUtils.validateToken("token_valido") } returns true
            every { jwtTokenUtils.extractEmail("token_valido") } returns "ana@test.com"
            every { jwtTokenUtils.extractRoles("token_valido") } returns listOf("ROLE_LECTOR")

            filter.doFilter(req, res, chain)

            SecurityContextHolder.getContext().authentication?.principal shouldBe "ana@test.com"
        }

        it("no reemplaza una Authentication ya existente en el contexto") {
            val req = request("Bearer token_valido")
            val res = MockHttpServletResponse()
            val chain = mockk<FilterChain>(relaxed = true)
            every { jwtTokenUtils.validateToken("token_valido") } returns true
            every { jwtTokenUtils.extractEmail("token_valido") } returns "user@test.com"
            every { jwtTokenUtils.extractRoles("token_valido") } returns listOf("ROLE_LECTOR")

            filter.doFilter(req, res, chain)
            val primeraAuth = SecurityContextHolder.getContext().authentication

            filter.doFilter(req, res, chain)
            val segundaAuth = SecurityContextHolder.getContext().authentication

            segundaAuth shouldBe primeraAuth
        }
    }
})
