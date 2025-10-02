package com.example.fleetIq.dto;

    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.EqualsAndHashCode;
    import lombok.NoArgsConstructor;

    import java.time.LocalDate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(of = "dni")
    public class ChoferDto {
        private String id;
        private String dni;
        private String nombre;
        private String apellidos;
        private String telefono;
        private String email;
        private String licenciaNumero;
        private String licenciaCategoria;
        private LocalDate licenciaVencimiento;
    }