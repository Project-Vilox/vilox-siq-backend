package com.example.fleetIq.repository;

import com.example.fleetIq.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Buscar usuario por correo electrónico
     */
    Optional<Usuario> findByCorreo(String correo);

    /**
     * Verificar si existe un usuario con el correo dado
     */
    boolean existsByCorreo(String correo);

    /**
     * Buscar usuario por token de verificación
     */
    Optional<Usuario> findByTokenVerificacion(String tokenVerificacion);

    /**
     * Buscar usuarios activos
     */
    List<Usuario> findByActivoTrue();

    /**
     * Buscar usuarios con correo verificado
     */
    List<Usuario> findByCorreoVerificadoTrue();

    /**
     * Buscar usuarios por rol
     */
    List<Usuario> findByRol(Usuario.Rol rol);

    /**
     * Buscar usuarios con tokens de verificación expirados
     */
    @Query("SELECT u FROM Usuario u WHERE u.tokenVerificacion IS NOT NULL " +
           "AND u.fechaExpiracionToken < :fechaActual AND u.correoVerificado = false")
    List<Usuario> findUsuariosConTokenExpirado(@Param("fechaActual") LocalDateTime fechaActual);

    /**
     * Eliminar tokens de verificación expirados
     */
    @Query("UPDATE Usuario u SET u.tokenVerificacion = NULL, u.fechaExpiracionToken = NULL " +
           "WHERE u.fechaExpiracionToken < :fechaActual AND u.correoVerificado = false")
    int limpiarTokensExpirados(@Param("fechaActual") LocalDateTime fechaActual);
}