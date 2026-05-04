package dev.gaonstack.gaonclimatecore.api.response

data class ApiResponse<T>(
    val code: String,
    val message: String?,
    val data: T?,
) {
    companion object {
        fun <T> ok(data: T?): ApiResponse<T> =
            ApiResponse(
                code = "OK",
                message = null,
                data = data,
            )

        fun error(code: String, message: String?): ApiResponse<Nothing> =
            ApiResponse(
                code = code,
                message = message,
                data = null,
            )
    }
}
