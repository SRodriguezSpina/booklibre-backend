package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.Usuario
import org.springframework.stereotype.Component

@Component
class UsuarioRepository : CommonRepository<Usuario>() {

    fun findById(id: Int): Usuario? = lista.find { it.id == id }
}