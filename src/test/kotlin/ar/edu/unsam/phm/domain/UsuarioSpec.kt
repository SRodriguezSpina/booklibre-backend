package ar.edu.unsam.phm.domain

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class UsuarioSpec : DescribeSpec({
    isolationMode = IsolationMode.InstancePerTest

    describe("Compruebo Usuario") {


        val usuario = Usuario(
            "juanito", "1234",
            nombre = "Juan Pérez",
            desc = "Lector intrépido",
            celular = "11556677",
            ciudad = "San Martín",
            tipoUsuario = "LECTOR",
            bibliokarmas = 500,
            fechaRegistro = LocalDate.of(2020, 9, 15)
        )

        it("VALIDA PASSWORD CORRECTA") {
            usuario.validarPassword("1234") shouldBe true
        }

        it("VALIDA PASSWORD INCORRECTA") {
            usuario.validarPassword("wrongpass") shouldBe false
        }

        it("TIPO USUARIO POR DEFECTO ES LECTOR") {
            usuario.tipoUsuario shouldBe "LECTOR"
        }
    }
})