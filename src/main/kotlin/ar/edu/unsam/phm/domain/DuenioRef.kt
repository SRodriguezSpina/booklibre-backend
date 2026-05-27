package ar.edu.unsam.phm.domain

import org.springframework.data.mongodb.core.mapping.Field

data class DuenioRef(
    @Field("_id")
    val id: Int,
    val nombre: String,
)
