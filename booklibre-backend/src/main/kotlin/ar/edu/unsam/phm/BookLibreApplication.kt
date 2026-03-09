package ar.edu.unsam.phm.domain

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class BookLibreApplication

fun main(args: Array<String>) {
    runApplication<BookLibreApplication>(*args)
}