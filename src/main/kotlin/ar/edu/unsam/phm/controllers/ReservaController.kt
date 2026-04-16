package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.domain.TipoReserva
import ar.edu.unsam.phm.dtos.ReservaDTO
import ar.edu.unsam.phm.dtos.CrearReservaDTO
import ar.edu.unsam.phm.services.ReservaService
import ar.edu.unsam.phm.mappers.toDTO
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/reservas")
class ReservaController(val reservaService: ReservaService) {
    @GetMapping
    fun getReservas(
        @RequestParam usuarioId: Int,
        @RequestParam tipo: TipoReserva
    ): List<ReservaDTO> = reservaService.obtenerReservas(usuarioId, tipo).map{ it.toDTO() }

    @PostMapping
    fun crearReserva(@RequestBody body: CrearReservaDTO) {
        reservaService.crearReserva(body)
    }
}