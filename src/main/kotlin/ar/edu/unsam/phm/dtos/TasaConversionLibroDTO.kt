package ar.edu.unsam.phm.dtos

data class TasaConversionLibroDTO(
    val libroId: Int,
    val titulo: String,
    val clicks: Int,
    val reservas: Int,
    val tasaConversion: Double
) {
    companion object {
        fun de(libroCache: LibroPopularCacheDTO, reservaCount: Int): TasaConversionLibroDTO {
            val clickCount = libroCache.clicks
            val tasa = if (clickCount > 0) reservaCount.toDouble() / clickCount.toDouble() else 0.0
            return TasaConversionLibroDTO(
                libroId = libroCache.id,
                titulo = libroCache.titulo,
                clicks = clickCount,
                reservas = reservaCount,
                tasaConversion = tasa
            )
        }
    }
}
