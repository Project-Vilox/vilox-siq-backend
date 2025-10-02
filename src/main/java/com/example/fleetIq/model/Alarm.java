package com.example.fleetIq.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "alarms")
public class Alarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "imei", nullable = false)
    private String imei;

    @Column(name = "geofence_id", nullable = false)
    private Long geofenceId;

    @Column(name = "track_time")
    private Long trackTime;

    @Column(name = "entry_time")
    private Long entryTime;

    @Column(name = "exit_time")
    private Long exitTime; // Nuevo campo para tiempo de salida

    @Column(name = "alarm_type", length = 20)
    private String alarmType; // "ENTRY", "EXIT", "ENTRY_EXIT"

    @Column(name = "duration")
    private Long duration; // Duración en segundos dentro de la geocerca

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "plate_number")
    private String plateNumber;

    @Column(name = "fecha_creacion", updatable = false)
    @CreationTimestamp
    private LocalDateTime creationDate;

    // Constructores
    public Alarm() {
    }

    public Alarm(String imei, Long geofenceId, Long entryTime) {
        this.imei = imei;
        this.geofenceId = geofenceId;
        this.entryTime = entryTime;
        this.alarmType = "ENTRY";
    }

    // Getters y Setters (generados por @Data, solo incluyo los nuevos implícitamente)
    // El resto de getters y setters, incluyendo el método setExitTime con cálculo de duración, sigue intacto
    // Método utilitario para verificar si la alarma está activa (sin salida)
    public boolean isActive() {
        return exitTime == null;
    }

    // Método para obtener duración en formato legible
    public String getDurationFormatted() {
        if (duration == null) return "N/A";

        long hours = duration / 3600;
        long minutes = (duration % 3600) / 60;
        long seconds = duration % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}