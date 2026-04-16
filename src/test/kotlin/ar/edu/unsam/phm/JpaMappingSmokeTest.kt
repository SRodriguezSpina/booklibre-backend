package ar.edu.unsam.phm

import ar.edu.unsam.phm.repository.LibroJpaRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
    ],
)
class JpaMappingSmokeTest {

    @Autowired
    lateinit var libroJpaRepository: LibroJpaRepository

    @Test
    fun `entity manager factory arranca y LibroJpaRepository ejecuta consultas`() {
        libroJpaRepository.findAll()
    }
}
