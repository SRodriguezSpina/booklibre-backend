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
class UsuarioRepository : Repository<Usuario>()

@Component
class LibroRepository : Repository<Libro>()