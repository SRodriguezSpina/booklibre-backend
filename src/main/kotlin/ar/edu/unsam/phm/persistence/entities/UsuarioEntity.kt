package ar.edu.unsam.phm.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "usuarios",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_usuarios_email", columnNames = ["email"]),
        UniqueConstraint(name = "uk_usuarios_celular", columnNames = ["celular"]),
    ],
)
class UsuarioEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var email: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false)
    var nombre: String,

    @Column(nullable = false)
    var desc: String = "",

    @Column(nullable = false)
    var celular: String = "",

    @Column(nullable = false)
    var ciudad: String = "",

    @Column(nullable = false)
    var tipoUsuario: String = "LECTOR",

    @Column(nullable = false)
    var bibliokarmas: Int = 0,

    @Column(nullable = true)
    var imagenUrl: String? = null,

    @Column(nullable = false)
    var fechaRegistro: LocalDate = LocalDate.now(),
)

