package com.example.fleetIq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FleetIqApplication {
	public static void main(String[] args) {
		SpringApplication.run(FleetIqApplication.class, args);
	}
}