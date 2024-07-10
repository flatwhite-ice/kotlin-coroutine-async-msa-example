package com.flatwhite.template.coroutine

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CoroutineExampleApplication

fun main(args: Array<String>) {
    runApplication<CoroutineExampleApplication>(*args)
}
