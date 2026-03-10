package ar.edu.unsam.phm

interface TipoUsuario{
} //si cobran comportamientos lo migro a strategy

class Usuario(
    val id: Int,    //para ser único?
    private val credencial: Credencial,
    val nombre: String,
    var desc: String,   //con límiteee
    var email: String,  //que respete un formato
    var celular: String,
    var ciudad: String,
    var tipoUsuario: TipoUsuario,
    var bibliokarmas: Int
) {
    // calificar libro
    // cobrar karmas
}

class UsuarioLector () : TipoUsuario {}
class UsuarioPublicador () : TipoUsuario {}
class UsuarioLectorPublicador () : TipoUsuario {}