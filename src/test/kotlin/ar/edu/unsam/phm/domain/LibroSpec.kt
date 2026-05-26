package ar.edu.unsam.phm.domain

import ar.edu.unsam.phm.exceptions.BusinessException
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class LibroSpec : DescribeSpec({
    isolationMode = IsolationMode.InstancePerTest

    describe("Compruebo Libro.kt") {

        val duenio = Usuario(
            "ana", "pass",
            nombre = "Ana García",
            desc = "Publicadora",
            bibliokarmas = 500,
        ).apply { id = 10 }
        val duenioRef = DuenioRef(id = duenio.id, nombre = duenio.nombre)

        val usuarioPobre   = Usuario("p", "p", "Pobre",   "sin karma", bibliokarmas = 0)
        val usuarioRico    = Usuario("r", "r", "Rico",    "con karma", bibliokarmas = 2000)

        val desde = LocalDate.of(2026, 3, 1).atStartOfDay()
        val hasta = LocalDate.of(2026, 3, 10).atStartOfDay() // 10 días

        // ── LibroComun ────────────────────────────────────────────────────────
        describe("LibroComun") {

            val libro = LibroComun(
                LibroData(
                    duenio = duenioRef,
                    titulo = "1984", autor = "George Orwell",
                    descripcion = "Distopía", genero = Genero.LITERATURA_CLASICA,
                    cantidadPaginas = 300, isbn = "9780000000001", idioma = Idioma.ESPANOL,
                    editorial = "Debolsillo", fechaPublicacion = LocalDate.of(1949, 6, 8),
                    estado = EstadoLibro.EXCELENTE, imagenUrl = "",
                )
            ).apply { id = 1 }

            it("PLUS BIBLIOKARMA CON USUARIO CON MENOS DE 1000 KARMA USA MULTIPLICADOR 5") {
                libro.plusBiblioKarma(usuarioPobre) shouldBe 1500
            }

            it("PLUS BIBLIOKARMA CON USUARIO CON MAS DE 1000 KARMA USA MULTIPLICADOR 2") {
                libro.plusBiblioKarma(usuarioRico) shouldBe 600
            }

            it("CALCULA BIBLIOKARMA TOTAL CORRECTAMENTE") {
                libro.calcularBiblioKarmaTotal(usuarioPobre, desde, hasta) shouldBe 1550
            }

            describe("calcularBiblioKarmaTotal — validaciones") {
                it("LANZA BusinessException CUANDO HASTA ES IGUAL A DESDE") {
                    val mismaFecha = LocalDate.of(2026, 3, 1).atStartOfDay()
                    assertThrows<BusinessException> {
                        libro.calcularBiblioKarmaTotal(usuarioPobre, mismaFecha, mismaFecha)
                    }
                }

                it("LANZA BusinessException CUANDO HASTA ES ANTERIOR A DESDE") {
                    assertThrows<BusinessException> {
                        libro.calcularBiblioKarmaTotal(
                            usuarioPobre,
                            LocalDate.of(2026, 3, 10).atStartOfDay(),
                            LocalDate.of(2026, 3, 1).atStartOfDay()
                        )
                    }
                }
            }

            describe("LibroComun — validaciones") {
                it("LANZA BusinessException SI LA CANTIDAD DE PAGINAS ES 0") {
                    assertThrows<BusinessException> {
                        LibroComun(
                            LibroData(
                                duenio = duenioRef, titulo = "Test", autor = "Test",
                                descripcion = "Descripción de prueba", genero = Genero.DRAMA, cantidadPaginas = 0,
                                isbn = "9780000000005", idioma = Idioma.ESPANOL, editorial = "Editorial de prueba",
                                fechaPublicacion = LocalDate.now(), estado = EstadoLibro.BUENO, imagenUrl = "",
                            ),
                        )
                    }
                }
            }
        }

        // ── LibroConDedicatoria ───────────────────────────────────────────────
        describe("LibroConDedicatoria") {

            val libro = LibroConDedicatoria(
                LibroData(
                    duenio = duenioRef,
                    titulo = "El Principito", autor = "Saint-Exupéry",
                    descripcion = "Clásico", genero = Genero.LITERATURA_CLASICA,
                    cantidadPaginas = 100, isbn = "9780000000002", idioma = Idioma.ESPANOL,
                    editorial = "Salamandra", fechaPublicacion = LocalDate.of(1943, 4, 6),
                    estado = EstadoLibro.MUY_BUENO, imagenUrl = "",
                )
            ).apply { id = 2 }

            it("PLUS BIBLIOKARMA SIN RESERVAS DEL DUENIO ES 200") {
                libro.plusBiblioKarma(usuarioPobre) shouldBe 200
            }

            it("PLUS BIBLIOKARMA AUMENTA SEGUN RESERVAS DEL DUENIO") {
                libro.plusBiblioKarma(usuarioPobre, cantidadReservasDelLibro = 1) shouldBe 210
            }

            it("CALCULA BIBLIOKARMA TOTAL CON DUENIO SIN RESERVAS") {
                libro.calcularBiblioKarmaTotal(usuarioPobre, desde, hasta) shouldBe 250
            }
        }

        // ── LibroColeccionable ────────────────────────────────────────────────
        describe("LibroColeccionable") {

            val libro = LibroColeccionable(
                LibroData(
                    duenio = duenioRef,
                    titulo = "Dune", autor = "Frank Herbert",
                    descripcion = "Sci-fi épico", genero = Genero.CIENCIA_FICCION,
                    cantidadPaginas = 500, isbn = "9780000000003", idioma = Idioma.ESPANOL,
                    editorial = "Minotauro", fechaPublicacion = LocalDate.of(1965, 8, 1),
                    estado = EstadoLibro.EXCELENTE, imagenUrl = "",
                )
            ).apply { id = 3 }

            it("PLUS BIBLIOKARMA ES CEIL(KARMA/5) + PAGINAS") {
                // ceil(2000/5) + 500 = 400 + 500 = 900
                libro.plusBiblioKarma(usuarioRico) shouldBe 900
            }

            it("PLUS BIBLIOKARMA CON KARMA 0 ES SOLO LAS PAGINAS") {
                // ceil(0/5) + 500 = 0 + 500 = 500
                libro.plusBiblioKarma(usuarioPobre) shouldBe 500
            }

            it("CALCULA BIBLIOKARMA TOTAL CORRECTAMENTE") {
                // (5 * 10) + 900 = 950
                libro.calcularBiblioKarmaTotal(usuarioRico, desde, hasta) shouldBe 950
            }

            describe("LibroColeccionable — validaciones") {
                it("LANZA BusinessException SI EL USUARIO TIENE BIBLIOKARMAS NEGATIVOS") {
                    val usuarioKarmaNegativo = Usuario(
                        "x", "x", "X", "", bibliokarmas = -1
                    )
                    assertThrows<BusinessException> {
                        libro.plusBiblioKarma(usuarioKarmaNegativo)
                    }
                }
            }
        }

        // ── Calificaciones ────────────────────────────────────────────────────
        describe("Calificaciones") {

            val libro = LibroComun(
                LibroData(
                    duenio = duenioRef,
                    titulo = "Sapiens", autor = "Yuval Harari",
                    descripcion = "Historia", genero = Genero.LITERATURA_CLASICA,
                    cantidadPaginas = 400, isbn = "9780000000004", idioma = Idioma.ESPANOL,
                    editorial = "Debate", fechaPublicacion = LocalDate.of(2011, 1, 1),
                    estado = EstadoLibro.EXCELENTE, imagenUrl = "",
                )
            ).apply { id = 4 }

            it("CALIFICACION INICIAL ES 0.0 CUANDO NO HAY CALIFICACIONES") {
                libro.calificacion shouldBe 0.0
            }

            it("AGREGA UNA CALIFICACION Y CALCULA EL PROMEDIO CORRECTAMENTE") {
                libro.agregarCalificacion(Calificacion(usuarioId = 1, valor = 4, comentario = "", fecha = LocalDateTime.now()))
                libro.calificacion shouldBe 4.0
            }

            it("PROMEDIO CON MULTIPLES CALIFICACIONES ES CORRECTO") {
                libro.agregarCalificacion(Calificacion(usuarioId = 1, valor = 4, comentario = "", fecha = LocalDateTime.now()))
                libro.agregarCalificacion(Calificacion(usuarioId = 2, valor = 2, comentario = "", fecha = LocalDateTime.now()))
                libro.calificacion shouldBe 3.0
            }

            it("NO SE PUEDE CALIFICAR DOS VECES CON EL MISMO USUARIO") {
                libro.agregarCalificacion(Calificacion(usuarioId = 1, valor = 5, comentario = "", fecha = LocalDateTime.now()))
                assertThrows<BusinessException> {
                    libro.agregarCalificacion(Calificacion(usuarioId = 1, valor = 3, comentario = "", fecha = LocalDateTime.now()))
                }
            }

            it("LA CALIFICACION NO CAMBIA TRAS UN INTENTO DUPLICADO") {
                libro.agregarCalificacion(Calificacion(usuarioId = 1, valor = 5, comentario = "", fecha = LocalDateTime.now()))
                assertThrows<BusinessException> {
                    libro.agregarCalificacion(Calificacion(usuarioId = 1, valor = 1, comentario = "", fecha = LocalDateTime.now()))
                }
                libro.calificacion shouldBe 5.0
            }

            describe("Calificaciones — casos borde") {
                it("AGREGAR CALIFICACION CON VALOR MENOR A 1 LANZA BusinessException") {
                    assertThrows<BusinessException> {
                        libro.agregarCalificacion(Calificacion(usuarioId = 1, valor = 0, comentario = "", fecha = LocalDateTime.now()))
                    }
                }

                it("AGREGAR CALIFICACION CON VALOR MAYOR A 5 LANZA BusinessException") {
                    assertThrows<BusinessException> {
                        libro.agregarCalificacion(Calificacion(usuarioId = 1, valor = 6, comentario = "", fecha = LocalDateTime.now()))
                    }
                }

                it("GETCLASIFICACION REDONDEA A 1 DECIMAL CORRECTAMENTE") {
                    libro.agregarCalificacion(Calificacion(usuarioId = 1, valor = 4, comentario = "", fecha = LocalDateTime.now()))
                    libro.agregarCalificacion(Calificacion(usuarioId = 2, valor = 3, comentario = "", fecha = LocalDateTime.now()))
                    libro.agregarCalificacion(Calificacion(usuarioId = 3, valor = 5, comentario = "", fecha = LocalDateTime.now()))
                    libro.calificacion shouldBe 4.0
                }
            }
        }

        // ── ISBN ──────────────────────────────────────────────────────────────
        describe("ISBN") {
            it("LANZA BusinessException SI EL ISBN CONTIENE CARACTERES NO NUMERICOS") {
                assertThrows<BusinessException> {
                    LibroComun(
                        LibroData(
                            duenio = duenioRef,
                            titulo = "Test",
                            autor = "Test",
                            descripcion = "Descripción de prueba",
                            genero = Genero.DRAMA,
                            cantidadPaginas = 10,
                            isbn = "9780ABC000001",
                            idioma = Idioma.ESPANOL,
                            editorial = "Editorial de prueba",
                            fechaPublicacion = LocalDate.now(),
                            estado = EstadoLibro.BUENO,
                            imagenUrl = "",
                        )
                    )
                }
            }

            it("LANZA BusinessException SI EL ISBN NO TIENE 13 DIGITOS") {
                assertThrows<BusinessException> {
                    LibroComun(
                        LibroData(
                            duenio = duenioRef,
                            titulo = "Test",
                            autor = "Test",
                            descripcion = "Descripción de prueba",
                            genero = Genero.DRAMA,
                            cantidadPaginas = 10,
                            isbn = "123456789012",
                            idioma = Idioma.ESPANOL,
                            editorial = "Editorial de prueba",
                            fechaPublicacion = LocalDate.now(),
                            estado = EstadoLibro.BUENO,
                            imagenUrl = "",
                        )
                    )
                }
            }
        }
    }
})