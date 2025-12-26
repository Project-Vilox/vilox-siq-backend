package com.example.fleetIq.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@Configuration
public class TimezoneConfig {

  @PostConstruct
  public void init() {
    TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));

    System.out.println("╔════════════════════════════════════════════════════════════════╗");
    System.out.println("║  ⏰ ZONA HORARIA: America/Lima (UTC-5)                         ║");
    System.out.println("║  🕐 Hora actual: " +
        ZonedDateTime.now(ZoneId.of("America/Lima"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        +
        "                    ║");
    System.out.println("╚════════════════════════════════════════════════════════════════╝");
  }
}