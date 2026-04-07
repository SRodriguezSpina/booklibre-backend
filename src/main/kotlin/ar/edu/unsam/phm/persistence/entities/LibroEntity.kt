package ar.edu.unsam.phm.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "libros")
class LibroEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var titulo: String,

    @Column(nullable = false)
    var autor: String,

    @Column(nullable = false)
    var genero: String,

    @Column(nullable = false)
    var estado: String,

    @Column(nullable = false)
    var isbn: String,

    @Column(nullable = false)
    var idioma: String,

    @Column(nullable = false)
    var editorial: String,

    @Column(nullable = false)
    var fechaPublicacion: String,

    @Column(nullable = false)
    var fechaAgregado: LocalDate = LocalDate.now(),

    @Column(nullable = true)
    var imagenUrl: String? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "duenio_id", nullable = false)
    var duenio: UsuarioEntity,
)

