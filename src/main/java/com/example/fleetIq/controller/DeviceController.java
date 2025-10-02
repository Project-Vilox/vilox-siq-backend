package com.example.fleetIq.controller;

import com.example.fleetIq.model.Device;
import com.example.fleetIq.service.DeviceService; // Asegúrate de tener este servicio
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:8080", "http://446ae7f42f09.ngrok-free.app"})
public class DeviceController {

    @Autowired
    private DeviceService deviceService; // Asegúrate de implementar DeviceService

    @GetMapping("/devices")
    public List<Device> getDevices() {
        return deviceService.getAllDevices();
    }
}