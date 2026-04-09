package ar.edu.unsam.phm.testing.inmemory.repository

import ar.edu.unsam.phm.domain.Libro

class LibroRepository : CommonRepository<Libro>() {
    fun getLibros(): List<Libro> = lista

    fun getLibrosDeUsuario(idUsuario: Int): List<Libro> {
        return lista.filter { idUsuario == it.duenio.id }
    }

    fun search(value: String): List<Libro> {
        if (value.isBlank()) return lista

        return lista.filter { libro ->
            libro.titulo.contains(value, ignoreCase = true) ||
                libro.autor.contains(value, ignoreCase = true)
        }
    }

    fun getByTitulo(nombre: String): Libro {
        return lista.find { it.titulo.equals(nombre, ignoreCase = true) }
            ?: throw EntityNotFoundException("No se encontró el libro con título: $nombre")
    }
}