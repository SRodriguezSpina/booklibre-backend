package ar.edu.unsam.phm.dtos

data class UsuarioResponse(
    val id: Int,
    val nombre: String,
    val email: String,
    val desc: String,
    val fechaRegistro: String,
    val celular: String?,
    val ciudad: String,
    val tipoUsuario: String,
    val bibliokarmas: Int,
    val imagenUrl: String? = null,
    val reservados: Int,
    val leidos: Int,
    val esAdmin: Boolean = false,
)

data class UsuarioUpdateResponse(
    val usuario: UsuarioResponse,
)

data class UsuarioUpdateRequest(
    val nombre: String,
    val desc: String,
    val email: String,
    val celular: String? = null,
    val ciudad: String,
    val tipoUsuario: String,
    val imagenUrl: String? = null,
)

data class LibroUsuarioItemDTO(
    val id: Int,
    val titulo: String,
    val autor: String,
    val genero: String,
    val disponible: Boolean,
    val fechaAgregado: String?,
    val imagenUrl: String?
)
