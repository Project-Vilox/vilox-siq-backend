package com.example.fleetIq.service;

import com.example.fleetIq.entity.Usuario;
import com.example.fleetIq.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class UsuarioService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                         PasswordEncoder passwordEncoder,
                         EmailService emailService,
                         JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtService = jwtService;
    }

    /**
     * Implementación de UserDetailsService para Spring Security
     */
    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + correo));
    }

    /**
     * Registrar nuevo usuario
     */
    public Usuario registrarUsuario(String correo, String contrasena) {
        logger.info("🔐 Iniciando registro de usuario: {}", correo);

        // Verificar si el usuario ya existe
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new RuntimeException("Ya existe un usuario con este correo electrónico");
        }

        // Crear nuevo usuario
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setCorreo(correo);
        nuevoUsuario.setContrasena(passwordEncoder.encode(contrasena));
        nuevoUsuario.setCorreoVerificado(false);
        nuevoUsuario.setActivo(true);
        nuevoUsuario.setRol(Usuario.Rol.USUARIO);

        // Generar token de verificación
        String tokenVerificacion = UUID.randomUUID().toString();
        nuevoUsuario.setTokenVerificacion(tokenVerificacion);
        nuevoUsuario.setFechaExpiracionToken(LocalDateTime.now().plusHours(24)); // Expira en 24 horas

        // Guardar usuario
        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);
        logger.info("✅ Usuario registrado exitosamente: {}", correo);

        // Enviar correo de verificación
        try {
            emailService.enviarCorreoVerificacion(correo, tokenVerificacion);
            logger.info("📧 Correo de verificación enviado a: {}", correo);
        } catch (Exception e) {
            logger.error("❌ Error al enviar correo de verificación: {}", e.getMessage());
            // No fallar el registro si el correo no se puede enviar
        }

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

        // Enviar correo de bienvenida
        try {
            emailService.enviarCorreoBienvenida(usuario.getCorreo(), usuario.getCorreo());
        } catch (Exception e) {
            logger.error("❌ Error al enviar correo de bienvenida: {}", e.getMessage());
        }

        return true;
    }

    /**
     * Reenviar correo de verificación
     */
    public boolean reenviarVerificacion(String correo) {
        logger.info("📧 Reenviando verificación para: {}", correo);

        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correo);
        
        if (usuarioOpt.isEmpty()) {
            logger.warn("❌ Usuario no encontrado: {}", correo);
            return false;
        }

        Usuario usuario = usuarioOpt.get();

        if (usuario.getCorreoVerificado()) {
            logger.warn("⚠️ La cuenta ya está verificada: {}", correo);
            return false;
        }

        // Generar nuevo token
        String nuevoToken = UUID.randomUUID().toString();
        usuario.setTokenVerificacion(nuevoToken);
        usuario.setFechaExpiracionToken(LocalDateTime.now().plusHours(24));
        usuarioRepository.save(usuario);

        // Enviar correo
        try {
            emailService.enviarCorreoVerificacion(correo, nuevoToken);
            logger.info("✅ Correo de verificación reenviado a: {}", correo);
            return true;
        } catch (Exception e) {
            logger.error("❌ Error al reenviar correo de verificación: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Autenticar usuario y generar tokens JWT
     */
    public AuthResponse autenticar(String correo, String contrasena) {
        logger.info("🔐 Intentando autenticar usuario: {}", correo);

        // Buscar usuario
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        // Verificar contraseña
        if (!passwordEncoder.matches(contrasena, usuario.getContrasena())) {
            logger.warn("❌ Contraseña incorrecta para usuario: {}", correo);
            throw new RuntimeException("Credenciales inválidas");
        }

        // Verificar que la cuenta esté verificada
        if (!usuario.getCorreoVerificado()) {
            logger.warn("⚠️ Cuenta no verificada: {}", correo);
            throw new RuntimeException("Debes verificar tu cuenta antes de iniciar sesión. Revisa tu correo electrónico.");
        }

        // Verificar que la cuenta esté activa
        if (!usuario.getActivo()) {
            logger.warn("⚠️ Cuenta inactiva: {}", correo);
            throw new RuntimeException("Tu cuenta está desactivada. Contacta al administrador.");
        }

        // Generar tokens
        String accessToken = jwtService.generateToken(usuario);
        String refreshToken = jwtService.generateRefreshToken(usuario);

        logger.info("✅ Usuario autenticado exitosamente: {}", correo);

        return new AuthResponse(accessToken, refreshToken, usuario);
    }

    /**
     * Buscar usuario por ID
     */
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
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
     * Limpiar tokens de verificación expirados
     */
    @Transactional
    public void limpiarTokensExpirados() {
        int tokensLimpiados = usuarioRepository.limpiarTokensExpirados(LocalDateTime.now());
        if (tokensLimpiados > 0) {
            logger.info("🧹 Limpiados {} tokens de verificación expirados", tokensLimpiados);
        }
    }

    /**
     * Clase para respuesta de autenticación
     */
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private Usuario usuario;

        public AuthResponse(String accessToken, String refreshToken, Usuario usuario) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.usuario = usuario;
        }

        // Getters y setters
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        
        public Usuario getUsuario() { return usuario; }
        public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    }
}