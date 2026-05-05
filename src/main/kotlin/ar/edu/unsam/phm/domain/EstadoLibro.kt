package ar.edu.unsam.phm.domain

import ar.edu.unsam.phm.exceptions.BusinessException

enum class EstadoLibro {
    EXCELENTE,
    MUY_BUENO,
    BUENO,
    MALO,
    REGULAR
    ;

    companion object {
        fun fromString(raw: String): EstadoLibro {
            val normalized = raw.trim().uppercase()
            return try {
                valueOf(normalized)
            } catch (_: IllegalArgumentException) {
                throw BusinessException("Estado de libro inválido: $raw")
            }
        }
    }
}