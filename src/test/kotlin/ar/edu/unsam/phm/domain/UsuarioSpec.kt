package ar.edu.unsam.phm.domain

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import java.time.LocalDate
import java.time.LocalDateTime

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

        val duenio = Usuario(
            credencial = Credencial("ana", "pass"),
            nombre = "Ana García",
            desc = "Publicadora"
        )

        // CORRECCIÓN: Usamos LibroData y cambiamos 'paginas' por 'cantidadPaginas'
        val dataLibro = LibroData(
            duenio = duenio,
            titulo = "Clean Code", autor = "Robert C. Martin",
            descripcion = "Buenas prácticas", genero = Genero.DISENO,
            cantidadPaginas = 464, isbn = "ISBN-001", idioma = Idioma.INGLES,
            editorial = "Prentice Hall", fechaPublicacion = LocalDate.of(2008, 8, 1),
            estado = EstadoLibro.EXCELENTE, imagenUrl = ""
        )

        // CORRECCIÓN: Asignamos el ID heredado de BaseEntity con apply
        val libroComun = LibroComun(dataLibro).apply { id = 1 }

        val reserva = Reserva(
            libro = libroComun,
            usuario = usuario,
            fechaDesde = LocalDate.of(2026, 3, 1),
            fechaHasta = LocalDate.of(2026, 3, 10)
        ).apply { id = 0 }

        it("VALIDA PASSWORD CORRECTA") {
            usuario.validarPassword("1234") shouldBe true
        }

        it("VALIDA PASSWORD INCORRECTA") {
            usuario.validarPassword("wrongpass") shouldBe false
        }

        it("AGREGA UNA RESERVA Y LA LISTA LA CONTIENE") {
            usuario.agregarReserva(reserva)
            usuario.todasLasReservas() shouldContain reserva
        }

        it("TODAS LAS RESERVAS DEVUELVE LA LISTA COMPLETA") {
            val reserva2 = Reserva(libroComun, usuario, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 10)).apply { id = 1 }
            usuario.agregarReserva(reserva)
            usuario.agregarReserva(reserva2)
            usuario.todasLasReservas().size shouldBe 2
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