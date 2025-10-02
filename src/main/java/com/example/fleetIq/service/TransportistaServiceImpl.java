package com.example.fleetIq.service;

import com.example.fleetIq.dto.*;
import com.example.fleetIq.model.Empresa;
import com.example.fleetIq.model.OperadoresDeClientes;
import com.example.fleetIq.model.AlmacenerasDeNavieras;
import com.example.fleetIq.repository.TransportistaRepository;
import com.example.fleetIq.repository.OperadoresDeClientesRepository;
import com.example.fleetIq.repository.AlmacenerasDeNavierasRepository;
import com.example.fleetIq.repository.EstablecimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransportistaServiceImpl implements TransportistaService {

    @Autowired
    private TransportistaRepository transportistaRepository;

    @Autowired
    private OperadoresDeClientesRepository operadoresDeClientesRepository;

    @Autowired
    private AlmacenerasDeNavierasRepository almacenerasDeNavierasRepository;

    @Autowired
    private EstablecimientoRepository establecimientoRepository;

    @Override
    @Transactional(readOnly = true)
    public TransportistaInfoResponse getTransportistaInfo(String transportistaId) {
        Optional<Empresa> empresaOptional = transportistaRepository.findByIdAndTipoEmpresa(transportistaId, "TRANSPORTISTA");
        if (!empresaOptional.isPresent()) {
            return null;
        }
        Empresa empresa = empresaOptional.get();

        // Mapear camiones
        List<CamionDto> camiones = empresa.getVehiculos().stream()
                .map(v -> new CamionDto(
                        v.getId(),
                        v.getEmpresa() != null ? v.getEmpresa().getId() : null,
                        v.getPlaca(),
                        v.getImei(),
                        v.getMarca(),
                        v.getModelo(),
                        v.getAno(),
                        v.getTipoVehiculo().name(),
                        v.getCapacidadToneladas(),
                        v.getEstado().name(),
                        v.getActivo(),
                        v.getFechaCreacion()
                ))
                .collect(Collectors.toList());

        // Mapear carretas
        List<CarretaDto> carretas = empresa.getCarretas().stream()
                .map(c -> new CarretaDto(
                        c.getId(),
                        c.getEmpresa() != null ? c.getEmpresa().getId() : null,
                        c.getPlaca(),
                        c.getImei(),
                        c.getMarca(),
                        c.getModelo(),
                        c.getAño(),
                        c.getTipoVehiculo().name(),
                        c.getCapacidadToneladas().doubleValue(),
                        c.getEstado() != null ? c.getEstado().name() : null,
                        c.getActivo(),
                        c.getFechaCreacion()
                ))
                .collect(Collectors.toList());

        // Mapear choferes
        List<ChoferDto> choferes = empresa.getConductorEmpresas().stream()
                .map(ce -> new ChoferDto(
                        ce.getConductor().getId(),
                        ce.getConductor().getDni(),
                        ce.getConductor().getNombre(),
                        ce.getConductor().getApellidos(),
                        ce.getConductor().getTelefono(),
                        ce.getConductor().getEmail(),
                        ce.getConductor().getLicenciaNumero(),
                        ce.getConductor().getLicenciaCategoria(),
                        ce.getConductor().getLicenciaVencimiento()
                ))
                .collect(Collectors.toList());

        // Mapear operadores logísticos y sus clientes
        List<OperadorLogisticosDto> operadoresLogisticos = operadoresDeClientesRepository
                .findOperadoresAndClientesByTransportistaId(transportistaId)
                .stream()
                .collect(Collectors.groupingBy(
                        oc -> oc.getEmpresaOperador(),
                        Collectors.mapping(
                                oc -> new ClienteDto(
                                        oc.getEmpresaCliente().getId(),
                                        oc.getEmpresaCliente().getNombre(),
                                        oc.getEmpresaCliente().getRuc()
                                ),
                                Collectors.toList()
                        )
                ))
                .entrySet().stream()
                .map(entry -> new OperadorLogisticosDto(
                        entry.getKey().getId(),
                        entry.getKey().getNombre(),
                        entry.getKey().getTipoEmpresa(),
                        entry.getKey().getRuc(),
                        entry.getKey().getDireccion(),
                        entry.getKey().getTelefono(),
                        entry.getKey().getEmail(),
                        entry.getKey().getActivo(),
                        entry.getKey().getConfiguracionAlertas(),
                        entry.getKey().getConfiguracionDashboard(),
                        entry.getKey().getFechaCreacion(),
                        entry.getKey().getFechaActualizacion(),
                        entry.getValue()
                ))
                .collect(Collectors.toList());

        // Mapear navieras y sus almaceneras
        List<NavieraConAlmacenerasDto> navieraConAlmaceneras = almacenerasDeNavierasRepository
                .findAllActive()
                .stream()
                .collect(Collectors.groupingBy(
                        an -> an.getEmpresaNaviera(),
                        Collectors.mapping(
                                an -> new AlmaceneraDto(
                                        an.getEmpresaAlmacenera().getId(),
                                        an.getEmpresaAlmacenera().getNombre(),
                                        an.getEmpresaAlmacenera().getTipoEmpresa(),
                                        an.getEmpresaAlmacenera().getRuc(),
                                        an.getEmpresaAlmacenera().getDireccion(),
                                        an.getEmpresaAlmacenera().getTelefono(),
                                        an.getEmpresaAlmacenera().getEmail(),
                                        an.getEmpresaAlmacenera().getActivo(),
                                        an.getEmpresaAlmacenera().getConfiguracionAlertas(),
                                        an.getEmpresaAlmacenera().getConfiguracionDashboard(),
                                        an.getEmpresaAlmacenera().getFechaCreacion(),
                                        an.getEmpresaAlmacenera().getFechaActualizacion()
                                ),
                                Collectors.toList()
                        )
                ))
                .entrySet().stream()
                .map(entry -> new NavieraConAlmacenerasDto(
                        new NavieraDto(
                                entry.getKey().getId(),
                                entry.getKey().getNombre(),
                                entry.getKey().getTipoEmpresa(),
                                entry.getKey().getRuc(),
                                entry.getKey().getDireccion(),
                                entry.getKey().getTelefono(),
                                entry.getKey().getEmail(),
                                entry.getKey().getActivo(),
                                entry.getKey().getConfiguracionAlertas(),
                                entry.getKey().getConfiguracionDashboard(),
                                entry.getKey().getFechaCreacion(),
                                entry.getKey().getFechaActualizacion()
                        ),
                        entry.getValue()
                ))
                .collect(Collectors.toList());

        // Mapear establecimientos
        List<EstablecimientosDto> establecimientos = establecimientoRepository.findAll().stream()
                .map(e -> new EstablecimientosDto(
                        e.getId(),
                        e.getEmpresaId(),
                        e.getNombre(),
                        e.getTipo(),
                        e.getDireccion(),
                        e.getLatitud() != null ? e.getLatitud().doubleValue() : null,
                        e.getLongitud() != null ? e.getLongitud().doubleValue() : null,
                        e.getPublico() != null ? e.getPublico() : false,
                        e.getActivo() != null ? e.getActivo() : false,
                        e.getConfiguracionSla(),
                        e.getFechaCreacion(),
                        null
                ))
                .collect(Collectors.toList());

        // Mapear almaceneras
        List<AlmaceneraDto> almaceneras = transportistaRepository.findAll().stream()
                .filter(e -> "ALMACENERA".equals(e.getTipoEmpresa()))
                .map(e -> new AlmaceneraDto(
                        e.getId(),
                        e.getNombre(),
                        e.getTipoEmpresa(),
                        e.getRuc(),
                        e.getDireccion(),
                        e.getTelefono(),
                        e.getEmail(),
                        e.getActivo(),
                        e.getConfiguracionAlertas(),
                        e.getConfiguracionDashboard(),
                        e.getFechaCreacion(),
                        e.getFechaActualizacion()
                ))
                .collect(Collectors.toList());

        // Mapear almaceneras con sus propios establecimientos
        List<AlmaceneraConEstablecimientosDto> almaceneraConEstablecimientos = almaceneras.stream()
                .map(almacenera -> {
                    List<EstablecimientosDto> establecimientosPorAlmacenera = establecimientoRepository
                            .findByEmpresaId(almacenera.getId()).stream()
                            .map(e -> new EstablecimientosDto(
                                    e.getId(),
                                    e.getEmpresaId(),
                                    e.getNombre(),
                                    e.getTipo(),
                                    e.getDireccion(),
                                    e.getLatitud() != null ? e.getLatitud().doubleValue() : null,
                                    e.getLongitud() != null ? e.getLongitud().doubleValue() : null,
                                    e.getPublico() != null ? e.getPublico() : false,
                                    e.getActivo() != null ? e.getActivo() : false,
                                    e.getConfiguracionSla(),
                                    e.getFechaCreacion(),
                                    null
                            ))
                            .collect(Collectors.toList());
                    return new AlmaceneraConEstablecimientosDto(almacenera, establecimientosPorAlmacenera);
                })
                .collect(Collectors.toList());

        // Mapear operadores logísticos con clientes
        List<OperadorLogisticoConClientesDto> operadorLogisticoConClientes = operadoresLogisticos.stream()
                .map(operadorLogistico -> new OperadorLogisticoConClientesDto(operadorLogistico))
                .collect(Collectors.toList());

        // Retornar respuesta
        return new TransportistaInfoResponse(
                empresa.getId(),
                empresa.getNombre(),
                empresa.getTipoEmpresa(),
                empresa.getRuc(),
                camiones,
                carretas,
                choferes,
                establecimientos,
                navieraConAlmaceneras,
                almaceneraConEstablecimientos,
                operadorLogisticoConClientes
        );
    }
}