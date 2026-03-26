package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.*
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.UsuarioRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDate

class LibroServiceSpec : DescribeSpec({
    isolationMode = IsolationMode.InstancePerTest
    val duenio = Usuario(
        credencial = Credencial("ana@mail.com", "pass"),
        nombre = "Ana García",
        desc = "Publicadora",
        bibliokarmas = 1200,
    )

    val otroUsuario = Usuario(
        credencial = Credencial("carlos@mail.com", "pass"),
        nombre = "Carlos",
        desc = "",
        bibliokarmas = 50,
    )

    fun buildRepos(): Pair<LibroRepository, UsuarioRepository> {
        val libroRepo = LibroRepository()
        val usuarioRepo = UsuarioRepository()
        usuarioRepo.create(duenio)
        usuarioRepo.create(otroUsuario)
        return libroRepo to usuarioRepo
    }

    fun buildService(libroRepo: LibroRepository, usuarioRepo: UsuarioRepository) =
        LibroService(libroRepo, usuarioRepo)

    fun libroDTOBase(
        tipo: String = "Comun",
        titulo: String = "Clean Code",
        autor: String = "Robert C. Martin",
        paginas: Int = 464,
        estado: String = "EXCELENTE",
    ) = LibroDTO(
        id = 0,
        titulo = titulo,
        tipo = tipo,
        descripcion = "Buenas prácticas de programación",
        genero = Genero.DISENO,
        autor = autor,
        paginas = paginas,
        isbn = "978-0132350884",
        idioma = Idioma.INGLES,
        editorial = "Prentice Hall",
        estado = estado,
        fechaPublicacion = LocalDate.of(2008, 8, 1),
        propietarioId = duenio.id,
        imagenUrl = "https://example.com/cover.jpg",
        alquiladoPorId = null,
        bibliokarma = null,
        estoyReservado = false,
        rating = 0.0,
        reservas = emptyList(),
        cantidadReservas = 0
        ultimasCalificaciones = emptyList()
    )

    describe("crearLibro") {

        describe("tipos de libro") {

            it("crea un LibroComun y lo persiste en el repositorio") {
                val (libroRepo, usuarioRepo) = buildRepos()
                val service = buildService(libroRepo, usuarioRepo)

                val resultado = service.crearLibro(libroDTOBase(tipo = "Comun"), duenio.id)

                resultado.titulo shouldBe "Clean Code"
                resultado.tipo shouldBe "Comun"
                libroRepo.getLibros().size shouldBe 1
            }

            it("crea un LibroConDedicatoria y lo persiste en el repositorio") {
                val (libroRepo, usuarioRepo) = buildRepos()
                val service = buildService(libroRepo, usuarioRepo)

                val resultado = service.crearLibro(libroDTOBase(tipo = "ConDedicatoria"), duenio.id)

                resultado.tipo shouldBe "ConDedicatoria"
                libroRepo.getLibros().size shouldBe 1
            }

            it("crea un LibroColeccionable y lo persiste en el repositorio") {
                val (libroRepo, usuarioRepo) = buildRepos()
                val service = buildService(libroRepo, usuarioRepo)

                val resultado = service.crearLibro(libroDTOBase(tipo = "Coleccionable"), duenio.id)

                resultado.tipo shouldBe "Coleccionable"
                libroRepo.getLibros().size shouldBe 1
            }

            it("lanza BusinessException si el tipo de libro es inválido") {
                val (libroRepo, usuarioRepo) = buildRepos()
                val service = buildService(libroRepo, usuarioRepo)

                val ex = shouldThrow<BusinessException> {
                    service.crearLibro(libroDTOBase(tipo = "TipoInexistente"), duenio.id)
                }
                ex.message shouldContain "TipoInexistente"
            }

            it("tipo vacío lanza BusinessException") {
                val (libroRepo, usuarioRepo) = buildRepos()
                val service = buildService(libroRepo, usuarioRepo)

                shouldThrow<BusinessException> {
                    service.crearLibro(libroDTOBase(tipo = ""), duenio.id)
                }
            }
        }

        describe("propietario") {

            it("asocia el libro al usuario propietario correcto") {
                val (libroRepo, usuarioRepo) = buildRepos()
                val service = buildService(libroRepo, usuarioRepo)

                service.crearLibro(libroDTOBase(), duenio.id)

                val libro = libroRepo.getLibros().first()
                libro.duenio.id shouldBe duenio.id
            }

            it("lanza NotFoundException si el usuario propietario no existe") {
                val (libroRepo, usuarioRepo) = buildRepos()
                val service = buildService(libroRepo, usuarioRepo)

                shouldThrow<NotFoundException> {
                    service.crearLibro(libroDTOBase(), usuarioId = 9999)
                }
            }

            it("el repositorio no crece si el propietario no existe") {
                val (libroRepo, usuarioRepo) = buildRepos()
                val service = buildService(libroRepo, usuarioRepo)

                shouldThrow<NotFoundException> {
                    service.crearLibro(libroDTOBase(), usuarioId = 9999)
                }
                libroRepo.getLibros().size shouldBe 0
            }
        }

        describe("datos del DTO") {

            it("persiste el título correctamente") {
                val (libroRepo, usuarioRepo) = buildRepos()
                val service = buildService(libroRepo, usuarioRepo)

                service.crearLibro(libroDTOBase(titulo = "Refactoring"), duenio.id)

                libroRepo.getLibros().first().titulo shouldBe "Refactoring"
            }

            it("persiste el autor correctamente") {
                val (libroRepo, usuarioRepo) = buildRepos()
                val service = buildService(libroRepo, usuarioRepo)

                service.crearLibro(libroDTOBase(autor = "Martin Fowler"), duenio.id)

                libroRepo.getLibros().first().autor shouldBe "Martin Fowler"
            }

            it("persiste la cantidad de páginas correctamente") {
                val (libroRepo, usuarioRepo) = buildRepos()
                val service = buildService(libroRepo, usuarioRepo)

                service.crearLibro(libroDTOBase(paginas = 448), duenio.id)

                libroRepo.getLibros().first().cantidadPaginas shouldBe 448
            }

            it("persiste el estado correctamente") {
                val (libroRepo, usuarioRepo) = buildRepos()
                val service = buildService(libroRepo, usuarioRepo)

                service.crearLibro(libroDTOBase(estado = "MUY_BUENO"), duenio.id)

                libroRepo.getLibros().first().estado shouldBe EstadoLibro.MUY_BUENO
            }

            it("el libro creado recibe un id asignado por el repositorio (no 0)") {
                val (libroRepo, usuarioRepo) = buildRepos()
                val service = buildService(libroRepo, usuarioRepo)

                val resultado = service.crearLibro(libroDTOBase(), duenio.id)

                resultado.id shouldNotBe 0
            }

            it("crear dos libros les asigna ids distintos") {
                val (libroRepo, usuarioRepo) = buildRepos()
                val service = buildService(libroRepo, usuarioRepo)

                val a = service.crearLibro(libroDTOBase(titulo = "Libro A"), duenio.id)
                val b = service.crearLibro(libroDTOBase(titulo = "Libro B"), duenio.id)

                a.id shouldNotBe b.id
            }

            it("dos usuarios distintos pueden publicar libros con el mismo título") {
                val (libroRepo, usuarioRepo) = buildRepos()
                val service = buildService(libroRepo, usuarioRepo)

                service.crearLibro(libroDTOBase(titulo = "Clean Code"), duenio.id)
                service.crearLibro(libroDTOBase(titulo = "Clean Code"), otroUsuario.id)

                libroRepo.getLibros().size shouldBe 2
            }
        }

        describe("parsing de EstadoLibro") {

            listOf("EXCELENTE", "MUY_BUENO", "BUENO", "REGULAR", "MALO").forEach { estado ->
                it("acepta el estado '$estado'") {
                    val (libroRepo, usuarioRepo) = buildRepos()
                    val service = buildService(libroRepo, usuarioRepo)

                    val resultado = service.crearLibro(libroDTOBase(estado = estado), duenio.id)

                    libroRepo.getById(resultado.id).estado.name shouldBe estado
                }
            }

            it("lanza excepción con un valor de estado inválido") {
                val (libroRepo, usuarioRepo) = buildRepos()
                val service = buildService(libroRepo, usuarioRepo)

                shouldThrow<Exception> {
                    service.crearLibro(libroDTOBase(estado = "PERFECTO"), duenio.id)
                }
            }
        }
    }

    describe("actualizarLibro") {

        fun crearLibroEnRepo(libroRepo: LibroRepository, usuarioRepo: UsuarioRepository): Int {
            val service = buildService(libroRepo, usuarioRepo)
            val creado = service.crearLibro(libroDTOBase(), duenio.id)
            return creado.id
        }

        it("actualiza el título del libro existente") {
            val (libroRepo, usuarioRepo) = buildRepos()
            val id = crearLibroEnRepo(libroRepo, usuarioRepo)
            val service = buildService(libroRepo, usuarioRepo)

            service.actualizarLibro(id, libroDTOBase(titulo = "Clean Code 2nd Edition"))

            libroRepo.getById(id).titulo shouldBe "Clean Code 2nd Edition"
        }

        it("actualiza el autor del libro existente") {
            val (libroRepo, usuarioRepo) = buildRepos()
            val id = crearLibroEnRepo(libroRepo, usuarioRepo)
            val service = buildService(libroRepo, usuarioRepo)

            service.actualizarLibro(id, libroDTOBase(autor = "Uncle Bob"))

            libroRepo.getById(id).autor shouldBe "Uncle Bob"
        }

        it("actualiza la cantidad de páginas del libro existente") {
            val (libroRepo, usuarioRepo) = buildRepos()
            val id = crearLibroEnRepo(libroRepo, usuarioRepo)
            val service = buildService(libroRepo, usuarioRepo)

            service.actualizarLibro(id, libroDTOBase(paginas = 999))

            libroRepo.getById(id).cantidadPaginas shouldBe 999
        }

        it("actualiza el estado del libro existente") {
            val (libroRepo, usuarioRepo) = buildRepos()
            val id = crearLibroEnRepo(libroRepo, usuarioRepo)
            val service = buildService(libroRepo, usuarioRepo)

            service.actualizarLibro(id, libroDTOBase(estado = "REGULAR"))

            libroRepo.getById(id).estado shouldBe EstadoLibro.REGULAR
        }

        it("devuelve el DTO con los valores actualizados") {
            val (libroRepo, usuarioRepo) = buildRepos()
            val id = crearLibroEnRepo(libroRepo, usuarioRepo)
            val service = buildService(libroRepo, usuarioRepo)

            val resultado = service.actualizarLibro(id, libroDTOBase(titulo = "Nuevo Título"))

            resultado.titulo shouldBe "Nuevo Título"
        }

        it("no cambia el propietario al actualizar") {
            val (libroRepo, usuarioRepo) = buildRepos()
            val id = crearLibroEnRepo(libroRepo, usuarioRepo)
            val service = buildService(libroRepo, usuarioRepo)

            service.actualizarLibro(id, libroDTOBase())

            libroRepo.getById(id).duenio.id shouldBe duenio.id
        }

        it("actualizar con múltiples campos modificados los refleja todos") {
            val (libroRepo, usuarioRepo) = buildRepos()
            val id = crearLibroEnRepo(libroRepo, usuarioRepo)
            val service = buildService(libroRepo, usuarioRepo)

            service.actualizarLibro(
                id,
                libroDTOBase(
                    titulo = "The Pragmatic Programmer",
                    autor = "Andrew Hunt",
                    paginas = 352,
                    estado = "BUENO"
                )
            )

            val libro = libroRepo.getById(id)
            libro.titulo shouldBe "The Pragmatic Programmer"
            libro.autor shouldBe "Andrew Hunt"
            libro.cantidadPaginas shouldBe 352
            libro.estado shouldBe EstadoLibro.BUENO
        }

        it("lanza excepción si se intenta actualizar un libro inexistente") {
            val (libroRepo, usuarioRepo) = buildRepos()
            val service = buildService(libroRepo, usuarioRepo)

            shouldThrow<Exception> {
                service.actualizarLibro(9999, libroDTOBase())
            }
        }

        it("actualizar no modifica otros libros del repositorio") {
            val (libroRepo, usuarioRepo) = buildRepos()
            val service = buildService(libroRepo, usuarioRepo)
            val idA = service.crearLibro(libroDTOBase(titulo = "Libro A"), duenio.id).id
            val idB = service.crearLibro(libroDTOBase(titulo = "Libro B"), duenio.id).id

            service.actualizarLibro(idA, libroDTOBase(titulo = "Libro A Actualizado"))

            libroRepo.getById(idB).titulo shouldBe "Libro B"
        }
    }

    describe("getLibro") {

        it("devuelve el DTO del libro cuando existe") {
            val (libroRepo, usuarioRepo) = buildRepos()
            val service = buildService(libroRepo, usuarioRepo)
            val creado = service.crearLibro(libroDTOBase(titulo = "Domain-Driven Design"), duenio.id)

            val resultado = service.getLibro(creado.id)

            resultado.titulo shouldBe "Domain-Driven Design"
            resultado.id shouldBe creado.id
        }

        it("lanza NotFoundException cuando el libro no existe") {
            val (libroRepo, usuarioRepo) = buildRepos()
            val service = buildService(libroRepo, usuarioRepo)

            shouldThrow<NotFoundException> {
                service.getLibro(9999)
            }
        }

        it("el DTO incluye el propietarioId correcto") {
            val (libroRepo, usuarioRepo) = buildRepos()
            val service = buildService(libroRepo, usuarioRepo)
            val creado = service.crearLibro(libroDTOBase(), duenio.id)

            val resultado = service.getLibro(creado.id)

            resultado.propietarioId shouldBe duenio.id
        }

        it("el DTO devuelto refleja el estado correcto del libro") {
            val (libroRepo, usuarioRepo) = buildRepos()
            val service = buildService(libroRepo, usuarioRepo)
            val creado = service.crearLibro(libroDTOBase(estado = "MALO"), duenio.id)

            val resultado = service.getLibro(creado.id)

            resultado.estado shouldBe "MALO"
        }
    }
})