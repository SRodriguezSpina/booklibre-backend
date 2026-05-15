package ar.edu.unsam.phm.domain

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.time.LocalDateTime

@Document("calificaciones")
@CompoundIndexes(
    CompoundIndex(name = "uk_calificacion_libro_usuario", def = "{'libroId': 1, 'usuarioId': 1}", unique = true),
    CompoundIndex(name = "ix_calificacion_libro_fecha", def = "{'libroId': 1, 'fecha': -1}"),
)
data class Calificacion(
    @MongoId(FieldType.OBJECT_ID)
    var id: ObjectId? = null,

    @Indexed
    var libroId: Int? = null,

    var usuarioId: Int,
    var valor: Int,
    var comentario: String,
    var fecha: LocalDateTime = LocalDateTime.now(),
)