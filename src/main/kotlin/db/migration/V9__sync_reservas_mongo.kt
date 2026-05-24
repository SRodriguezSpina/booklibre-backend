package db.migration

import com.mongodb.client.MongoClients
import org.bson.Document
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class V9__sync_reservas_mongo : BaseJavaMigration() {

  override fun migrate(context: Context) {
    val mongoUri = System.getenv("MONGODB_URI") ?: "mongodb://localhost:27017/booklibreMongo"
    val dbName = mongoUri.substringAfterLast("/").substringBefore("?")

    val reservasPorLibro = loadReservasPorLibro(context.connection)

    MongoClients.create(mongoUri).use { client ->
      val librosCollection = client.getDatabase(dbName).getCollection("libros")

      for ((libroId, rangos) in reservasPorLibro) {
        val reservaDocs =
            rangos.map { (desde, hasta) ->
              Document()
                  .append("fechaDesde", toDate(desde))
                  .append("fechaHasta", toDate(hasta))
            }

        librosCollection.updateOne(
            Document("libroId", libroId),
            Document("\$set", Document("reservas", reservaDocs)),
        )
      }
    }
  }

  private fun loadReservasPorLibro(connection: Connection): Map<Int, List<Pair<LocalDateTime, LocalDateTime>>> {
    val map = mutableMapOf<Int, MutableList<Pair<LocalDateTime, LocalDateTime>>>()
    connection.createStatement().use { stmt ->
      stmt
          .executeQuery(
              "SELECT libro_id, fecha_desde, fecha_hasta FROM reservas ORDER BY libro_id, fecha_desde",
          )
          .use { rs ->
            while (rs.next()) {
              val libroId = rs.getInt("libro_id")
              val desde = rs.getTimestamp("fecha_desde")!!.toLocalDateTime()
              val hasta = rs.getTimestamp("fecha_hasta")!!.toLocalDateTime()
              map.getOrPut(libroId) { mutableListOf() }.add(desde to hasta)
            }
          }
    }
    return map
  }

  private fun toDate(value: LocalDateTime): Date =
      Date.from(value.atZone(ZoneId.systemDefault()).toInstant())
}
