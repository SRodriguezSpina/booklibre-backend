package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest

interface UsuarioService {
    fun obtenerUsuario(id: Int): Usuario
    fun actualizarUsuario(id: Int, request: UsuarioUpdateRequest): Usuario
    fun obtenerLibrosDeUsuario(usuarioId: Int): List<Libro>
}
