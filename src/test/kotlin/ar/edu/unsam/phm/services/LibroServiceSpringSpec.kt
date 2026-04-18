package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Calificacion
import ar.edu.unsam.phm.domain.Disponibilidad
import ar.edu.unsam.phm.domain.EstadoLibro
import ar.edu.unsam.phm.domain.Genero
import ar.edu.unsam.phm.domain.Idioma
import ar.edu.unsam.phm.domain.LibroColeccionable
import ar.edu.unsam.phm.domain.LibroComun
import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.domain.LibroConDedicatoria
import ar.edu.unsam.phm.domain.LibroData
import ar.edu.unsam.phm.domain.LibroFiltros
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.NewCalificacionDTO
import ar.edu.unsam.phm.dtos.PagedResponse
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.exceptions.UnauthorizedException
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.ReservaRepository
import ar.edu.unsam.phm.repository.UsuarioRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate

class LibroServiceSpringSpec : DescribeSpec({

    val libroRepository = mockk<LibroRepository>()
    val usuarioRepository = mockk<UsuarioRepository>()
    val reservaRepository = mockk<ReservaRepository>()
    val service = LibroService(libroRepository, usuarioRepository, reservaRepository)

    val duenio = Usuario(
        email = "ana@example.com",
        password = "123",
        nombre = "Ana García",
        desc = "Publicadora",
        celular = "+5412345678",
        ciudad = "CABA",
        bibliokarmas = 1200,
    ).apply { id = 3 }

    fun libroDataBase() = LibroData(
        duenio = duenio,
        titulo = "Clean Code",
        autor = "Robert C. Martin",
        descripcion = "Buenas prácticas",
        genero = Genero.DISENO,
        cantidadPaginas = 464,
        isbn = "9780132350884",
        idioma = Idioma.INGLES,
        editorial = "Prentice Hall",
        fechaPublicacion = LocalDate.of(2008, 8, 1),
        estado = EstadoLibro.EXCELENTE,
        imagenUrl = "https://example.com/cover.jpg",
        fechaAgregado = LocalDate.now(),
        disponibilidad = Disponibilidad.DISPONIBLE,
    )

    fun libroDto(tipo: String = "Comun") = LibroDTO(
        id = 0,
        titulo = "Clean Code",
        tipo = tipo,
        descripcion = "Buenas prácticas",
        genero = Genero.DISENO,
        autor = "Robert C. Martin",
        paginas = 464,
        isbn = "9780132350884",
        idioma = Idioma.INGLES,
        editorial = "Prentice Hall",
        estado = "EXCELENTE",
        fechaPublicacion = LocalDate.of(2008, 8, 1),
        propietarioId = duenio.id,
        imagenUrl = "https://example.com/cover.jpg",
        alquiladoPorId = null,
        bibliokarma = null,
        estoyReservado = false,
        rating = 0.0,
        cantidadReservas = 0,
        reservas = emptyList(),
        fechaAgregado = LocalDate.now(),
        cantidadCalificaciones = 0,
        ultimasCalificaciones = emptyList(),
    )

    fun libroComunPersistido() = LibroComun(libroDataBase()).apply { id = 10 }

    describe("LibroService (Spring) con repositorios mockeados") {

        it("crearLibro guarda LibroComun cuando tipo es Comun") {
            every { usuarioRepository.getById(3) } returns duenio
            val cap = slot<Libro>()
            every { libroRepository.save(capture(cap)) } answers { cap.captured.apply { id = 99 } }

            val resultado = service.crearLibro(libroDto("Comun"), 3)

            resultado.id shouldBe 99
            resultado.shouldBeInstanceOf<LibroComun>()
            verify(exactly = 1) { libroRepository.save(any()) }
        }

        it("crearLibro guarda LibroConDedicatoria") {
            every { usuarioRepository.getById(3) } returns duenio
            every { libroRepository.save(any()) } answers { firstArg<Libro>().apply { id = 1 } }

            service.crearLibro(libroDto("ConDedicatoria"), 3).shouldBeInstanceOf<LibroConDedicatoria>()
        }

        it("crearLibro guarda LibroColeccionable") {
            every { usuarioRepository.getById(3) } returns duenio
            every { libroRepository.save(any()) } answers { firstArg<Libro>().apply { id = 2 } }

            service.crearLibro(libroDto("Coleccionable"), 3).shouldBeInstanceOf<LibroColeccionable>()
        }

        it("crearLibro lanza BusinessException si el tipo es inválido") {
            every { usuarioRepository.getById(3) } returns duenio
            shouldThrow<BusinessException> {
                service.crearLibro(libroDto("LibroComun"), 3)
            }
        }

        it("agregarCalificacion carga libro, valida usuario y persiste") {
            val libro = libroComunPersistido()
            val calificador = Usuario(
                email = "juan@example.com",
                password = "123",
                nombre = "Juan",
                desc = "",
                celular = "+54987654321",
                ciudad = "Lanús",
                bibliokarmas = 50,
            ).apply { id = 2 }
            every { libroRepository.getById(10) } returns libro
            every { usuarioRepository.getById(2) } returns calificador
            every { libroRepository.save(libro) } returns libro

            service.agregarCalificacion(10, NewCalificacionDTO(usuarioId = 2, valor = 5, comentario = "Bien"))

            libro.calificaciones.size shouldBe 1
            verify(exactly = 1) { libroRepository.save(libro) }
        }

        it("actualizarLibro lanza UnauthorizedException si el usuario no es el dueño") {
            val libro = libroComunPersistido()
            every { libroRepository.getById(10) } returns libro
            shouldThrow<UnauthorizedException> {
                service.actualizarLibro(10, libroDto(), usuarioId = 999)
            }
        }

        it("search delega en reservaRepository y libroRepository") {
            every { reservaRepository.obtenerIdsLibrosReservados(null, null) } returns null
            val filtrosSlot = slot<LibroFiltros>()
            every { libroRepository.buscarPaginado(capture(filtrosSlot), 0, 6) } returns PagedResponse(emptyList(), 0, 0)

            service.search("", "", 0, 6, 1, null, null, null, null, null, null, "titulo")

            filtrosSlot.captured.excluirDuenioId shouldBe 1
            verify { libroRepository.buscarPaginado(any(), 0, 6) }
        }

        it("getRating delega en getById del libro") {
            val libro = libroComunPersistido()
            libro.agregarCalificacion(Calificacion(libro = libro, usuarioId = 2, valor = 4, comentario = "ok"))
            every { libroRepository.getById(10) } returns libro
            service.getRating(10) shouldBe 4.0
        }

        it("obtenerLibro propaga NotFoundException del repositorio") {
            every { libroRepository.getById(999) } throws NotFoundException("Libro.kt 999 no encontrado")
            shouldThrow<NotFoundException> { service.obtenerLibro(999) }
        }
    }
})
