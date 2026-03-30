package ar.edu.unsam.phm.domain

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class UsuarioSpec : DescribeSpec({
    isolationMode = IsolationMode.InstancePerTest

    describe("Compruebo Usuario") {

        val credencial = Credencial("juanito", "1234")
        val usuario = Usuario(
            credencial = credencial,
            nombre = "Juan Pérez",
            desc = "Lector intrépido",
            celular = "11556677",
            ciudad = "San Martín",
            tipoUsuario = UsuarioLector(),
            bibliokarmas = 500,
            fechaRegistro = LocalDate.of(2020, 9, 15)
        )

        it("VALIDA PASSWORD CORRECTA") {
            usuario.validarPassword("1234") shouldBe true
        }

        it("VALIDA PASSWORD INCORRECTA") {
            usuario.validarPassword("wrongpass") shouldBe false
        }

        it("GET CREDENCIAL DEVUELVE LA CREDENCIAL CORRECTA") {
            usuario.getCredencial() shouldBe credencial
        }

        it("TIPO USUARIO POR DEFECTO ES LECTOR") {
            usuario.tipoUsuario::class shouldBe UsuarioLector::class
        }

        it("SE PUEDE CAMBIAR EL TIPO DE USUARIO A PUBLICADOR") {
            usuario.tipoUsuario = UsuarioPublicador()
            usuario.tipoUsuario::class shouldBe UsuarioPublicador::class
        }

        it("SE PUEDE CAMBIAR EL TIPO DE USUARIO A LECTOR PUBLICADOR") {
            usuario.tipoUsuario = UsuarioLectorPublicador()
            usuario.tipoUsuario::class shouldBe UsuarioLectorPublicador::class
        }
    }
})