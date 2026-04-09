package ar.edu.unsam.phm.testing.inmemory.repository

import ar.edu.unsam.phm.domain.BaseEntity

class EntityNotFoundException(message: String) : Exception(message)

open class CommonRepository<T : BaseEntity> {
    val lista = mutableListOf<T>()
    var nextId = 1

    fun create(objeto: T): T {
        objeto.id = nextId++
        lista.add(objeto)
        return objeto
    }

    fun delete(objeto: T) {
        lista.removeIf { it.id == objeto.id }
    }

    fun update(objeto: T) {
        val index = lista.indexOfFirst { it.id == objeto.id }
        if (index != -1) {
            lista[index] = objeto
        } else {
            throw EntityNotFoundException("No se encontró el elemento con ID ${objeto.id}")
        }
    }

    fun getById(id: Int): T {
        return lista.find { it.id == id } ?: throw EntityNotFoundException("No se encontró el elemento con ID $id")
    }

    fun findAll(): List<T> = lista.toList()

    fun search(condicion: (T) -> Boolean): List<T> = lista.filter(condicion)

    fun limpiarRepositorio() {
        lista.clear()
        nextId = 1
    }
}