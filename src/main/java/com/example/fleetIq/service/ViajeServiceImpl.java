package com.example.fleetIq.service;

import com.example.fleetIq.dto.ViajeDto;
import com.example.fleetIq.model.Tramo;
import com.example.fleetIq.model.Viaje;
import com.example.fleetIq.repository.ViajeRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ViajeServiceImpl implements ViajeService {

    @Autowired
    private ViajeRepository viajeRepository;

    @Override
    public Viaje guardarViaje(Viaje viaje) {
        if (viaje.getFechaCreacion() == null) {
            viaje.setFechaCreacion(LocalDateTime.now());
        }
        viaje.setFechaActualizacion(LocalDateTime.now());
        return viajeRepository.save(viaje);
    }

    @Override
    public List<ViajeDto> listarTodosLosViajes() {
        return viajeRepository.findAllWithRelations().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Viaje> listarTodosLosViajesPaginado(Pageable pageable) {
        return viajeRepository.findAll(pageable);
    }

    @Override
    public List<ViajeDto> listarViajesPorCodigo(String codigoViaje) {
        return viajeRepository.findByCodigoViaje(codigoViaje).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ViajeDto> listarViajesPorEmpresa(String empresaId) {
        return viajeRepository.findByEmpresaTransportistaId(empresaId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ViajeDto> listarViajesPorOperador(String operadorId) {
        return viajeRepository.findByEmpresaOperadorId(operadorId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ViajeDto> listarViajesPorCliente(String clienteId) {
        return viajeRepository.findByEmpresaClienteId(clienteId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ViajeDto> listarViajesId(String empresaId) {
        return viajeRepository.findByEmpresaId(empresaId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private ViajeDto convertToDto(Viaje viaje) {
        ViajeDto dto = new ViajeDto();
        dto.setId(viaje.getId());
        dto.setCodigoViaje(viaje.getCodigoViaje());
        dto.setTipoOperacion(viaje.getTipoOperacion());
        dto.setDocumentoEmbarque(viaje.getDocumentoEmbarque());
        dto.setEstado(viaje.getEstado());
        dto.setFechaInicioProgramada(viaje.getFechaInicioProgramada());
        dto.setFechaFinProgramada(viaje.getFechaFinProgramada());
        dto.setFechaInicioReal(viaje.getFechaInicioReal());
        dto.setFechaFinReal(viaje.getFechaFinReal());
        dto.setObservaciones(viaje.getObservaciones());
        dto.setConfiguracionAlertas(viaje.getConfiguracionAlertas());
        dto.setFechaCreacion(viaje.getFechaCreacion());
        dto.setFechaActualizacion(viaje.getFechaActualizacion());

        // Mapeo de EmpresaTransportista
        if (viaje.getEmpresaTransportista() != null) {
            ViajeDto.EmpresaDto empresaTransportistaDto = new ViajeDto.EmpresaDto();
            empresaTransportistaDto.setId(viaje.getEmpresaTransportista().getId());
            empresaTransportistaDto.setNombre(viaje.getEmpresaTransportista().getNombre());
            empresaTransportistaDto.setTipoEmpresa(viaje.getEmpresaTransportista().getTipoEmpresa());
            empresaTransportistaDto.setRuc(viaje.getEmpresaTransportista().getRuc());
            empresaTransportistaDto.setDireccion(viaje.getEmpresaTransportista().getDireccion());
            empresaTransportistaDto.setTelefono(viaje.getEmpresaTransportista().getTelefono());
            empresaTransportistaDto.setEmail(viaje.getEmpresaTransportista().getEmail());
            empresaTransportistaDto.setConfiguracionAlertas(viaje.getEmpresaTransportista().getConfiguracionAlertas());
            empresaTransportistaDto.setConfiguracionDashboard(viaje.getEmpresaTransportista().getConfiguracionDashboard());
            empresaTransportistaDto.setActivo(viaje.getEmpresaTransportista().getActivo());
            empresaTransportistaDto.setFechaCreacion(viaje.getEmpresaTransportista().getFechaCreacion());
            empresaTransportistaDto.setFechaActualizacion(viaje.getEmpresaTransportista().getFechaActualizacion());
            //empresaTransportistaDto.setEmpresaAdministradoraId(viaje.getEmpresaTransportista().getEmpresaAdministradora() != null ? viaje.getEmpresaTransportista().getEmpresaAdministradora().getId() : null);
            dto.setEmpresaTransportista(empresaTransportistaDto);
        }

        // Mapeo de EmpresaOperador
        if (viaje.getEmpresaOperador() != null) {
            ViajeDto.EmpresaDto empresaOperadorDto = new ViajeDto.EmpresaDto();
            empresaOperadorDto.setId(viaje.getEmpresaOperador().getId());
            empresaOperadorDto.setNombre(viaje.getEmpresaOperador().getNombre());
            empresaOperadorDto.setTipoEmpresa(viaje.getEmpresaOperador().getTipoEmpresa());
            empresaOperadorDto.setRuc(viaje.getEmpresaOperador().getRuc());
            empresaOperadorDto.setDireccion(viaje.getEmpresaOperador().getDireccion());
            empresaOperadorDto.setTelefono(viaje.getEmpresaOperador().getTelefono());
            empresaOperadorDto.setEmail(viaje.getEmpresaOperador().getEmail());
            empresaOperadorDto.setConfiguracionAlertas(viaje.getEmpresaOperador().getConfiguracionAlertas());
            empresaOperadorDto.setConfiguracionDashboard(viaje.getEmpresaOperador().getConfiguracionDashboard());
            empresaOperadorDto.setActivo(viaje.getEmpresaOperador().getActivo());
            empresaOperadorDto.setFechaCreacion(viaje.getEmpresaOperador().getFechaCreacion());
            empresaOperadorDto.setFechaActualizacion(viaje.getEmpresaOperador().getFechaActualizacion());
            //empresaOperadorDto.setEmpresaAdministradoraId(viaje.getEmpresaOperador().getEmpresaAdministradora() != null ? viaje.getEmpresaOperador().getEmpresaAdministradora().getId() : null);
            dto.setEmpresaOperador(empresaOperadorDto);
        }

        // Mapeo de EmpresaCliente
        if (viaje.getEmpresaCliente() != null) {
            ViajeDto.EmpresaDto empresaClienteDto = new ViajeDto.EmpresaDto();
            empresaClienteDto.setId(viaje.getEmpresaCliente().getId());
            empresaClienteDto.setNombre(viaje.getEmpresaCliente().getNombre());
            empresaClienteDto.setTipoEmpresa(viaje.getEmpresaCliente().getTipoEmpresa());
            empresaClienteDto.setRuc(viaje.getEmpresaCliente().getRuc());
            empresaClienteDto.setDireccion(viaje.getEmpresaCliente().getDireccion());
            empresaClienteDto.setTelefono(viaje.getEmpresaCliente().getTelefono());
            empresaClienteDto.setEmail(viaje.getEmpresaCliente().getEmail());
            empresaClienteDto.setConfiguracionAlertas(viaje.getEmpresaCliente().getConfiguracionAlertas());
            empresaClienteDto.setConfiguracionDashboard(viaje.getEmpresaCliente().getConfiguracionDashboard());
            empresaClienteDto.setActivo(viaje.getEmpresaCliente().getActivo());
            empresaClienteDto.setFechaCreacion(viaje.getEmpresaCliente().getFechaCreacion());
            empresaClienteDto.setFechaActualizacion(viaje.getEmpresaCliente().getFechaActualizacion());
            //empresaClienteDto.setEmpresaAdministradoraId(viaje.getEmpresaCliente().getEmpresaAdministradora() != null ? viaje.getEmpresaCliente().getEmpresaAdministradora().getId() : null);
            dto.setEmpresaCliente(empresaClienteDto);
        }

        // Mapeo de EmpresaNaviera (nueva relación)
        if (viaje.getEmpresaNaviera() != null) {
            ViajeDto.EmpresaDto empresaNavieraDto = new ViajeDto.EmpresaDto();
            empresaNavieraDto.setId(viaje.getEmpresaNaviera().getId());
            empresaNavieraDto.setNombre(viaje.getEmpresaNaviera().getNombre());
            empresaNavieraDto.setTipoEmpresa(viaje.getEmpresaNaviera().getTipoEmpresa());
            empresaNavieraDto.setRuc(viaje.getEmpresaNaviera().getRuc());
            empresaNavieraDto.setDireccion(viaje.getEmpresaNaviera().getDireccion());
            empresaNavieraDto.setTelefono(viaje.getEmpresaNaviera().getTelefono());
            empresaNavieraDto.setEmail(viaje.getEmpresaNaviera().getEmail());
            empresaNavieraDto.setConfiguracionAlertas(viaje.getEmpresaNaviera().getConfiguracionAlertas());
            empresaNavieraDto.setConfiguracionDashboard(viaje.getEmpresaNaviera().getConfiguracionDashboard());
            empresaNavieraDto.setActivo(viaje.getEmpresaNaviera().getActivo());
            empresaNavieraDto.setFechaCreacion(viaje.getEmpresaNaviera().getFechaCreacion());
            empresaNavieraDto.setFechaActualizacion(viaje.getEmpresaNaviera().getFechaActualizacion());
          //  empresaNavieraDto.setEmpresaAdministradoraId(viaje.getEmpresaNaviera().getEmpresaAdministradora() != null ? viaje.getEmpresaNaviera().getEmpresaAdministradora().getId() : null);
            dto.setEmpresaNaviera(empresaNavieraDto); // Añadido al DTO
        }

        // Mapeo de Vehiculo
        if (viaje.getVehiculo() != null) {
            ViajeDto.VehiculoDto vehiculoDto = new ViajeDto.VehiculoDto();
            vehiculoDto.setId(viaje.getVehiculo().getId());
            vehiculoDto.setEmpresaId(viaje.getVehiculo().getEmpresa() != null ? viaje.getVehiculo().getEmpresa().getId() : null);
            vehiculoDto.setPlaca(viaje.getVehiculo().getPlaca());
            vehiculoDto.setImei(viaje.getVehiculo().getImei());
            vehiculoDto.setMarca(viaje.getVehiculo().getMarca());
            vehiculoDto.setModelo(viaje.getVehiculo().getModelo());
            vehiculoDto.setAno(viaje.getVehiculo().getAno());
            vehiculoDto.setTipoVehiculo(viaje.getVehiculo().getTipoVehiculo().name());
            vehiculoDto.setCapacidadToneladas(viaje.getVehiculo().getCapacidadToneladas());
            vehiculoDto.setEstado(viaje.getVehiculo().getEstado().name());
            vehiculoDto.setActivo(viaje.getVehiculo().getActivo());
            vehiculoDto.setFechaCreacion(viaje.getVehiculo().getFechaCreacion());
            dto.setVehiculo(vehiculoDto);
        }

        // Mapeo de Carreta
        if (viaje.getCarreta() != null) {
            ViajeDto.CarretaDto carretaDto = new ViajeDto.CarretaDto();
            carretaDto.setId(viaje.getCarreta().getId());
            carretaDto.setEmpresaId(viaje.getCarreta().getEmpresa() != null ? viaje.getCarreta().getEmpresa().getId() : null);
            carretaDto.setPlaca(viaje.getCarreta().getPlaca());
            carretaDto.setImei(viaje.getCarreta().getImei());
            carretaDto.setMarca(viaje.getCarreta().getMarca());
            carretaDto.setModelo(viaje.getCarreta().getModelo());
            carretaDto.setAno(viaje.getCarreta().getAño());
            carretaDto.setTipoVehiculo(viaje.getCarreta().getTipoVehiculo().name());
            carretaDto.setCapacidadToneladas(viaje.getCarreta().getCapacidadToneladas());
            carretaDto.setEstado(viaje.getCarreta().getEstado().name());
            carretaDto.setActivo(viaje.getCarreta().getActivo());
            carretaDto.setFechaCreacion(viaje.getCarreta().getFechaCreacion());
            dto.setCarreta(carretaDto);
        }

        // Mapeo de Conductor
        if (viaje.getConductor() != null) {
            ViajeDto.ConductorDto conductorDto = new ViajeDto.ConductorDto();
            conductorDto.setId(viaje.getConductor().getId());
            conductorDto.setDni(viaje.getConductor().getDni());
            conductorDto.setNombre(viaje.getConductor().getNombre());
            conductorDto.setApellidos(viaje.getConductor().getApellidos());
            conductorDto.setTelefono(viaje.getConductor().getTelefono());
            conductorDto.setEmail(viaje.getConductor().getEmail());
            conductorDto.setLicenciaNumero(viaje.getConductor().getLicenciaNumero());
            conductorDto.setLicenciaCategoria(viaje.getConductor().getLicenciaCategoria());
            conductorDto.setLicenciaVencimiento(viaje.getConductor().getLicenciaVencimiento());
            conductorDto.setActivo(viaje.getConductor().getActivo());
            conductorDto.setFechaCreacion(viaje.getConductor().getFechaCreacion());

            // Initialize conductorEmpresas to avoid LazyInitializationException
            Hibernate.initialize(viaje.getConductor().getConductorEmpresas());
            if (viaje.getConductor().getConductorEmpresas() != null && !viaje.getConductor().getConductorEmpresas().isEmpty()) {
                conductorDto.setEmpresaId(viaje.getConductor().getConductorEmpresas().get(0).getEmpresa() != null ? viaje.getConductor().getConductorEmpresas().get(0).getEmpresa().getId() : null);
                conductorDto.setFechaInicio(viaje.getConductor().getConductorEmpresas().get(0).getFechaInicio() != null ? viaje.getConductor().getConductorEmpresas().get(0).getFechaInicio().atStartOfDay() : null);
                conductorDto.setFechaFin(viaje.getConductor().getConductorEmpresas().get(0).getFechaFin() != null ? viaje.getConductor().getConductorEmpresas().get(0).getFechaFin().atStartOfDay() : null);
            }
            dto.setConductor(conductorDto);
        }

        // Mapeo de Tramos
        if (viaje.getTramos() != null) {
            List<ViajeDto.TramoDto> tramoDtos = viaje.getTramos().stream().map(tramo -> {
                ViajeDto.TramoDto tramoDto = new ViajeDto.TramoDto();
                tramoDto.setId(tramo.getId());
                tramoDto.setViajeId(tramo.getViaje() != null ? tramo.getViaje().getId() : null);
                tramoDto.setOrden(tramo.getOrden());

                // Mapeo de EstablecimientoOrigen
                if (tramo.getEstablecimientoOrigen() != null) {
                    ViajeDto.EstablecimientoDto establecimientoOrigenDto = new ViajeDto.EstablecimientoDto();
                    establecimientoOrigenDto.setId(tramo.getEstablecimientoOrigen().getId());
                    establecimientoOrigenDto.setEmpresaId(tramo.getEstablecimientoOrigen().getEmpresaId());
                    establecimientoOrigenDto.setNombre(tramo.getEstablecimientoOrigen().getNombre());
                    establecimientoOrigenDto.setTipo(tramo.getEstablecimientoOrigen().getTipo());
                    establecimientoOrigenDto.setDireccion(tramo.getEstablecimientoOrigen().getDireccion());
                    establecimientoOrigenDto.setLatitud(tramo.getEstablecimientoOrigen().getLatitud());
                    establecimientoOrigenDto.setLongitud(tramo.getEstablecimientoOrigen().getLongitud());
                    establecimientoOrigenDto.setPublico(tramo.getEstablecimientoOrigen().getPublico());
                    establecimientoOrigenDto.setActivo(tramo.getEstablecimientoOrigen().getActivo());
                    establecimientoOrigenDto.setConfiguracionSla(tramo.getEstablecimientoOrigen().getConfiguracionSla());
                    establecimientoOrigenDto.setFechaCreacion(tramo.getEstablecimientoOrigen().getFechaCreacion());
                    tramoDto.setEstablecimientoOrigen(establecimientoOrigenDto);
                }

                // Mapeo de EstablecimientoDestino
                if (tramo.getEstablecimientoDestino() != null) {
                    ViajeDto.EstablecimientoDto establecimientoDestinoDto = new ViajeDto.EstablecimientoDto();
                    establecimientoDestinoDto.setId(tramo.getEstablecimientoDestino().getId());
                    establecimientoDestinoDto.setEmpresaId(tramo.getEstablecimientoDestino().getEmpresaId());
                    establecimientoDestinoDto.setNombre(tramo.getEstablecimientoDestino().getNombre());
                    establecimientoDestinoDto.setTipo(tramo.getEstablecimientoDestino().getTipo());
                    establecimientoDestinoDto.setDireccion(tramo.getEstablecimientoDestino().getDireccion());
                    establecimientoDestinoDto.setLatitud(tramo.getEstablecimientoDestino().getLatitud());
                    establecimientoDestinoDto.setLongitud(tramo.getEstablecimientoDestino().getLongitud());
                    establecimientoDestinoDto.setPublico(tramo.getEstablecimientoDestino().getPublico());
                    establecimientoDestinoDto.setActivo(tramo.getEstablecimientoDestino().getActivo());
                    establecimientoDestinoDto.setConfiguracionSla(tramo.getEstablecimientoDestino().getConfiguracionSla());
                    establecimientoDestinoDto.setFechaCreacion(tramo.getEstablecimientoDestino().getFechaCreacion());
                    tramoDto.setEstablecimientoDestino(establecimientoDestinoDto);
                }

                tramoDto.setTipoActividad(tramo.getTipoActividad() != null ? tramo.getTipoActividad().name() : null);
                tramoDto.setDescripcion(tramo.getDescripcion());
                tramoDto.setHoraLlegadaProgramada(tramo.getHoraLlegadaProgramada());
                tramoDto.setHoraSalidaProgramada(tramo.getHoraSalidaProgramada());
                tramoDto.setHoraLlegadaReal(tramo.getHoraLlegadaReal());
                tramoDto.setHoraSalidaReal(tramo.getHoraSalidaReal());
                tramoDto.setEstado(tramo.getEstado() != null ? tramo.getEstado().name() : null);
                tramoDto.setSlaMinutos(tramo.getSlaMinutos());
                tramoDto.setObservaciones(tramo.getObservaciones());
                return tramoDto;
            }).collect(Collectors.toList());
            dto.setTramos(tramoDtos);
        }

        return dto;
    }
}