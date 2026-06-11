package ar.edu.unsam.phm.dtos

data class SaludCatalogoDTO(
    val total: Int,
    val prestados: Int,
    val disponiblesNuncaReservados: Int,
    val disponiblesReservadosAFuturo: Int,
    val disponiblesDevueltos: Int,
)
