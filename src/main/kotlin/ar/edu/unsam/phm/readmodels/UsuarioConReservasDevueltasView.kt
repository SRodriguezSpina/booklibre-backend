package ar.edu.unsam.phm.readmodels

interface UsuarioConReservasDevueltasView {
    val usuarioId: Int
    val usuarioNombre: String
    val cantidadReservasDevueltas: Int
}