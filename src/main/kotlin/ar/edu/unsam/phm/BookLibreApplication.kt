package ar.edu.unsam.phm

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class BookLibreApplication

fun main(args: Array<String>) {
    runApplication<BookLibreApplication>(*args)
}