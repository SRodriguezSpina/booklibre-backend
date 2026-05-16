package ar.edu.unsam.phm.testing.inmemory.repository

import ar.edu.unsam.phm.domain.Calificacion
import ar.edu.unsam.phm.domain.Usuario

class UsuarioRepository : CommonRepository<Usuario>() {
    fun findById(id: Int): Usuario? = lista.find { it.id == id }
    fun findByEmail(email: String): Usuario? = lista.find { it.email.equals(email, ignoreCase = true) }
    fun findByCelular(celular: String): Usuario? = lista.find { it.celular == celular }

}