//package com.example.fleetIq;
//
//import com.example.fleetIq.service.TokenCache;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.context.annotation.Bean;
//import org.springframework.scheduling.annotation.EnableScheduling;
//
///**
// * Aplicación de prueba para verificar autenticación GPS API
// */
//@SpringBootApplication
//@EnableScheduling
//public class TestAuthApplication implements CommandLineRunner {
//
//    @Autowired
//    private TokenCache tokenCache;
//
//    public static void main(String[] args) {
//        System.setProperty("spring.main.web-application-type", "none");
//        SpringApplication.run(TestAuthApplication.class, args);
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        System.out.println("🚀 Aplicación de prueba iniciada");
//        System.out.println("✅ Token cache inicializado - autenticación funcionando");
//
//        // Mantener la aplicación ejecutándose para ver las renovaciones programadas
//        Thread.sleep(300000); // 5 minutos
//
//        System.out.println("🔚 Finalizando aplicación de prueba");
//    }
//}