package ar.edu.unsam.phm

import ar.edu.unsam.phm.repository.BaseEntity

interface TipoUsuario{
} //si cobran comportamientos lo migro a strategy

class Usuario(
    private val credencial: Credencial,
    val nombre: String,
    var desc: String,   //con límiteee
    var email: String,  //que respete un formato
    var celular: String,
    var ciudad: String,
    var tipoUsuario: TipoUsuario,
    var bibliokarmas: Int
) {
    fun getCredencial(): Credencial = credencial
}

class UsuarioLector () : TipoUsuario {}
class UsuarioPublicador () : TipoUsuario {}
class UsuarioLectorPublicador () : TipoUsuario {}