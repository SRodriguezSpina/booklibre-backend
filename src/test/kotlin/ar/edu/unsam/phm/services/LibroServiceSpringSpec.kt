package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.DuenioRef
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
import ar.edu.unsam.phm.repository.mongo.LibroMongoRepository
import ar.edu.unsam.phm.repository.ReservaJpaRepository
import ar.edu.unsam.phm.repository.UsuarioJpaRepository
import ar.edu.unsam.phm.repository.redis.TopLibrosCacheRepository
import ar.edu.unsam.phm.domain.TopLibrosCache
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime

class LibroServiceSpringSpec : DescribeSpec({

    val libroRepository = mockk<LibroMongoRepository>()
    val usuarioJpa = mockk<UsuarioJpaRepository>()
    val reservaJpa = mockk<ReservaJpaRepository>()
    val reservaService = mockk<ReservaService>()
    val topLibrosCacheRepository = mockk<TopLibrosCacheRepository>(relaxed = true)
    val librosClicks = mockk<ar.edu.unsam.phm.domain.LibrosClicks>(relaxed = true)
    val service = LibroService(
        libroRepository, usuarioJpa, reservaJpa, reservaService,
        topLibrosCacheRepository, librosClicks,
    )

    every { libroRepository.findAllByIds(any()) } returns emptyList()
    every { libroRepository.contarLibrosActivos() } returns 0L
    every { libroRepository.findTopActivosExcluyendo(any(), any()) } returns emptyList()
    every { topLibrosCacheRepository.save(any<TopLibrosCache>()) } answers { firstArg() }

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
        duenio = DuenioRef(id = duenio.id, nombre = duenio.nombre),
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
        fechaAgregado = LocalDateTime.now(),
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
        fechaAgregado = LocalDateTime.now(),
        cantidadCalificaciones = 0,
        ultimasCalificaciones = emptyList(),
    )

    fun libroComunPersistido() = LibroComun(libroDataBase()).apply { id = 10 }

    describe("LibroService (Spring) con repositorios mockeados") {

        it("crearLibro guarda LibroComun cuando tipo es Comun") {
            every { usuarioJpa.findById(3) } returns java.util.Optional.of(duenio)
            val cap = slot<Libro>()
            every { libroRepository.save(capture(cap)) } answers { cap.captured.apply { id = 99 } }

            val resultado = service.crearLibro(libroDto("Comun"), 3)

            resultado.id shouldBe 99
            resultado.shouldBeInstanceOf<LibroComun>()
            verify(exactly = 1) { libroRepository.save(any()) }
        }

        it("crearLibro guarda LibroConDedicatoria") {
            every { usuarioJpa.findById(3) } returns java.util.Optional.of(duenio)
            every { libroRepository.save(any()) } answers { (args[0] as Libro).apply { id = 1 } }

            service.crearLibro(libroDto("ConDedicatoria"), 3).shouldBeInstanceOf<LibroConDedicatoria>()
        }

        it("crearLibro guarda LibroColeccionable") {
            every { usuarioJpa.findById(3) } returns java.util.Optional.of(duenio)
            every { libroRepository.save(any()) } answers { (args[0] as Libro).apply { id = 2 } }

            service.crearLibro(libroDto("Coleccionable"), 3).shouldBeInstanceOf<LibroColeccionable>()
        }

        it("crearLibro lanza BusinessException si el tipo es inválido") {
            every { usuarioJpa.findById(3) } returns java.util.Optional.of(duenio)
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
            every { libroRepository.getByIdCompleto(10) } returns libro
            every { usuarioJpa.findById(2) } returns java.util.Optional.of(calificador)
            every { libroRepository.usuarioYaCalifico(10, 2) } returns false
            every { libroRepository.agregarCalificacion(any(), any()) } returns libro

            service.agregarCalificacion(10, NewCalificacionDTO(usuarioId = 2, valor = 5, comentario = "Bien"))

            verify(exactly = 1) { libroRepository.agregarCalificacion(any(), any()) }
        }

        it("actualizarLibro lanza UnauthorizedException si el usuario no es el dueño") {
            val libro = libroComunPersistido()
            every { libroRepository.getByIdCompleto(10) } returns libro
            shouldThrow<UnauthorizedException> {
                service.actualizarLibro(10, libroDto(), usuarioId = 999)
            }
        }

        it("search delega en reservaService y libroRepository") {
            every { reservaService.obtenerIdsLibrosReservados(null, null) } returns null
            val filtrosSlot = slot<LibroFiltros>()
            every { libroRepository.buscarPaginado(capture(filtrosSlot), 0, 6) } returns PagedResponse(emptyList(), 0, 0)

            service.search("", "", 0, 6, 1, null, null, null, null, null, null, "titulo")

            filtrosSlot.captured.excluirDuenioId shouldBe 1
            verify { libroRepository.buscarPaginado(any(), 0, 6) }
        }

        it("getRating delega en getById del libro") {
            val libro = libroComunPersistido().apply { calificacion = 4.0 }
            every { libroRepository.getById(10) } returns libro
            service.getRating(10) shouldBe 4.0
        }

        it("obtenerLibro propaga NotFoundException del repositorio") {
            every { libroRepository.getById(999) } throws NotFoundException("Libro.kt 999 no encontrado")
            shouldThrow<NotFoundException> { service.obtenerLibro(999) }
        }
    }
})
