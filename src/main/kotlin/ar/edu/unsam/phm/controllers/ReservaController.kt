package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.domain.TipoReserva
import ar.edu.unsam.phm.dtos.CrearReservaDTO
import ar.edu.unsam.phm.dtos.PagedResponse
import ar.edu.unsam.phm.dtos.ReservaDTO
import ar.edu.unsam.phm.dtos.UsuarioConMasReservasDTO
import ar.edu.unsam.phm.dtos.UsuarioReservasDevueltasDTO
import ar.edu.unsam.phm.services.ReservaService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reservas")
class ReservaController(val reservaService: ReservaService) {
    @Transactional(readOnly = true)
    @GetMapping
    fun getReservas(
        @RequestParam usuarioId: Int,
        @RequestParam tipo: TipoReserva,
        @RequestParam(name = "page", defaultValue = "0") page: Int,
        @RequestParam(name = "size", defaultValue = "10") size: Int,
    ): PagedResponse<ReservaDTO> = reservaService.obtenerReservas(usuarioId, tipo, page, size)

    @PostMapping("/crear")
    fun crearReserva(@RequestBody body: CrearReservaDTO) {
        reservaService.crearReserva(body)
    }

    @GetMapping("/usuarios-con-devoluciones")
    fun getUsuariosConDevoluciones() : List<UsuarioReservasDevueltasDTO> =
        reservaService.obtenerUsuariosConReservasDevueltas()

    @GetMapping("/usuarios-con-mas-de-n-reservas")
    fun getUsuariosConMasDeNReservas(@RequestParam minReservas: Int): List<UsuarioConMasReservasDTO> =
        reservaService.obtenerUsuariosConMasDeNReservas(minReservas)
}