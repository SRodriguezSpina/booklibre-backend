package ar.edu.unsam.phm.domain

import ar.edu.unsam.phm.exceptions.BusinessException

enum class EstadoLibro {
    EXCELENTE,
    MUY_BUENO,
    BUENO,
    MALO,
    REGULAR;

    companion object {
        fun fromString(value: String): EstadoLibro =
            entries.find { it.name.equals(value.trim(), ignoreCase = true) }
                ?: throw BusinessException("Estado de libro inválido: $value")
    }
}