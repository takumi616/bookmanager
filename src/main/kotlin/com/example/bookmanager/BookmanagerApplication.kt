package com.example.bookmanager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BookmanagerApplication

fun main(args: Array<String>) {
	runApplication<BookmanagerApplication>(*args)
}
