package ar.edu.unsam.phm.domain

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Transient as JpaTransient
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId

@MappedSuperclass
open class BaseEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,

    @field:MongoId(FieldType.OBJECT_ID)
    @field:JpaTransient
    var mongoId: ObjectId? = null,
)