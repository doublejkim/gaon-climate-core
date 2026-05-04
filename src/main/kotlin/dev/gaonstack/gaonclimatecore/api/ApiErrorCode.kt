package dev.gaonstack.gaonclimatecore.api

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

fun apiErrorCode(statusCode: HttpStatusCode): String =
    runCatching { HttpStatus.valueOf(statusCode.value()).name }
        .getOrDefault("ERROR")
