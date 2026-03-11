package ar.edu.unsam.phm.repository
import ar.edu.unsam.phm.Libro
import ar.edu.unsam.phm.Usuario
import org.springframework.stereotype.Component

@Component
class Repository<T : Any> {
    val objetos = mutableListOf<T>()

    fun create(objeto: T) {
        objetos.add(objeto)
    }

    fun all(): List<T> = objetos
}

@Component
class UsuarioRepository : Repository<Usuario>() {

    fun findById(id: Int): Usuario? = objetos.find { it.id == id }

    fun update(usuario: Usuario) {
        val i = objetos.indexOfFirst { it.id == usuario.id }
        if (i >= 0) objetos[i] = usuario
    }
}

@Component
class LibroRepository : Repository<Libro>()