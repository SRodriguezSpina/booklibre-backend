package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.dtos.ReservaDTO
import ar.edu.unsam.phm.services.ReservaService
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/reservas")
class ReservaController(val reservaService: ReservaService) {
    @GetMapping("/{usuarioId}")
    fun getReservas(@PathVariable usuarioId: Int): List<ReservaDTO> = reservaService.obtenerReservas(usuarioId)
}