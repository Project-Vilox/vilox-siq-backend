package com.example.fleetIq.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@fleetiq.com}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Enviar correo de verificación de cuenta
     */
    public void enviarCorreoVerificacion(String destinatario, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(destinatario);
            message.setSubject("🚛 FleetIQ - Verifica tu cuenta");
            
            String verificationUrl = frontendUrl + "/verificar-cuenta?token=" + token;
            
            String contenido = String.format("""
                ¡Bienvenido a FleetIQ! 🚛
                
                Gracias por registrarte en nuestra plataforma de seguimiento vehicular.
                
                Para completar tu registro, por favor verifica tu cuenta haciendo clic en el siguiente enlace:
                
                %s
                
                Este enlace expirará en 24 horas.
                
                Si no creaste esta cuenta, puedes ignorar este correo.
                
                ¡Gracias por elegir FleetIQ!
                El equipo de FleetIQ
                """, verificationUrl);
            
            message.setText(contenido);
            
            mailSender.send(message);
            logger.info("📧 Correo de verificación enviado a: {}", destinatario);
            
        } catch (Exception e) {
            logger.error("❌ Error al enviar correo de verificación a {}: {}", destinatario, e.getMessage());
            throw new RuntimeException("Error al enviar correo de verificación", e);
        }
    }

    /**
     * Enviar correo de bienvenida después de verificar cuenta
     */
    public void enviarCorreoBienvenida(String destinatario, String nombre) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(destinatario);
            message.setSubject("🎉 ¡Cuenta verificada exitosamente - FleetIQ!");
            
            String contenido = String.format("""
                ¡Hola %s! 🎉
                
                ¡Tu cuenta ha sido verificada exitosamente!
                
                Ya puedes acceder a todas las funcionalidades de FleetIQ:
                • 📍 Seguimiento en tiempo real de vehículos
                • 📊 Reportes detallados de rutas
                • 🔔 Alertas y notificaciones
                • 🗺️ Gestión de geocercas
                
                Accede a tu cuenta en: %s/login
                
                Si tienes alguna pregunta, no dudes en contactarnos.
                
                ¡Disfruta de FleetIQ!
                El equipo de FleetIQ
                """, nombre, frontendUrl);
            
            message.setText(contenido);
            
            mailSender.send(message);
            logger.info("📧 Correo de bienvenida enviado a: {}", destinatario);
            
        } catch (Exception e) {
            logger.error("❌ Error al enviar correo de bienvenida a {}: {}", destinatario, e.getMessage());
            // No lanzar excepción aquí, ya que la verificación ya se completó
        }
    }

    /**
     * Enviar correo de restablecimiento de contraseña
     */
    public void enviarCorreoRestablecimiento(String destinatario, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(destinatario);
            message.setSubject("🔐 FleetIQ - Restablece tu contraseña");
            
            String resetUrl = frontendUrl + "/restablecer-contrasena?token=" + token;
            
            String contenido = String.format("""
                Hola,
                
                Recibimos una solicitud para restablecer la contraseña de tu cuenta FleetIQ.
                
                Para crear una nueva contraseña, haz clic en el siguiente enlace:
                
                %s
                
                Este enlace expirará en 1 hora.
                
                Si no solicitaste este cambio, puedes ignorar este correo. Tu contraseña actual seguirá siendo válida.
                
                Por seguridad, no compartas este enlace con nadie.
                
                El equipo de FleetIQ
                """, resetUrl);
            
            message.setText(contenido);
            
            mailSender.send(message);
            logger.info("📧 Correo de restablecimiento enviado a: {}", destinatario);
            
        } catch (Exception e) {
            logger.error("❌ Error al enviar correo de restablecimiento a {}: {}", destinatario, e.getMessage());
            throw new RuntimeException("Error al enviar correo de restablecimiento", e);
        }
    }
}