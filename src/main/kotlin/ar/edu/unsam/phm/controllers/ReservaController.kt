package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.dtos.ReservaDTO
import ar.edu.unsam.phm.dtos.CrearReservaDTO
import ar.edu.unsam.phm.services.ReservaService
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/reservas")
class ReservaController(val reservaService: ReservaService) {
    @GetMapping("/{usuarioId}")
    fun getReservas(@PathVariable usuarioId: Int): List<ReservaDTO> = reservaService.obtenerReservas(usuarioId)

    @PostMapping
    fun crearReserva(@RequestBody body: CrearReservaDTO) {
        reservaService.crearReserva(body)
    }
}