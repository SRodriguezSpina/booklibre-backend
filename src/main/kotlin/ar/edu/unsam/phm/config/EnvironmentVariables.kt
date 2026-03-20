package ar.edu.unsam.phm.config

object EnvironmentVariables {
    val API_URL: String = System.getenv("API_URL") ?: "http://localhost:8080"
}