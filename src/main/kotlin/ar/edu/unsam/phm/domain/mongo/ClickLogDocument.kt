package ar.edu.unsam.phm.domain.mongo

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.time.LocalDateTime

/**
 * Registro de cada vez que un usuario entra al detalle de un libro.
 * Colección: click_logs
 *
 * Se usa para:
 *  - Query del Integrante 1: libro más clickeado
 *  - Bonus: historial de clicks en el perfil del publicador
 */
@Document(collection = "click_logs")
data class ClickLogDocument(

    @MongoId(FieldType.OBJECT_ID)
    val id: ObjectId? = null,

    // Momento exacto en que el usuario entró al detalle
    @Indexed
    val fechaHora: LocalDateTime = LocalDateTime.now(),

    // Email del usuario logueado (viene del JWT via Authentication.name)
    val nombreUsuario: String,

    // Referencia al libro (mismo Int que usa Libro.id en MongoDB)
    @Indexed
    val libroId: Int,

    // Desnormalizado para evitar joins en la query de más clickeado
    val libroTitulo: String,
)
