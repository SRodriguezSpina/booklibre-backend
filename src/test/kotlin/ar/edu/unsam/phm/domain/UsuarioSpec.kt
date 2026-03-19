package ar.edu.unsam.phm.domain

import ar.edu.unsam.phm.Credencial
import ar.edu.unsam.phm.Usuario
import ar.edu.unsam.phm.UsuarioLector
import ar.edu.unsam.phm.UsuarioPublicador
import ar.edu.unsam.phm.UsuarioLectorPublicador
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class UsuarioSpec : DescribeSpec({
    fun credencial(email: String = "test@mail.com", password: String = "secret") =
        Credencial(email, password)

    fun usuario(
        email: String = "test@mail.com",
        password: String = "secret",
        tipo: ar.edu.unsam.phm.TipoUsuario = UsuarioLector()
    ) = Usuario(credencial(email, password), "Test", "", tipoUsuario = tipo)

    describe("validarPassword") {
        it("retorna true si la password coincide") {
            val u = usuario(password = "abc123")
            u.validarPassword("abc123") shouldBe true
        }

        it("retorna false si la password no coincide") {
            val u = usuario(password = "abc123")
            u.validarPassword("wrongpassword") shouldBe false
        }

        it("retorna false si la password está vacía") {
            val u = usuario(password = "abc123")
            u.validarPassword("") shouldBe false
        }

        it("es case-sensitive") {
            val u = usuario(password = "Secret")
            u.validarPassword("secret") shouldBe false
            u.validarPassword("Secret") shouldBe true
        }
    }

    describe("getCredencial") {
        it("expone el email de la credencial") {
            val u = usuario(email = "juan@mail.com")
            u.getCredencial().email shouldBe "juan@mail.com"
        }

        it("expone la password de la credencial") {
            val u = usuario(password = "mypassword")
            u.getCredencial().password shouldBe "mypassword"
        }
    }

    describe("tipoUsuario") {
        it("por defecto es UsuarioLector") {
            val u = Usuario(credencial(), "Test", "")
            u.tipoUsuario.shouldBeInstanceOf<UsuarioLector>()
        }

        it("acepta UsuarioPublicador") {
            val u = usuario(tipo = UsuarioPublicador())
            u.tipoUsuario.shouldBeInstanceOf<UsuarioPublicador>()
        }

        it("acepta UsuarioLectorPublicador") {
            val u = usuario(tipo = UsuarioLectorPublicador())
            u.tipoUsuario.shouldBeInstanceOf<UsuarioLectorPublicador>()        }
    }

    describe("reservas") {
        it("inicia con lista vacía") {
            val u = usuario()
            u.todasLasReservas() shouldBe emptyList()
        }
    }
})