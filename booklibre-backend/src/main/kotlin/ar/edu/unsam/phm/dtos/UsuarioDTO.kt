package ar.edu.unsam.phm.dtos

data class UsuarioResponse(
    val id: Int,
    val nombre: String,
    val desc: String,
    val email: String,
    val fechaRegistro: String
    val celular: String,
    val ciudad: String,
    val tipoUsuario: String,
    val bibliokarmas: Int
)

data class UsuarioUpdateRequest(
    val nombre: String,
    val desc: String,
    val email: String,
    val celular: String,
    val ciudad: String,
    val tipoUsuario: String
)

data class LibroUsuarioItemDTO(
    val id: Int,
    val titulo: String,
    val autor: String,
    val genero: String,
    val disponible: Boolean,
    val fechaAgregado: String?
)
