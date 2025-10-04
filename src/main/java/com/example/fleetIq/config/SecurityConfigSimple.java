package com.example.fleetIq.config;

// Configuración simplificada sin Spring Security
// Para usar cuando las dependencias de Spring Security no están disponibles
public class SecurityConfigSimple {
    
    public SecurityConfigSimple() {
        System.out.println("⚠️ Usando configuración de seguridad simplificada");
        System.out.println("🔧 Para usar Spring Security completo, instala las dependencias:");
        System.out.println("   - spring-boot-starter-security");
        System.out.println("   - jjwt-api, jjwt-impl, jjwt-jackson");
    }
    
    // Simulación de encoder de contraseñas
    public static class SimplePasswordEncoder {
        public String encode(String password) {
            // En producción, usar BCrypt
            return "encoded_" + password;
        }
        
        public boolean matches(String rawPassword, String encodedPassword) {
            return encodedPassword.equals("encoded_" + rawPassword);
        }
    }
    
    public SimplePasswordEncoder passwordEncoder() {
        return new SimplePasswordEncoder();
    }
}