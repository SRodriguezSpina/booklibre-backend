package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.readmodels.PromedioCalificacionPorTipoView
import ar.edu.unsam.phm.repository.mongo.LibroMongoRepository
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class AnalisisCalificacionesServiceSpec : DescribeSpec({
    isolationMode = IsolationMode.InstancePerTest

    describe("Dado un AnalisisCalificacionesService") {
        val libroMongoRepository = mockk<LibroMongoRepository>()
        val analisisCalificacionesService = AnalisisCalificacionesService(libroMongoRepository)

        val promedioComun = object : PromedioCalificacionPorTipoView {
            override val tipoLibro = "Comun"
            override val promedioCalificacion = 4.25
        }
        val promedioConDedicatoria = object : PromedioCalificacionPorTipoView {
            override val tipoLibro = "ConDedicatoria"
            override val promedioCalificacion = 3.5
        }

        it("mapea los promedios por tipo de libro a DTOs") {
            every { libroMongoRepository.obtenerPromedioCalificacionPorTipo() } returns listOf(
                promedioComun,
                promedioConDedicatoria,
            )

            val resultado = analisisCalificacionesService.obtenerAnalisisCalificaciones()

            resultado shouldHaveSize 2
            resultado[0].tipoLibro shouldBe "Comun"
            resultado[0].promedioCalificacion shouldBe 4.25
            resultado[1].tipoLibro shouldBe "ConDedicatoria"
            resultado[1].promedioCalificacion shouldBe 3.5

            verify(exactly = 1) { libroMongoRepository.obtenerPromedioCalificacionPorTipo() }
        }

        it("cuando no hay libros calificados devuelve una lista vacia") {
            every { libroMongoRepository.obtenerPromedioCalificacionPorTipo() } returns emptyList()

            val resultado = analisisCalificacionesService.obtenerAnalisisCalificaciones()

            resultado.shouldBeEmpty()
            verify(exactly = 1) { libroMongoRepository.obtenerPromedioCalificacionPorTipo() }
        }
    }
})
