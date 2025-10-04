package com.example.fleetIq.dto;

import com.example.fleetIq.entity.Usuario;

public class AuthResponse {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private UsuarioInfo usuario;

    // Constructores
    public AuthResponse() {}

    public AuthResponse(String accessToken, String refreshToken, long expiresIn, Usuario usuario) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.usuario = new UsuarioInfo(usuario);
    }

    // Getters y setters
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UsuarioInfo getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioInfo usuario) {
        this.usuario = usuario;
    }

    // Clase interna para información del usuario
    public static class UsuarioInfo {
        private Long id;
        private String correo;
        private String rol;
        private boolean correoVerificado;
        private boolean activo;

        public UsuarioInfo(Usuario usuario) {
            this.id = usuario.getId();
            this.correo = usuario.getCorreo();
            this.rol = usuario.getRol().name();
            this.correoVerificado = usuario.getCorreoVerificado();
            this.activo = usuario.getActivo();
        }

        // Getters y setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getCorreo() { return correo; }
        public void setCorreo(String correo) { this.correo = correo; }
        
        public String getRol() { return rol; }
        public void setRol(String rol) { this.rol = rol; }
        
        public boolean isCorreoVerificado() { return correoVerificado; }
        public void setCorreoVerificado(boolean correoVerificado) { this.correoVerificado = correoVerificado; }
        
        public boolean isActivo() { return activo; }
        public void setActivo(boolean activo) { this.activo = activo; }
    }
}