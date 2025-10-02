package com.example.fleetIq.service;

import com.example.fleetIq.dto.EmpresaDto;
import com.example.fleetIq.dto.OperadorLogisticoInfoResponse;
import com.example.fleetIq.model.Empresa;
import com.example.fleetIq.model.TransportistasDeOperadores;
import com.example.fleetIq.repository.EmpresaRepository;
import com.example.fleetIq.repository.TransportistasDeOperadoresRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OperadorLogisticoServiceImpl implements OperadorLogisticoService {

    private final EmpresaRepository empresaRepository;
    private final TransportistasDeOperadoresRepository transportistasDeOperadoresRepository;

    @Override
    @Transactional(readOnly = true)
    public OperadorLogisticoInfoResponse getOperadorLogisticoInfo(String operadorId) {
        // Obtener el operador logístico
        Optional<Empresa> operadorOptional = empresaRepository.findById(operadorId);
        if (!operadorOptional.isPresent() || !"OPERADOR_LOGISTICO".equals(operadorOptional.get().getTipoEmpresa())) {
            return null;
        }

        Empresa operador = operadorOptional.get();
        EmpresaDto operadorDto = convertToDto(operador);

        // Obtener los transportistas asociados
        List<TransportistasDeOperadores> relaciones = transportistasDeOperadoresRepository.findByEmpresaOperadorId(operadorId);

        List<EmpresaDto> transportistas = relaciones.stream()
                .map(relacion -> relacion.getEmpresaTransportista())
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return new OperadorLogisticoInfoResponse(operadorDto, transportistas);
    }

    private EmpresaDto convertToDto(Empresa empresa) {
        return new EmpresaDto(
                empresa.getId(),
                empresa.getNombre(),
                empresa.getTipoEmpresa(),
                empresa.getRuc(),
                empresa.getDireccion(),
                empresa.getTelefono(),
                empresa.getEmail(),
                empresa.getActivo(),
                empresa.getConfiguracionAlertas(),
                empresa.getConfiguracionDashboard(),
                empresa.getFechaCreacion(),
                empresa.getFechaActualizacion()
        );
    }
}