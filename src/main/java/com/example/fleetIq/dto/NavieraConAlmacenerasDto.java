package com.example.fleetIq.dto;

import lombok.Data;

import java.util.List;

@Data
public class NavieraConAlmacenerasDto {
    private NavieraDto naviera;
    private List<AlmaceneraDto> almaceneras;

    public NavieraConAlmacenerasDto(NavieraDto naviera, List<AlmaceneraDto> almaceneras) {
        this.naviera = naviera;
        this.almaceneras = almaceneras;
    }
}