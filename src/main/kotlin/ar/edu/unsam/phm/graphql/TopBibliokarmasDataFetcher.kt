package ar.edu.unsam.phm.graphql

import ar.edu.unsam.phm.dtos.UsuarioBibliokarmaDTO
import ar.edu.unsam.phm.services.UsuarioService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery

@DgsComponent
class TopBibliokarmasDataFetcher(
    private val usuarioService: UsuarioService,
) {
    @DgsQuery
    fun topBibliokarmas(): List<UsuarioBibliokarmaDTO> =
        usuarioService.obtenerTop5Bibliokarmas()
}