package ar.edu.unsam.phm

import ar.edu.unsam.phm.repository.BaseEntity

interface TipoUsuario {
}

class Usuario(
    private val credencial: Credencial,
    val nombre: String,
    var desc: String,
    var email: String,
    var celular: String,
    var ciudad: String,
    var tipoUsuario: TipoUsuario,
    var bibliokarmas: Int
) : BaseEntity() {

    fun getCredencial(): Credencial = credencial ///
}

class UsuarioLector () : TipoUsuario {}
class UsuarioPublicador () : TipoUsuario {}
class UsuarioLectorPublicador () : TipoUsuario {}