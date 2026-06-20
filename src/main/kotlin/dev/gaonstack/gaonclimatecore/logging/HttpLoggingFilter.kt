package dev.gaonstack.gaonclimatecore.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.net.InetAddress
import java.time.LocalDateTime

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class HttpLoggingFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(HttpLoggingFilter::class.java)
    private val serverHostName = runCatching { InetAddress.getLocalHost().hostName }.getOrDefault("unknown")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestWrapper = ContentCachingRequestWrapper(request, MAX_BODY_LENGTH)
        val responseWrapper = ContentCachingResponseWrapper(response)
        val startedAt = System.currentTimeMillis()

        try {
            filterChain.doFilter(requestWrapper, responseWrapper)
        } finally {
            writeAccessLog(requestWrapper, responseWrapper, startedAt)
            responseWrapper.copyBodyToResponse()
        }
    }

    private fun writeAccessLog(
        request: ContentCachingRequestWrapper,
        response: ContentCachingResponseWrapper,
        startedAt: Long,
    ) {
        val elapsedMs = System.currentTimeMillis() - startedAt
        log.info(
            "http_access time={} server={} method={} url={} headers={} params={} request_body={} response={}",
            LocalDateTime.now(),
            request.serverInfo(),
            request.method,
            request.requestUrl(),
            request.maskedHeaders(),
            request.parameterMap.toLogString(),
            request.cachedBody(),
            response.responseInfo(elapsedMs),
        )
    }

    private fun HttpServletRequest.serverInfo(): Map<String, Any?> =
        mapOf(
            "host_name" to serverHostName,
            "server_name" to serverName,
            "server_port" to serverPort,
            "local_addr" to localAddr,
            "local_port" to localPort,
            "remote_addr" to remoteAddr,
        )

    private fun HttpServletRequest.requestUrl(): String {
        val query = queryString?.takeIf { it.isNotBlank() }
        return if (query == null) {
            requestURL.toString()
        } else {
            "${requestURL}?$query"
        }
    }

    private fun HttpServletRequest.maskedHeaders(): Map<String, String> =
        headerNames.asSequence().associateWith { headerName ->
            if (headerName.isSensitiveHeader()) {
                "***"
            } else {
                getHeaders(headerName).toList().joinToString(",")
            }
        }

    private fun Map<String, Array<String>>.toLogString(): Map<String, List<String>> =
        mapValues { (_, values) -> values.toList() }

    private fun ContentCachingRequestWrapper.cachedBody(): String =
        contentAsByteArray.toBodyString(contentType)

    private fun ContentCachingResponseWrapper.responseInfo(elapsedMs: Long): Map<String, Any?> =
        mapOf(
            "status" to status,
            "headers" to headerNames.associateWith { headerName ->
                if (headerName.isSensitiveHeader()) {
                    "***"
                } else {
                    getHeaders(headerName).joinToString(",")
                }
            },
            "content_type" to contentType,
            "body" to contentAsByteArray.toBodyString(contentType).maskSensitiveBodyFields(),
            "elapsed_ms" to elapsedMs,
        )

    private fun ByteArray.toBodyString(contentType: String?): String {
        if (isEmpty()) {
            return ""
        }
        if (!contentType.isLoggableBodyType()) {
            return "[${size} bytes]"
        }

        return toString(Charsets.UTF_8)
            .maskSensitiveBodyFields()
            .let { if (it.length > MAX_BODY_LENGTH) it.take(MAX_BODY_LENGTH) + "...[truncated]" else it }
    }

    private fun String?.isLoggableBodyType(): Boolean {
        if (this == null) {
            return true
        }

        return startsWith(MediaType.APPLICATION_JSON_VALUE, ignoreCase = true) ||
            startsWith(MediaType.TEXT_PLAIN_VALUE, ignoreCase = true) ||
            startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE, ignoreCase = true)
    }

    private fun String.isSensitiveHeader(): Boolean =
        equals("Authorization", ignoreCase = true) ||
            equals("Cookie", ignoreCase = true) ||
            equals("Set-Cookie", ignoreCase = true) ||
            equals("X-Admin-Token", ignoreCase = true)

    private fun String.maskSensitiveBodyFields(): String =
        replace(Regex(""""(api_key|api_key_hash|claim_code|authorization|token|password)"\s*:\s*"[^"]*"""", RegexOption.IGNORE_CASE)) {
            """"${it.groupValues[1]}":"***""""
        }

    companion object {
        private const val MAX_BODY_LENGTH = 4096
    }
}
