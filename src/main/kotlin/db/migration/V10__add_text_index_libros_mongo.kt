package db.migration

import com.mongodb.client.MongoClients
import org.bson.Document
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V10__add_text_index_libros_mongo : BaseJavaMigration() {

    override fun migrate(context: Context) {
        val mongoUri = System.getenv("MONGODB_URI") ?: "mongodb://booklibre_app:booklibre_app_secret@localhost:27017,localhost:27018/booklibreMongo?authSource=booklibreMongo&wtimeoutMS=5000&socketTimeoutMS=5000&connectTimeoutMS=5000"
        val dbName = mongoUri.substringAfterLast("/").substringBefore("?")

        MongoClients.create(mongoUri).use { client ->
            val database = client.getDatabase(dbName)
            val librosCollection = database.getCollection("libros")

            val indexOptions = com.mongodb.client.model.IndexOptions()
                .name("libros_text_index")
                .defaultLanguage("spanish")
            
            val keys = Document()
                .append("titulo", "text")
                .append("autor", "text")
                .append("descripcion", "text")

            librosCollection.createIndex(keys, indexOptions)
        }
    }
}
