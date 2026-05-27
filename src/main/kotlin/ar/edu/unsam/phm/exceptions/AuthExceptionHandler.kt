package ar.edu.unsam.phm.exceptions

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.MethodArgumentNotValidException
import java.time.Instant

data class ErrorResponse(
    val timestamp: String = Instant.now().toString(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
)

@RestControllerAdvice
class AuthExceptionHandler {

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(error: UnauthorizedException, request: HttpServletRequest) =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ErrorResponse(status = 401, error = "Unauthorized", message = error.message.toString(), path = request.requestURI)
        )

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(error: ConflictException, request: HttpServletRequest) =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(status = 409, error = "Conflict", message = error.message.toString(), path = request.requestURI)
        )

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(error: NotFoundException, request: HttpServletRequest) =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(status = 404, error = "Not Found", message = error.message.toString(), path = request.requestURI)
        )

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(error: BusinessException, request: HttpServletRequest) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(status = 400, error = "Bad Request", message = error.message.toString(), path = request.requestURI)
        )

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(error: ForbiddenException, request: HttpServletRequest) =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ErrorResponse(status = 403, error = "Forbidden", message = error.message.toString(), path = request.requestURI)
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(error: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val errorMessage = error.bindingResult.fieldErrors.joinToString(", ") { it.defaultMessage ?: "Error de validación" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(status = 400, error = "Bad Request", message = errorMessage, path = request.requestURI)
        )
    }
}