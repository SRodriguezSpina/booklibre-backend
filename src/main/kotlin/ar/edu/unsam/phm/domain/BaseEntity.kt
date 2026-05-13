package ar.edu.unsam.phm.domain

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.Id as MongoID

@MappedSuperclass
class BaseEntity(
    @MongoID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0
)