package com.example.fleetIq.listener;

import org.springframework.context.ApplicationEvent;

public class ViajeUpdateEvent extends ApplicationEvent {
    private final String viajeId;

    public ViajeUpdateEvent(Object source, String viajeId) {
        super(source);
        this.viajeId = viajeId;
    }

    public String getViajeId() {
        return viajeId;
    }
}