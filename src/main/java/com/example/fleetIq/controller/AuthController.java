package com.example.fleetIq.controller;

import com.example.fleetIq.dto.AuthResponse;
import com.example.fleetIq.dto.LoginRequest;
import com.example.fleetIq.dto.RegistroRequest;
import com.example.fleetIq.entity.Usuario;
import com.example.fleetIq.service.JwtService;
import com.example.fleetIq.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtService jwtService;

    /**
     * Registro de nuevo usuario
     */
    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@Valid @RequestBody RegistroRequest request) {
        try {
            logger.info("🔐 Solicitud de registro para: {}", request.getCorreo());

            // Validar que las contraseñas coincidan
            if (!request.getContrasena().equals(request.getConfirmarContrasena())) {
                return ResponseEntity.badRequest()
                    .body(crearRespuestaError("Las contraseñas no coinciden"));
            }

            // Registrar usuario
            Usuario usuario = usuarioService.registrarUsuario(request.getCorreo(), request.getContrasena());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Usuario registrado exitosamente. Revisa tu correo para verificar tu cuenta.");
            response.put("usuario", Map.of(
                "id", usuario.getId(),
                "correo", usuario.getCorreo(),
                "correoVerificado", usuario.getCorreoVerificado()
            ));

            logger.info("✅ Usuario registrado exitosamente: {}", request.getCorreo());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("❌ Error en registro: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(crearRespuestaError(e.getMessage()));
        }
    }

    /**
     * Inicio de sesión
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            logger.info("🔐 Intento de login para: {}", request.getCorreo());

            // Autenticar usuario
            UsuarioService.AuthResponse authResult = usuarioService.autenticar(
                request.getCorreo(), 
                request.getContrasena()
            );

            // Crear respuesta
            AuthResponse response = new AuthResponse(
                authResult.getAccessToken(),
                authResult.getRefreshToken(),
                86400000, // 24 horas en milisegundos
                authResult.getUsuario()
            );

            logger.info("✅ Login exitoso para: {}", request.getCorreo());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Error en login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(crearRespuestaError(e.getMessage()));
        }
    }

    /**
     * Verificar cuenta con token
     */
    @PostMapping("/verificar")
    public ResponseEntity<?> verificarCuenta(@RequestParam String token) {
        try {
            logger.info("🔍 Verificando cuenta con token: {}", token);

            boolean verificado = usuarioService.verificarCuenta(token);

            if (verificado) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "¡Cuenta verificada exitosamente! Ya puedes iniciar sesión.");

                logger.info("✅ Cuenta verificada exitosamente");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest()
                    .body(crearRespuestaError("Token de verificación inválido o expirado"));
            }

        } catch (Exception e) {
            logger.error("❌ Error en verificación: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(crearRespuestaError("Error al verificar la cuenta"));
        }
    }

    /**
     * Reenviar correo de verificación
     */
    @PostMapping("/reenviar-verificacion")
    public ResponseEntity<?> reenviarVerificacion(@RequestBody Map<String, String> request) {
        try {
            String correo = request.get("correo");
            logger.info("📧 Reenviando verificación para: {}", correo);

            if (correo == null || correo.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(crearRespuestaError("El correo es obligatorio"));
            }

            boolean enviado = usuarioService.reenviarVerificacion(correo);

            if (enviado) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Correo de verificación enviado. Revisa tu bandeja de entrada.");

                logger.info("✅ Verificación reenviada a: {}", correo);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest()
                    .body(crearRespuestaError("No se pudo enviar el correo de verificación"));
            }

        } catch (Exception e) {
            logger.error("❌ Error al reenviar verificación: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(crearRespuestaError("Error al reenviar el correo de verificación"));
        }
    }

    /**
     * Renovar token de acceso
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(crearRespuestaError("Refresh token es obligatorio"));
            }

            // Extraer usuario del refresh token
            String correo = jwtService.extractUsername(refreshToken);
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Validar refresh token
            if (!jwtService.isTokenValid(refreshToken, usuario)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(crearRespuestaError("Refresh token inválido"));
            }

            // Generar nuevo access token
            String nuevoAccessToken = jwtService.generateToken(usuario);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("accessToken", nuevoAccessToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", 86400000); // 24 horas

            logger.info("✅ Token renovado para: {}", correo);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Error al renovar token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(crearRespuestaError("Error al renovar el token"));
        }
    }

    /**
     * Endpoint para verificar si el token es válido
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(crearRespuestaError("Token no proporcionado"));
            }

            String token = authHeader.substring(7);
            String correo = jwtService.extractUsername(token);
            Usuario usuario = usuarioService.buscarPorCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (jwtService.isTokenValid(token, usuario)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("valid", true);
                response.put("usuario", Map.of(
                    "id", usuario.getId(),
                    "correo", usuario.getCorreo(),
                    "rol", usuario.getRol().name(),
                    "correoVerificado", usuario.getCorreoVerificado()
                ));

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(crearRespuestaError("Token inválido"));
            }

        } catch (Exception e) {
            logger.error("❌ Error al validar token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(crearRespuestaError("Token inválido"));
        }
    }

    /**
     * Método auxiliar para crear respuestas de error
     */
    private Map<String, Object> crearRespuestaError(String mensaje) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", mensaje);
        return error;
    }
}