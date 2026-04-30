package ar.edu.unsam.phm.exceptions

@ResponseStatus(HttpStatus.FORBIDDEN)
class ForbiddenException(msg: String) : RuntimeException(msg)
