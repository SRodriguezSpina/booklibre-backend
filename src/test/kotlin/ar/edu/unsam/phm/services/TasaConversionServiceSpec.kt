package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Genero
import ar.edu.unsam.phm.domain.Idioma
import ar.edu.unsam.phm.domain.TopLibrosCache
import ar.edu.unsam.phm.dtos.LibroPopularCacheDTO
import ar.edu.unsam.phm.readmodels.LibroReservasCount
import ar.edu.unsam.phm.repository.redis.TopLibrosCacheRepository
import ar.edu.unsam.phm.repository.ReservaJpaRepository
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class TasaConversionServiceSpec : DescribeSpec({
    isolationMode = IsolationMode.InstancePerTest

    describe("Dado un TasaConversionService") {
        val topLibrosCacheRepository = mockk<TopLibrosCacheRepository>()
        val reservaJpaRepository = mockk<ReservaJpaRepository>()
        val libroService = mockk<LibroService>()
        val tasaConversionService = TasaConversionService(
            topLibrosCacheRepository,
            reservaJpaRepository,
            libroService
        )

        val libroHome1 = LibroPopularCacheDTO(
            id = 1, imagenUrl = "", genero = Genero.LITERATURA_CLASICA,
            titulo = "Libro 1", autor = "Autor 1", calificacion = 4.5,
            isbn = "9780000000001", idioma = Idioma.ESPANOL, tipo = "Comun",
            bibliokarma = 100, estado = "BUENO", duenio = "Owner 1", clicks = 10
        )
        val libroHome2 = LibroPopularCacheDTO(
            id = 2, imagenUrl = "", genero = Genero.LITERATURA_CLASICA,
            titulo = "Libro 2", autor = "Autor 2", calificacion = 4.0,
            isbn = "9780000000002", idioma = Idioma.ESPANOL, tipo = "Comun",
            bibliokarma = 200, estado = "BUENO", duenio = "Owner 2", clicks = 0
        )
        val libroHome3 = LibroPopularCacheDTO(
            id = 3, imagenUrl = "", genero = Genero.LITERATURA_CLASICA,
            titulo = "Libro 3", autor = "Autor 3", calificacion = 3.5,
            isbn = "9780000000003", idioma = Idioma.ESPANOL, tipo = "Comun",
            bibliokarma = 300, estado = "BUENO", duenio = "Owner 3", clicks = 5
        )

        val cacheConLibros = TopLibrosCache(
            id = "HOME",
            libros = listOf(libroHome1, libroHome2, libroHome3),
            totalElements = 3L
        )

        val count1 = object : LibroReservasCount {
            override val libroId = 1
            override val count = 5L
        }
        val count3 = object : LibroReservasCount {
            override val libroId = 3
            override val count = 1L
        }

        it("cuando hay cache hit, calcula correctamente las tasas y maneja clicks = 0") {
            every { topLibrosCacheRepository.getCachedTopLibros() } returns cacheConLibros
            every { reservaJpaRepository.countReservasByLibroIds(listOf(1, 2, 3)) } returns listOf(count1, count3)

            val resultado = tasaConversionService.obtenerTasaConversion()

            resultado shouldHaveSize 3
            resultado[0].libroId shouldBe 1
            resultado[0].clicks shouldBe 10
            resultado[0].reservas shouldBe 5
            resultado[0].tasaConversion shouldBe 0.5

            resultado[1].libroId shouldBe 2
            resultado[1].clicks shouldBe 0
            resultado[1].reservas shouldBe 0
            resultado[1].tasaConversion shouldBe 0.0

            resultado[2].libroId shouldBe 3
            resultado[2].clicks shouldBe 5
            resultado[2].reservas shouldBe 1
            resultado[2].tasaConversion shouldBe 0.2

            verify(exactly = 0) { libroService.actualizarCacheTopLibros() }
        }

        it("cuando hay cache miss, regenera la caché y calcula el resultado") {
            every { topLibrosCacheRepository.getCachedTopLibros() } returns null
            every { libroService.actualizarCacheTopLibros() } returns cacheConLibros
            every { reservaJpaRepository.countReservasByLibroIds(listOf(1, 2, 3)) } returns listOf(count1, count3)

            val resultado = tasaConversionService.obtenerTasaConversion()

            resultado shouldHaveSize 3
            resultado[0].tasaConversion shouldBe 0.5

            verify(exactly = 1) { libroService.actualizarCacheTopLibros() }
        }

        it("si la caché está vacía, devuelve una lista vacía") {
            val cacheVacio = TopLibrosCache(id = "HOME", libros = emptyList(), totalElements = 0L)
            every { topLibrosCacheRepository.getCachedTopLibros() } returns cacheVacio

            val resultado = tasaConversionService.obtenerTasaConversion()

            resultado.shouldBeEmpty()
            verify(exactly = 0) { reservaJpaRepository.countReservasByLibroIds(any()) }
        }
    }
})
