package ar.edu.unsam.phm.helpers

import ar.edu.unsam.phm.domain.TipoUsuario
import ar.edu.unsam.phm.domain.UsuarioLector
import ar.edu.unsam.phm.domain.UsuarioLectorPublicador
import ar.edu.unsam.phm.domain.UsuarioPublicador
import ar.edu.unsam.phm.exceptions.BusinessException

fun parseTipoUsuario(value: String): TipoUsuario {
    val normalized = value.trim().uppercase().replace(" ", "_")
    return when (normalized) {
        "LECTOR" -> UsuarioLector()
        "PUBLICADOR" -> UsuarioPublicador()
        "LECTOR_PUBLICADOR", "LECTORPUBLICADOR" -> UsuarioLectorPublicador()
        else -> throw BusinessException("Tipo de usuario inválido: $value")
    }
}