package com.example.fleetIq.service;

import com.example.fleetIq.entity.Usuario;
import com.example.fleetIq.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UsuarioServiceSimple {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioServiceSimple.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Registrar nuevo usuario (versión simple)
     */
    public Usuario registrarUsuario(String correo, String contrasena) {
        logger.info("🔐 Iniciando registro de usuario: {}", correo);

        // Verificar si el usuario ya existe
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new RuntimeException("Ya existe un usuario con este correo electrónico");
        }

        // Crear nuevo usuario (sin encriptar por ahora)
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setCorreo(correo);
        nuevoUsuario.setContrasena(contrasena); // Sin encriptar por simplicidad
        nuevoUsuario.setCorreoVerificado(false);
        nuevoUsuario.setActivo(true);
        nuevoUsuario.setRol(Usuario.Rol.USUARIO);

        // Generar token de verificación
        String tokenVerificacion = UUID.randomUUID().toString();
        nuevoUsuario.setTokenVerificacion(tokenVerificacion);
        nuevoUsuario.setFechaExpiracionToken(LocalDateTime.now().plusHours(24));

        // Guardar usuario
        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);
        logger.info("✅ Usuario registrado exitosamente: {}", correo);

        return usuarioGuardado;
    }

    /**
     * Verificar cuenta de usuario
     */
    public boolean verificarCuenta(String token) {
        logger.info("🔍 Verificando token: {}", token);

        Optional<Usuario> usuarioOpt = usuarioRepository.findByTokenVerificacion(token);
        
        if (usuarioOpt.isEmpty()) {
            logger.warn("❌ Token de verificación no encontrado: {}", token);
            return false;
        }

        Usuario usuario = usuarioOpt.get();

        // Verificar si el token ha expirado
        if (usuario.getFechaExpiracionToken().isBefore(LocalDateTime.now())) {
            logger.warn("⏰ Token de verificación expirado para usuario: {}", usuario.getCorreo());
            return false;
        }

        // Verificar cuenta
        usuario.setCorreoVerificado(true);
        usuario.setTokenVerificacion(null);
        usuario.setFechaExpiracionToken(null);
        usuarioRepository.save(usuario);

        logger.info("✅ Cuenta verificada exitosamente: {}", usuario.getCorreo());
        return true;
    }

    /**
     * Autenticar usuario (versión simple)
     */
    public AuthResponseSimple autenticar(String correo, String contrasena) {
        logger.info("🔐 Intentando autenticar usuario: {}", correo);

        // Buscar usuario
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        // Verificar contraseña (comparación simple)
        if (!contrasena.equals(usuario.getContrasena())) {
            logger.warn("❌ Contraseña incorrecta para usuario: {}", correo);
            throw new RuntimeException("Credenciales inválidas");
        }

        // Verificar que la cuenta esté verificada
        if (!usuario.getCorreoVerificado()) {
            logger.warn("⚠️ Cuenta no verificada: {}", correo);
            throw new RuntimeException("Debes verificar tu cuenta antes de iniciar sesión.");
        }

        // Verificar que la cuenta esté activa
        if (!usuario.getActivo()) {
            logger.warn("⚠️ Cuenta inactiva: {}", correo);
            throw new RuntimeException("Tu cuenta está desactivada.");
        }

        logger.info("✅ Usuario autenticado exitosamente: {}", correo);
        return new AuthResponseSimple("simple-token-" + UUID.randomUUID(), usuario);
    }

    /**
     * Buscar usuario por correo
     */
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo);
    }

    /**
     * Listar todos los usuarios activos
     */
    public List<Usuario> listarUsuariosActivos() {
        return usuarioRepository.findByActivoTrue();
    }

    /**
     * Clase para respuesta de autenticación simple
     */
    public static class AuthResponseSimple {
        private String token;
        private Usuario usuario;

        public AuthResponseSimple(String token, Usuario usuario) {
            this.token = token;
            this.usuario = usuario;
        }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        
        public Usuario getUsuario() { return usuario; }
        public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    }
}