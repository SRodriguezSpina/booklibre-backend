package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.dtos.UsuarioResponse
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest

interface UsuarioService {
    fun obtenerUsuario(id: Int): UsuarioResponse
    fun actualizarUsuario(id: Int, request: UsuarioUpdateRequest): UsuarioResponse
    fun obtenerLibrosDeUsuario(usuarioId: Int): List<Libro>
}
