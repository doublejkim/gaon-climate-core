package dev.gaonstack.gaonclimatecore

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class GaonClimateCoreApplication

fun main(args: Array<String>) {
    runApplication<GaonClimateCoreApplication>(*args)
}
