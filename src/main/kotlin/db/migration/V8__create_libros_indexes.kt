package db.migration

import com.mongodb.client.MongoClients
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V8__create_libros_indexes : BaseJavaMigration() {

    override fun migrate(context: Context) {
        val mongoUri = System.getenv("MONGODB_URI") ?: "mongodb://localhost:27017/booklibre"
        val dbName = mongoUri.substringAfterLast("/").substringBefore("?")

        MongoClients.create(mongoUri).use { client ->
            val collection = client.getDatabase(dbName).getCollection("libros")
            val opts = IndexOptions().background(true)

            collection.createIndex(Indexes.ascending("id"), opts)
            collection.createIndex(Indexes.ascending("activo"), opts)
            collection.createIndex(Indexes.ascending("duenio.id"), opts)
            collection.createIndex(Indexes.ascending("duenio.id", "activo"), opts)
            collection.createIndex(Indexes.ascending("genero"), opts)
            collection.createIndex(Indexes.ascending("isbn"), opts)
            collection.createIndex(Indexes.ascending("titulo"), opts)
            collection.createIndex(Indexes.ascending("autor"), opts)
            collection.createIndex(Indexes.descending("calificacion"), opts)
            collection.createIndex(Indexes.ascending("activo", "titulo"), opts)
        }
    }
}
