package ar.edu.unsam.phm.exceptions

import ar.edu.unsam.phm.controllers.AuthController
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ErrorResponse(val message: String)

@RestControllerAdvice(assignableTypes = [AuthController::class])
class AuthExceptionHandler {

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(error: UnauthorizedException) =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(error.message.toString()))

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(error: ConflictException) =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(error.message.toString()))
}