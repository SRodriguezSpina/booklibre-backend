package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.dtos.AuthResponseUsuario
import ar.edu.unsam.phm.dtos.LoginRequest
import ar.edu.unsam.phm.dtos.RegisterRequest

interface AuthService {
    fun login(request: LoginRequest): AuthResponseUsuario
    fun register(request: RegisterRequest): AuthResponseUsuario
}
