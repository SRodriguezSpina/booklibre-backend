package ar.edu.unsam.phm.persistence.mappers

import ar.edu.unsam.phm.domain.Credencial
import ar.edu.unsam.phm.domain.TipoUsuario
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.domain.UsuarioLector
import ar.edu.unsam.phm.domain.UsuarioLectorPublicador
import ar.edu.unsam.phm.domain.UsuarioPublicador
import ar.edu.unsam.phm.persistence.entities.UsuarioEntity

fun UsuarioEntity.toDomain(): Usuario {
    val tipo: TipoUsuario = when (tipoUsuario.trim().uppercase().replace(" ", "_")) {
        "PUBLICADOR" -> UsuarioPublicador()
        "LECTOR_PUBLICADOR", "LECTORPUBLICADOR" -> UsuarioLectorPublicador()
        else -> UsuarioLector()
    }

    val usuario = Usuario(
        credencial = Credencial(email, password),
        nombre = nombre,
        desc = desc,
        celular = celular,
        ciudad = ciudad,
        tipoUsuario = tipo,
        bibliokarmas = bibliokarmas,
        imagenUrl = imagenUrl,
        fechaRegistro = fechaRegistro,
        reservas = mutableListOf(),
    )

    usuario.id = id ?: 0
    return usuario
}

fun Usuario.toEntity(): UsuarioEntity {
    return UsuarioEntity(
        id = id,
        email = getCredencial().email,
        password = getCredencial().password,
        nombre = nombre,
        desc = desc,
        celular = celular,
        ciudad = ciudad,
        tipoUsuario = when (tipoUsuario) {
            is UsuarioLector -> "LECTOR"
            is UsuarioPublicador -> "PUBLICADOR"
            is UsuarioLectorPublicador -> "LECTOR_PUBLICADOR"
            else -> "LECTOR"
        },
        bibliokarmas = bibliokarmas,
        imagenUrl = imagenUrl,
        fechaRegistro = fechaRegistro,
    )
}

