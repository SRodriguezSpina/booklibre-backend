package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Disponibilidad
import ar.edu.unsam.phm.domain.EstadoLibro
import ar.edu.unsam.phm.domain.Genero
import ar.edu.unsam.phm.domain.Idioma
import ar.edu.unsam.phm.domain.LibroComun
import ar.edu.unsam.phm.domain.LibroData
import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.domain.TipoReserva
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.dtos.CrearReservaDTO
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.persistence.entities.ReservaEntity
import ar.edu.unsam.phm.repository.LibroJpaRepository
import ar.edu.unsam.phm.repository.ReservaJpaRepository
import ar.edu.unsam.phm.repository.UsuarioJpaRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.Optional

class ReservaServiceSpringSpec : DescribeSpec({

    val libroJpaRepository = mockk<LibroJpaRepository>()
    val usuarioJpaRepository = mockk<UsuarioJpaRepository>()
    val reservaJpaRepository = mockk<ReservaJpaRepository>()
    val service = ReservaService(libroJpaRepository, usuarioJpaRepository, reservaJpaRepository)

    val duenio = Usuario(
        email = "ana@example.com",
        password = "123",
        nombre = "Ana García",
        desc = "Publicadora",
        celular = "+5412345678",
        ciudad = "CABA",
        bibliokarmas = 1200,
    ).apply { id = 3 }

    fun libroDisponible(): LibroComun {
        val data = LibroData(
            duenio = duenio,
            titulo = "1984",
            autor = "George Orwell",
            descripcion = "Distopía",
            genero = Genero.LITERATURA_CLASICA,
            cantidadPaginas = 300,
            isbn = "9780000000001",
            idioma = Idioma.ESPANOL,
            editorial = "Debolsillo",
            fechaPublicacion = LocalDate.of(1949, 6, 8),
            estado = EstadoLibro.EXCELENTE,
            imagenUrl = "",
            fechaAgregado = LocalDate.now(),
            disponibilidad = Disponibilidad.DISPONIBLE,
        )
        return LibroComun(data).apply { id = 10 }
    }

    describe("ReservaService (Spring) con JPA mockeado") {

        it("crearReserva lanza ConflictException si hay superposición") {
            val dto = CrearReservaDTO(10, 1, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10))
            every { reservaJpaRepository.existsSuperposicion(10, dto.fechaDesde, dto.fechaHasta) } returns true

            shouldThrow<ConflictException> { service.crearReserva(dto) }

            verify(exactly = 0) { libroJpaRepository.save(any()) }
            verify(exactly = 0) { reservaJpaRepository.save(any()) }
        }

        it("crearReserva lanza NotFoundException si el libro no existe") {
            val dto = CrearReservaDTO(999, 1, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10))
            every { reservaJpaRepository.existsSuperposicion(999, dto.fechaDesde, dto.fechaHasta) } returns false
            every { libroJpaRepository.findById(999) } returns Optional.empty()

            shouldThrow<NotFoundException> { service.crearReserva(dto) }
        }

        it("crearReserva lanza NotFoundException si el usuario no existe") {
            val dto = CrearReservaDTO(10, 999, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10))
            val libro = libroDisponible()
            every { reservaJpaRepository.existsSuperposicion(10, dto.fechaDesde, dto.fechaHasta) } returns false
            every { libroJpaRepository.findById(10) } answers { Optional.of(libro) }

            every { usuarioJpaRepository.findById(999) } returns Optional.empty()

            shouldThrow<NotFoundException> { service.crearReserva(dto) }
        }

        it("crearReserva persiste libro, usuario y reserva cuando todo es válido") {
            val libro = libroDisponible()
            val user = Usuario(
                email = "juan@example.com",
                password = "123",
                nombre = "Juan Pérez",
                desc = "Lector",
                celular = "+54987654321",
                ciudad = "San Martín",
                bibliokarmas = 500,
            ).apply { id = 1 }

            val dto = CrearReservaDTO(10, 1, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10))
            every { reservaJpaRepository.existsSuperposicion(10, dto.fechaDesde, dto.fechaHasta) } returns false
            every { libroJpaRepository.findById(10) } answers { Optional.of(libro) }
            every { usuarioJpaRepository.findById(1) } answers { Optional.of(user) }
            every { libroJpaRepository.save(any()) } answers { firstArg() }
            every { usuarioJpaRepository.save(any()) } answers { firstArg() }

            every { reservaJpaRepository.save(any()) } answers {
                firstArg<ReservaEntity>().apply { id = 500 }
            }

            val reserva = service.crearReserva(dto)

            reserva.libro.id shouldBe 10
            reserva.usuario.id shouldBe 1
            libro.estaDisponible().shouldBeFalse()
            verify { libroJpaRepository.save(libro) }
            verify { usuarioJpaRepository.save(user) }
            verify { reservaJpaRepository.save(any()) }
        }

        it("obtenerReservas lanza NotFoundException si el usuario no existe") {
            every { usuarioJpaRepository.findById(99) } returns Optional.empty()
            shouldThrow<NotFoundException> { service.obtenerReservas(99, TipoReserva.HECHAS) }
        }
    }
})
