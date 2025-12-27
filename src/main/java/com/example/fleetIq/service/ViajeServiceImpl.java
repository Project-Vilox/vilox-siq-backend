package com.example.fleetIq.service;

import com.example.fleetIq.dto.EstablecimientoDto;
import com.example.fleetIq.dto.TramoDto;
import com.example.fleetIq.dto.ViajeDto;
import com.example.fleetIq.dto.ViajeResumenDto;
import com.example.fleetIq.model.*;
import com.example.fleetIq.repository.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ViajeServiceImpl implements ViajeService {

  @Autowired
  private ViajeRepository viajeRepository;

  @Autowired
  private TramoService tramoService;

  @Autowired
  private EstablecimientoRepository establecimientoRepository;

  @Autowired
  private TrackRepository trackRepository;

  // ════════════════════════════════════════════════════════════════════════════
  // 🔧 MÉTODO PRINCIPAL - SIN @Transactional
  // ════════════════════════════════════════════════════════════════════════════

  @Override
  public ViajeDto obtenerViajePorId(String id) {
    try {
      System.out.println("🔍 Iniciando carga de viaje: " + id);

      // 1️⃣ Cargar datos básicos (con su propia transacción)
      ViajeDto dto = cargarDatosBasicosTransaccional(id);

      if (dto == null) {
        throw new RuntimeException("Viaje no encontrado: " + id);
      }

      System.out.println("✅ Datos básicos cargados: " + dto.getCodigoViaje());
      System.out.println("📦 Tramos encontrados: " + (dto.getTramos() != null ? dto.getTramos().size() : 0));

      // 2️⃣ Enriquecer con ETA/Avance (SIN transacción activa)
      if (dto.getTramos() != null && !dto.getTramos().isEmpty()) {
        System.out.println("🔄 Iniciando enriquecimiento de tramos...");
        enriquecerTramosConCalculos(dto, id);
        System.out.println("✅ Enriquecimiento completado");
      }

      return dto;

    } catch (Exception e) {
      System.err.println("❌ Error en obtenerViajePorId: " + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException("Error al obtener viaje: " + e.getMessage(), e);
    }
  }

  /**
   * ✅ Método TRANSACCIONAL con configuración optimizada para producción
   */
  @Transactional(readOnly = true, timeout = 120, // 2 minutos de timeout
      isolation = Isolation.READ_COMMITTED // Nivel de aislamiento más permisivo
  )
  private ViajeDto cargarDatosBasicosTransaccional(String id) {
    System.out.println("🚀 [" + Thread.currentThread().getName() + "] Iniciando carga de viaje: " + id);
    long startTime = System.currentTimeMillis();

    Viaje viaje = viajeRepository.findByIdWithTramos(id).orElse(null);

    if (viaje == null) {
      System.out.println("❌ Viaje no encontrado: " + id);
      return null;
    }

    System.out.println("⏱️ Query principal ejecutado en: " + (System.currentTimeMillis() - startTime) + "ms");
    System.out.println("🔄 Iniciando inicialización de relaciones lazy...");

    try {
      // 1. Inicializar tramos y sus establecimientos
      if (viaje.getTramos() != null) {
        long tramoStart = System.currentTimeMillis();
        Hibernate.initialize(viaje.getTramos());
        System.out.println("✅ Tramos inicializados: " + viaje.getTramos().size() + " ("
            + (System.currentTimeMillis() - tramoStart) + "ms)");

        viaje.getTramos().forEach(tramo -> {
          Hibernate.initialize(tramo.getEstablecimientoOrigen());
          Hibernate.initialize(tramo.getEstablecimientoDestino());
        });
        System.out.println(
            "✅ Establecimientos de tramos inicializados (" + (System.currentTimeMillis() - tramoStart) + "ms)");
      }

      // 2. Inicializar conductor y TODAS sus relaciones
      if (viaje.getConductor() != null) {
        long conductorStart = System.currentTimeMillis();
        Conductor conductor = viaje.getConductor();
        Hibernate.initialize(conductor);
        System.out.println("✅ Conductor inicializado: " + conductor.getId() + " ("
            + (System.currentTimeMillis() - conductorStart) + "ms)");

        // ⚠️ CRÍTICO: Forzar la carga con size() y manejar posibles errores
        try {
          if (conductor.getConductorEmpresas() != null) {
            // Forzar carga inmediata
            int size = conductor.getConductorEmpresas().size();
            System.out.println("✅ ConductorEmpresas cargado: " + size + " registros ("
                + (System.currentTimeMillis() - conductorStart) + "ms)");

            // Inicializar empresas
            conductor.getConductorEmpresas().forEach(ce -> {
              if (ce.getEmpresa() != null) {
                Hibernate.initialize(ce.getEmpresa());
              }
            });
            System.out.println(
                "✅ Empresas del conductor inicializadas (" + (System.currentTimeMillis() - conductorStart) + "ms)");
          } else {
            System.out.println("⚠️ ConductorEmpresas es null para conductor: " + conductor.getId());
          }
        } catch (Exception e) {
          System.err.println("❌ Error crítico inicializando conductorEmpresas: " + e.getMessage());
          System.err.println("   Tipo de error: " + e.getClass().getName());
          System.err.println("   Causa raíz: " + (e.getCause() != null ? e.getCause().getMessage() : "N/A"));
          e.printStackTrace();
          // En producción, intentamos continuar sin esta información
        }
      } else {
        System.out.println("⚠️ Viaje sin conductor asignado");
      }

      // 3. Inicializar vehículo y carreta
      if (viaje.getVehiculo() != null) {
        long vehiculoStart = System.currentTimeMillis();
        Hibernate.initialize(viaje.getVehiculo());
        if (viaje.getVehiculo().getEmpresa() != null) {
          Hibernate.initialize(viaje.getVehiculo().getEmpresa());
        }
        System.out.println("✅ Vehículo inicializado (" + (System.currentTimeMillis() - vehiculoStart) + "ms)");
      }

      if (viaje.getCarreta() != null) {
        long carretaStart = System.currentTimeMillis();
        Hibernate.initialize(viaje.getCarreta());
        if (viaje.getCarreta().getEmpresa() != null) {
          Hibernate.initialize(viaje.getCarreta().getEmpresa());
        }
        System.out.println("✅ Carreta inicializada (" + (System.currentTimeMillis() - carretaStart) + "ms)");
      }

      // 4. Inicializar empresas del viaje
      long empresasStart = System.currentTimeMillis();
      if (viaje.getEmpresaTransportista() != null) {
        Hibernate.initialize(viaje.getEmpresaTransportista());
      }
      if (viaje.getEmpresaOperador() != null) {
        Hibernate.initialize(viaje.getEmpresaOperador());
      }
      if (viaje.getEmpresaCliente() != null) {
        Hibernate.initialize(viaje.getEmpresaCliente());
      }
      if (viaje.getEmpresaNaviera() != null) {
        Hibernate.initialize(viaje.getEmpresaNaviera());
      }
      System.out.println("✅ Empresas del viaje inicializadas (" + (System.currentTimeMillis() - empresasStart) + "ms)");

      long totalTime = System.currentTimeMillis() - startTime;
      System.out.println("✅ ✨ TODAS las relaciones inicializadas exitosamente en " + totalTime + "ms");

      if (totalTime > 5000) {
        System.out.println("⚠️ ADVERTENCIA: La carga tomó más de 5 segundos. Considerar optimización.");
      }

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      System.err.println("❌ ERROR FATAL en inicialización después de " + totalTime + "ms");
      System.err.println("   Mensaje: " + e.getMessage());
      System.err.println("   Tipo: " + e.getClass().getName());
      if (e.getCause() != null) {
        System.err.println("   Causa: " + e.getCause().getMessage());
      }
      e.printStackTrace();
      throw new RuntimeException("Error al inicializar relaciones del viaje: " + e.getMessage(), e);
    }

    return convertToDto(viaje);
  }

  /**
   * ✅ Método SIN TRANSACCIÓN: Enriquece con cálculos externos
   * NO tiene @Transactional, por lo que no hay contexto transaccional activo
   */
  private void enriquecerTramosConCalculos(ViajeDto dto, String viajeId) {
    System.out.println("📊 Procesando " + dto.getTramos().size() + " tramos para cálculos...");

    List<Tramo> tramosParaCalculo = cargarTramosParaCalculo(viajeId);

    for (int i = 0; i < dto.getTramos().size(); i++) {
      try {
        TramoDto tramoDto = dto.getTramos().get(i);

        // 🚀 OPTIMIZACIÓN: Solo procesar si está "en_curso"
        if (!"en_curso".equals(tramoDto.getEstado())) {
          if ("completado".equals(tramoDto.getEstado())) {
            tramoDto.setEta("--:--");
            tramoDto.setAvance(100.0);
          } else {
            tramoDto.setAvance(0.0);
          }
          continue;
        }

        Tramo tramo = tramosParaCalculo.stream()
            .filter(t -> t.getId().equals(tramoDto.getId()))
            .findFirst()
            .orElse(null);

        if (tramo != null) {
          System.out.println("   🔄 Calculando ETA/Avance REAL para tramo activo " + (i + 1));
          tramoService.enriquecerConEtaYAvance(tramoDto, tramo);
        }
      } catch (Exception e) {
        System.err.println("   ⚠️ Error en tramo " + (i + 1) + ": " + e.getMessage());
      }
    }
  }

  /**
   * 🆕 Carga tramos con nueva transacción independiente
   */
  @Transactional(readOnly = true)
  private List<Tramo> cargarTramosParaCalculo(String viajeId) {
    return viajeRepository.findByIdWithTramos(viajeId)
        .map(viaje -> {
          List<Tramo> tramos = viaje.getTramos();
          tramos.forEach(tramo -> {
            Hibernate.initialize(tramo.getViaje());
            Hibernate.initialize(tramo.getViaje().getVehiculo());
            Hibernate.initialize(tramo.getEstablecimientoOrigen());
            Hibernate.initialize(tramo.getEstablecimientoDestino());
          });
          return tramos;
        })
        .orElseGet(List::of);
  }

  // ════════════════════════════════════════════════════════════════════════════
  // 📋 OTROS MÉTODOS (Sin cambios)
  // ════════════════════════════════════════════════════════════════════════════

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
  @Transactional(readOnly = true)
  public List<ViajeDto> listarViajesPorEmpresa(String empresaId) {
    return viajeRepository.findByEmpresaTransportistaId(empresaId).stream()
        .map(this::convertToDto)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<ViajeResumenDto> listarViajesResumenPorEmpresa(String empresaId) {
    return viajeRepository.findViajesResumenByEmpresaTransportistaId(empresaId);
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

  // ════════════════════════════════════════════════════════════════════════════
  // 🔄 MÉTODOS DE MAPEO (Sin cambios)
  // ════════════════════════════════════════════════════════════════════════════

  private ViajeDto convertToDto(Viaje viaje) {
    ViajeDto dto = new ViajeDto();
    dto.setId(viaje.getId());
    dto.setCodigoViaje(viaje.getCodigoViaje());
    dto.setContenedor(viaje.getContenedor());
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

    mapearEmpresas(viaje, dto);

    if (viaje.getVehiculo() != null) {
      dto.setVehiculo(mapearVehiculo(viaje.getVehiculo()));
    }

    if (viaje.getCarreta() != null) {
      dto.setCarreta(mapearCarreta(viaje.getCarreta()));
    }

    if (viaje.getConductor() != null) {
      dto.setConductor(mapearConductor(viaje.getConductor()));
    }

    if (viaje.getTramos() != null) {
      dto.setTramos(viaje.getTramos().stream()
          .map(tramo -> {
            TramoDto tDto = tramoService.convertToDto(tramo);

            // MAPEO DE ORIGEN
            if (tramo.getEstablecimientoOrigen() != null) {
              Establecimiento est = tramo.getEstablecimientoOrigen();
              EstablecimientoDto eDto = new EstablecimientoDto(
                  est.getId(), est.getEmpresaId(),
                  est.getNombre(), est.getTipo(), est.getDireccion(),
                  est.getLatitud(), est.getLongitud(),
                  est.getPublico(), est.getActivo(), null, est.getFechaCreacion());
              tDto.setEstablecimientoOrigen(eDto);
            }

            // MAPEO DE DESTINO
            if (tramo.getEstablecimientoDestino() != null) {
              Establecimiento estDest = tramo.getEstablecimientoDestino();
              EstablecimientoDto eDtoDest = new EstablecimientoDto(
                  estDest.getId(), estDest.getEmpresaId(),
                  estDest.getNombre(), estDest.getTipo(), estDest.getDireccion(),
                  estDest.getLatitud(), estDest.getLongitud(),
                  estDest.getPublico(), estDest.getActivo(), null, estDest.getFechaCreacion());
              tDto.setEstablecimientoDestino(eDtoDest);
            }

            return tDto;
          })
          .collect(Collectors.toList()));
    }
    return dto;
  }

  private void mapearEmpresas(Viaje viaje, ViajeDto dto) {
    if (viaje.getEmpresaTransportista() != null) {
      dto.setEmpresaTransportista(mapearEmpresa(viaje.getEmpresaTransportista()));
    }
    if (viaje.getEmpresaOperador() != null) {
      dto.setEmpresaOperador(mapearEmpresa(viaje.getEmpresaOperador()));
    }
    if (viaje.getEmpresaCliente() != null) {
      dto.setEmpresaCliente(mapearEmpresa(viaje.getEmpresaCliente()));
    }
    if (viaje.getEmpresaNaviera() != null) {
      dto.setEmpresaNaviera(mapearEmpresa(viaje.getEmpresaNaviera()));
    }
  }

  private ViajeDto.EmpresaDto mapearEmpresa(Empresa empresa) {
    ViajeDto.EmpresaDto dto = new ViajeDto.EmpresaDto();
    dto.setId(empresa.getId());
    dto.setNombre(empresa.getNombre());
    dto.setTipoEmpresa(empresa.getTipoEmpresa());
    dto.setRuc(empresa.getRuc());
    dto.setDireccion(empresa.getDireccion());
    dto.setTelefono(empresa.getTelefono());
    dto.setEmail(empresa.getEmail());
    dto.setConfiguracionAlertas(empresa.getConfiguracionAlertas());
    dto.setConfiguracionDashboard(empresa.getConfiguracionDashboard());
    dto.setActivo(empresa.getActivo());
    dto.setFechaCreacion(empresa.getFechaCreacion());
    dto.setFechaActualizacion(empresa.getFechaActualizacion());
    return dto;
  }

  private ViajeDto.VehiculoDto mapearVehiculo(Vehiculo vehiculo) {
    ViajeDto.VehiculoDto dto = new ViajeDto.VehiculoDto();
    dto.setId(vehiculo.getId());
    dto.setEmpresaId(vehiculo.getEmpresa() != null ? vehiculo.getEmpresa().getId() : null);
    dto.setPlaca(vehiculo.getPlaca());
    dto.setImei(vehiculo.getImei());
    dto.setMarca(vehiculo.getMarca());
    dto.setModelo(vehiculo.getModelo());
    dto.setAno(vehiculo.getAno());
    dto.setTipoVehiculo(vehiculo.getTipoVehiculo().name());
    dto.setCapacidadToneladas(vehiculo.getCapacidadToneladas());
    dto.setEstado(vehiculo.getEstado().name());
    dto.setActivo(vehiculo.getActivo());
    dto.setFechaCreacion(vehiculo.getFechaCreacion());
    return dto;
  }

  private ViajeDto.CarretaDto mapearCarreta(Carreta carreta) {
    ViajeDto.CarretaDto dto = new ViajeDto.CarretaDto();
    dto.setId(carreta.getId());
    dto.setEmpresaId(carreta.getEmpresa() != null ? carreta.getEmpresa().getId() : null);
    dto.setPlaca(carreta.getPlaca());
    dto.setImei(carreta.getImei());
    dto.setMarca(carreta.getMarca());
    dto.setModelo(carreta.getModelo());
    dto.setAno(carreta.getAño());
    dto.setTipoVehiculo(carreta.getTipoVehiculo().name());
    dto.setCapacidadToneladas(carreta.getCapacidadToneladas());
    dto.setEstado(carreta.getEstado().name());
    dto.setActivo(carreta.getActivo());
    dto.setFechaCreacion(carreta.getFechaCreacion());
    return dto;
  }

  /**
   * ✅ MÉTODO CORREGIDO: Manejo defensivo de colecciones lazy
   */
  private ViajeDto.ConductorDto mapearConductor(Conductor conductor) {
    ViajeDto.ConductorDto dto = new ViajeDto.ConductorDto();
    dto.setId(conductor.getId());
    dto.setDni(conductor.getDni());
    dto.setNombre(conductor.getNombre());
    dto.setApellidos(conductor.getApellidos());
    dto.setTelefono(conductor.getTelefono());
    dto.setEmail(conductor.getEmail());
    dto.setLicenciaNumero(conductor.getLicenciaNumero());
    dto.setLicenciaCategoria(conductor.getLicenciaCategoria());
    dto.setLicenciaVencimiento(conductor.getLicenciaVencimiento());
    dto.setActivo(conductor.getActivo());
    dto.setFechaCreacion(conductor.getFechaCreacion());

    // ✅ MANEJO DEFENSIVO: Verificar múltiples condiciones antes de acceder
    try {
      System.out.println("🔍 Intentando mapear conductor: " + conductor.getId());

      if (conductor.getConductorEmpresas() != null) {
        System.out.println("  📋 ConductorEmpresas no es null, obteniendo tamaño...");

        // Verificar que no esté vacío
        if (!conductor.getConductorEmpresas().isEmpty()) {
          System.out.println("  ✅ ConductorEmpresas tiene " + conductor.getConductorEmpresas().size() + " elementos");

          var ce = conductor.getConductorEmpresas().get(0);

          if (ce != null) {
            System.out.println("  ✅ Primer elemento de ConductorEmpresas obtenido");

            if (ce.getEmpresa() != null) {
              dto.setEmpresaId(ce.getEmpresa().getId());
              System.out.println("  ✅ EmpresaId mapeado: " + ce.getEmpresa().getId());
            }

            if (ce.getFechaInicio() != null) {
              dto.setFechaInicio(ce.getFechaInicio().atStartOfDay());
              System.out.println("  ✅ FechaInicio mapeada");
            }

            if (ce.getFechaFin() != null) {
              dto.setFechaFin(ce.getFechaFin().atStartOfDay());
              System.out.println("  ✅ FechaFin mapeada");
            }
          } else {
            System.out.println("  ⚠️ Primer elemento de ConductorEmpresas es null");
          }
        } else {
          System.out.println("  ⚠️ ConductorEmpresas está vacío");
        }
      } else {
        System.out.println("  ⚠️ ConductorEmpresas es null para conductor: " + conductor.getId());
      }
    } catch (Exception e) {
      System.err.println("  ❌ Error al mapear conductor empresas: " + e.getMessage());
      e.printStackTrace();
      // No lanzamos la excepción, solo registramos el error
      // El DTO se retorna con los campos de empresa sin llenar
    }

    System.out.println("✅ Conductor mapeado exitosamente: " + conductor.getId());
    return dto;
  }
}