package com.example.fleetIq.service;

import com.example.fleetIq.dto.ViajeReporteDetalleDto;
import com.example.fleetIq.model.*;
import com.example.fleetIq.repository.*;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ViajeReportePdfService {

    private static final Logger logger = LoggerFactory.getLogger(ViajeReportePdfService.class);

    @Value("${app.brand-name}")
    private String appName;
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ViajeRepository viajeRepository;
    private final TramoRepository tramoRepository;
    private final EvidenciaViajeRepository evidenciaViajeRepository;
    private final TrackRepository trackRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Definición de sesiones de evidencias (igual que evidenciasSets en React)
    // ─────────────────────────────────────────────────────────────────────────
    private static class FotoSet {
        final String nombre;
        final boolean obligatorio;
        FotoSet(String n, boolean o) { this.nombre = n; this.obligatorio = o; }
        String nombre() { return nombre; }
        boolean obligatorio() { return obligatorio; }
    }

    private static class HitoSet {
        final String hito;
        final String nombreUi;
        final List<FotoSet> fotos;
        HitoSet(String h, String u, List<FotoSet> f) { this.hito = h; this.nombreUi = u; this.fotos = f; }
        String hito() { return hito; }
        String nombreUi() { return nombreUi; }
        List<FotoSet> fotos() { return fotos; }
    }

    private static List<FotoSet> fo(FotoSet... s) { return java.util.Arrays.asList(s); }
    private static FotoSet fp(String n, boolean o) { return new FotoSet(n, o); }

    private static final List<HitoSet> EVIDENCIAS_SETS;
    static {
        EVIDENCIAS_SETS = java.util.Arrays.asList(
            new HitoSet("PREVIO_SERVICIO", "Previo al servicio", fo(
                fp("Booking", true), fp("Carta de precinto", true), fp("Carta de temperatura", false)
            )),
            new HitoSet("LLEGADA_DEPOT", "Llegada al depot", fo(
                fp("Registro", false), fp("Foto exterior de depot", true)
            )),
            new HitoSet("SALIDA_DEPOT", "Salida depot", fo(
                fp("EIR", true), fp("Precinto de aduana", true), fp("Precinto de linea", true),
                fp("Puerta de contenedor", true), fp("Precinto de puerta", true),
                fp("Maquinaria contenedor", false), fp("Ventilas de reefer", false),
                fp("Thermoregistros", false), fp("Filtros", false),
                fp("Cortina", false), fp("Sensores", false)
            )),
            new HitoSet("INGRESO_SALIDA_COCHERA", "Ingreso - Salida cochera", fo(
                fp("Pernocte en cochera - Ingreso", true), fp("Precinto de puerta - Salida", true)
            )),
            new HitoSet("LLEGADA_PLANTA", "Llegada a planta", fo(
                fp("Precinto de puerta - Salida", true),
                fp("Foto exterior de planta", true),
                fp("Temperatura de reefer", false)
            )),
            new HitoSet("TERMINO_CARGA", "Termino de carga", fo(
                fp("Guia de remision remitente", true), fp("Guia transportista", true),
                fp("Pesos y medidas", true), fp("Puerta de contenedor", true),
                fp("Precinto de linea", true), fp("Precinto aduana", true),
                fp("Precinto SENASA-SANIPES", true),
                fp("Precinto exportador", false), fp("Temperatura reefer", false)
            )),
            new HitoSet("LLEGADA_PUERTO", "Llegada a puerto", fo(
                fp("Registro", false), fp("Foto exterior de puerto", true),
                fp("Puerta de contenedor", false), fp("Temperatura reefer", false)
            )),
            new HitoSet("SALIDA_PUERTO", "Salida de puerto", fo(
                fp("Ticket de balanza", true)
            ))
        );
    }

    /**
     * Genera el PDF del reporte de trazabilidad para un viaje
     */
    public byte[] generarReportePdf(String viajeId) throws Exception {
        logger.info("📄 Generando reporte PDF para viaje: {}", viajeId);

        // 1. Obtener datos del viaje
        Viaje viaje = viajeRepository.findById(viajeId)
                .orElseThrow(() -> new RuntimeException("Viaje no encontrado: " + viajeId));

        // 2. Consolidar datos
        ViajeReporteDetalleDto datos = consolidarDatos(viaje);

        // 3. Generar HTML
        String html = generarHtml(datos);

        // 4. Convertir HTML a PDF
        return htmlToPdf(html);
    }

    /**
     * Consolida todos los datos necesarios para el reporte
     */
    private ViajeReporteDetalleDto consolidarDatos(Viaje viaje) {
        ViajeReporteDetalleDto dto = new ViajeReporteDetalleDto();

        // Información básica
        dto.setViajeId(viaje.getId());
        dto.setCodigoViaje(viaje.getCodigoViaje());
        dto.setContenedor(viaje.getContenedor());
        dto.setDocumentoEmbarque(viaje.getDocumentoEmbarque());
        dto.setTipoOperacion(viaje.getTipoOperacion());
        dto.setEstado(viaje.getEstado());
        dto.setFechaInicioProgramada(viaje.getFechaInicioProgramada());
        dto.setFechaFinProgramada(viaje.getFechaFinProgramada());
        dto.setFechaInicioReal(viaje.getFechaInicioReal());
        dto.setFechaFinReal(viaje.getFechaFinReal());
        dto.setObservaciones(viaje.getObservaciones());

        // Actores
        if (viaje.getEmpresaCliente() != null) {
            ViajeReporteDetalleDto.EmpresaInfo cliente = new ViajeReporteDetalleDto.EmpresaInfo();
            cliente.setNombre(viaje.getEmpresaCliente().getNombre());
            cliente.setRuc(viaje.getEmpresaCliente().getRuc());
            cliente.setTipoEmpresa(viaje.getEmpresaCliente().getTipoEmpresa());
            dto.setCliente(cliente);
        }

        if (viaje.getEmpresaTransportista() != null) {
            ViajeReporteDetalleDto.EmpresaInfo transportista = new ViajeReporteDetalleDto.EmpresaInfo();
            transportista.setNombre(viaje.getEmpresaTransportista().getNombre());
            transportista.setRuc(viaje.getEmpresaTransportista().getRuc());
            transportista.setTipoEmpresa(viaje.getEmpresaTransportista().getTipoEmpresa());
            dto.setTransportista(transportista);
        }

        if (viaje.getEmpresaOperador() != null) {
            ViajeReporteDetalleDto.EmpresaInfo operador = new ViajeReporteDetalleDto.EmpresaInfo();
            operador.setNombre(viaje.getEmpresaOperador().getNombre());
            operador.setRuc(viaje.getEmpresaOperador().getRuc());
            operador.setTipoEmpresa(viaje.getEmpresaOperador().getTipoEmpresa());
            dto.setOperadorLogistico(operador);
        }

        if (viaje.getEmpresaNaviera() != null) {
            ViajeReporteDetalleDto.EmpresaInfo naviera = new ViajeReporteDetalleDto.EmpresaInfo();
            naviera.setNombre(viaje.getEmpresaNaviera().getNombre());
            naviera.setRuc(viaje.getEmpresaNaviera().getRuc());
            naviera.setTipoEmpresa(viaje.getEmpresaNaviera().getTipoEmpresa());
            dto.setNaviera(naviera);
        }

        // Recursos
        if (viaje.getConductor() != null) {
            ViajeReporteDetalleDto.ConductorInfo conductor = new ViajeReporteDetalleDto.ConductorInfo();
            conductor.setNombre(viaje.getConductor().getNombre());
            conductor.setLicencia(viaje.getConductor().getLicenciaNumero()); // Corrected method
            conductor.setTelefono(viaje.getConductor().getTelefono());
            dto.setConductor(conductor);
        }

        if (viaje.getVehiculo() != null) {
            ViajeReporteDetalleDto.VehiculoInfo vehiculo = new ViajeReporteDetalleDto.VehiculoInfo();
            vehiculo.setPlaca(viaje.getVehiculo().getPlaca());
            vehiculo.setMarca(viaje.getVehiculo().getMarca());
            vehiculo.setModelo(viaje.getVehiculo().getModelo());
            vehiculo.setTipoVehiculo(viaje.getVehiculo().getTipoVehiculo().name()); // Convert enum to string
            dto.setVehiculo(vehiculo);
        }

        if (viaje.getCarreta() != null) {
            ViajeReporteDetalleDto.VehiculoInfo carreta = new ViajeReporteDetalleDto.VehiculoInfo();
            // Confirmado: CAR-0005 está en el campo 'placa' de la carreta
            carreta.setPlaca(viaje.getCarreta().getPlaca());
            carreta.setMarca(viaje.getCarreta().getMarca());
            carreta.setModelo(viaje.getCarreta().getModelo());
            carreta.setTipoVehiculo(viaje.getCarreta().getTipoVehiculo().name());
            dto.setCarreta(carreta);
        }

        // Tramos - Using existing repository method
        List<Tramo> tramos = tramoRepository.findByViajeId(viaje.getId());
        List<ViajeReporteDetalleDto.TramoInfo> tramosInfo = tramos.stream().map(t -> {
            ViajeReporteDetalleDto.TramoInfo info = new ViajeReporteDetalleDto.TramoInfo();
            info.setNumero(t.getOrden() != null ? t.getOrden() : 0);
            if (t.getEstablecimientoOrigen() != null) {
                info.setNombreOrigen(t.getEstablecimientoOrigen().getNombre());
                info.setTipoOrigen(t.getEstablecimientoOrigen().getTipo());
            }
            if (t.getEstablecimientoDestino() != null) {
                info.setNombreDestino(t.getEstablecimientoDestino().getNombre());
                info.setTipoDestino(t.getEstablecimientoDestino().getTipo());
            }
            info.setFechaHoraCita1(t.getHoraLlegadaProgramada());
            info.setFechaHoraCita2(t.getHoraSalidaProgramada());
            info.setHoraInicioReal(t.getHoraLlegadaReal());
            info.setHoraFinReal(t.getHoraSalidaReal());
            info.setTardanzaCita1(t.getTardanzaCita1() != null ? t.getTardanzaCita1() : 0);
            info.setTardanzaCita2(t.getTardanzaCita2() != null ? t.getTardanzaCita2() : 0);
            info.setTiempoAtencionCita1(t.getTiempoAtencionCita1() != null ? t.getTiempoAtencionCita1() : 0);
            info.setTiempoAtencionCita2(t.getTiempoAtencionCita2() != null ? t.getTiempoAtencionCita2() : 0);
            info.setTiempoPermanenciaCita1(t.getTiempoPermanenciaCita1() != null ? t.getTiempoPermanenciaCita1() : 0);
            info.setTiempoPermanenciaCita2(t.getTiempoPermanenciaCita2() != null ? t.getTiempoPermanenciaCita2() : 0);
            info.setEstado(t.getEstado() != null ? t.getEstado().name() : "DESCONOCIDO");
            return info;
        }).collect(Collectors.toList());
        dto.setTramos(tramosInfo);

        // Paradas detectadas por GPS - FASE 2
        List<ViajeReporteDetalleDto.ParadaInfo> paradasDetectadas = detectarParadasGps(viaje);
        dto.setParadasDetectadas(paradasDetectadas);

        // Evidencias - obtener entidades completas con datos binarios
        List<EvidenciaViaje> evidencias = evidenciaViajeRepository.findByIdViaje(viaje.getId()).stream()
                .sorted((a, b) -> {
                    // Ordenar primero por hito, luego por secuencia
                    if (a.getHito() == null && b.getHito() == null)
                        return 0;
                    if (a.getHito() == null)
                        return 1;
                    if (b.getHito() == null)
                        return -1;

                    int hitoCompare = a.getHito().compareTo(b.getHito());
                    if (hitoCompare != 0)
                        return hitoCompare;

                    if (a.getSecuencia() == null && b.getSecuencia() == null)
                        return 0;
                    if (a.getSecuencia() == null)
                        return 1;
                    if (b.getSecuencia() == null)
                        return -1;

                    return Integer.compare(a.getSecuencia(), b.getSecuencia());
                })
                .collect(Collectors.toList());

        List<ViajeReporteDetalleDto.EvidenciaInfo> evidenciasInfo = evidencias.stream().map(e -> {
            ViajeReporteDetalleDto.EvidenciaInfo info = new ViajeReporteDetalleDto.EvidenciaInfo();
            info.setHito(e.getHito() != null ? e.getHito().toString() : "SIN_HITO");
            info.setSecuencia(e.getSecuencia() != null ? e.getSecuencia() : 0);
            info.setTipoAdjunto(e.getTipoAdjunto() != null ? e.getTipoAdjunto().name() : "DESCONOCIDO");
            info.setNombreArchivo(e.getNombreArchivo() != null ? e.getNombreArchivo() : "archivo_sin_nombre");
            info.setFechaUpload(e.getFechaCreacion());
            info.setUrlArchivo(e.getUrlArchivo() != null ? e.getUrlArchivo() : "");

            // Descargar el archivo igual que lo hace el frontend: via URL HTTP primero,
            // con fallback a lectura de disco si la URL falla.
            byte[] fileBytes = null;

            // ── Opción 1: Descarga via URL (igual que el frontend) ──────────────
            if (e.getUrlArchivo() != null && !e.getUrlArchivo().isEmpty()) {
                try {
                    java.net.URL url = java.net.URI.create(e.getUrlArchivo()).toURL();
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(15000);
                    conn.setRequestMethod("GET");
                    int status = conn.getResponseCode();
                    if (status == 200) {
                        try (java.io.InputStream stream = conn.getInputStream()) {
                            fileBytes = stream.readAllBytes();
                        }
                        logger.info("✅ Evidencia descargada via URL: {} ({} bytes)", e.getUrlArchivo(), fileBytes.length);
                    } else {
                        logger.warn("⚠️ HTTP {} al descargar evidencia: {}", status, e.getUrlArchivo());
                    }
                    conn.disconnect();
                } catch (Exception ex) {
                    logger.warn("⚠️ No se pudo descargar via URL {}: {}", e.getUrlArchivo(), ex.getMessage());
                }
            }

            // ── Opción 2: Fallback a lectura de disco ────────────────────────────
            if (fileBytes == null && e.getPathArchivo() != null) {
                try {
                    String basePath = "c:/Users/yarle/OneDrive/Documentos/Trabajo/Vilox v3/Vilox v3/vilox.api/storage/app/public/";
                    java.nio.file.Path fullPath = java.nio.file.Paths.get(basePath, e.getPathArchivo());
                    if (java.nio.file.Files.exists(fullPath)) {
                        fileBytes = java.nio.file.Files.readAllBytes(fullPath);
                        logger.info("✅ Evidencia leída del disco: {} ({} bytes)", fullPath, fileBytes.length);
                    } else {
                        logger.warn("⚠️ Archivo no encontrado en disco: {}", fullPath);
                    }
                } catch (Exception ex) {
                    logger.error("❌ Error al leer archivo desde disco {}: {}", e.getPathArchivo(), ex.getMessage());
                }
            }

            if (fileBytes != null) {
                info.setImagenData(fileBytes);
            }

            return info;
        }).collect(Collectors.toList());
        dto.setEvidencias(evidenciasInfo);

        // Reporte GPS Detallado - FASE 3
        List<ViajeReporteDetalleDto.TrackGpsInfo> tracksGps = obtenerTracksGpsDetallado(viaje);
        dto.setTracksGps(tracksGps);

        // Métricas
        ViajeReporteDetalleDto.MetricasViaje metricas = calcularMetricas(viaje, tramos, dto.getParadasDetectadas());
        dto.setMetricas(metricas);

        return dto;
    }

    /**
     * Calcula métricas del viaje
     */
    private ViajeReporteDetalleDto.MetricasViaje calcularMetricas(Viaje viaje, List<Tramo> tramos,
            List<ViajeReporteDetalleDto.ParadaInfo> paradas) {
        ViajeReporteDetalleDto.MetricasViaje metricas = new ViajeReporteDetalleDto.MetricasViaje();

        // Kilometraje total (podría calcularse desde tracks o estimarse)
        metricas.setKilometrajeTotal(0); // Placeholder

        // Tiempo total
        if (viaje.getFechaInicioReal() != null && viaje.getFechaFinReal() != null) {
            long minutos = Duration.between(viaje.getFechaInicioReal(), viaje.getFechaFinReal()).toMinutes();
            metricas.setTiempoTotalMinutos((int) minutos);
        }

        // Paradas
        if (paradas != null) {
            metricas.setParadasTotales(paradas.size());
            long justificadas = paradas.stream()
                    .filter(p -> p.getJustificada() != null && p.getJustificada())
                    .count();
            metricas.setParadasJustificadas((int) justificadas);
        }

        // Tardanza total
        int tardanzaTotal = tramos.stream()
                .mapToInt(t -> (t.getTardanzaCita1() != null ? t.getTardanzaCita1() : 0) +
                        (t.getTardanzaCita2() != null ? t.getTardanzaCita2() : 0))
                .sum();
        metricas.setTardanzaTotalMinutos(tardanzaTotal);

        // Porcentaje de cumplimiento (ejemplo básico)
        int tramosCompletados = (int) tramos.stream().filter(t -> t.getEstado().name().equalsIgnoreCase("completado"))
                .count();
        if (!tramos.isEmpty()) {
            metricas.setPorcentajeCumplimiento((double) tramosCompletados / tramos.size() * 100);
        }

        return metricas;
    }

    /**
     * Detecta paradas mayores a 20 minutos usando datos GPS - FASE 2
     */
    private List<ViajeReporteDetalleDto.ParadaInfo> detectarParadasGps(Viaje viaje) {
        List<ViajeReporteDetalleDto.ParadaInfo> paradas = new ArrayList<>();

        // Verificar que tenemos vehículo y fechas
        if (viaje.getVehiculo() == null || viaje.getVehiculo().getImei() == null) {
            logger.warn("⚠️ Vehículo sin IMEI, no se pueden detectar paradas GPS");
            return paradas;
        }

        if (viaje.getFechaInicioReal() == null || viaje.getFechaFinReal() == null) {
            logger.warn("⚠️ Viaje sin fechas reales, no se pueden detectar paradas GPS");
            return paradas;
        }

        String imei = viaje.getVehiculo().getImei();

        // Convertir fechas a timestamps Unix (segundos)
        long timestampInicio = viaje.getFechaInicioReal().atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
        long timestampFin = viaje.getFechaFinReal().atZone(java.time.ZoneId.systemDefault()).toEpochSecond();

        logger.info("🔍 Buscando tracks GPS para IMEI: {} entre {} y {}",
                imei, viaje.getFechaInicioReal(), viaje.getFechaFinReal());

        // Obtener tracks GPS del viaje
        List<Track> tracks = trackRepository.findTracksByImeiInTimeRange(imei, timestampInicio, timestampFin);
        logger.info("📍 Se encontraron {} tracks GPS", tracks.size());

        if (tracks.isEmpty()) {
            return paradas;
        }

        // Analizar tracks para detectar paradas
        final double VELOCIDAD_MAXIMA_PARADA = 5.0; // km/h
        final int MINUTOS_MINIMOS_PARADA = 20;

        Track trackInicio = null;
        LocalDateTime horaInicioParada = null;

        for (int i = 0; i < tracks.size(); i++) {
            Track track = tracks.get(i);
            Double velocidad = track.getSpeed() != null ? track.getSpeed() : 0.0;
            LocalDateTime fechaTrack = LocalDateTime.ofEpochSecond(
                    track.getGpstime(), 0, java.time.ZoneOffset.ofHours(-5)); // UTC-5 para Perú

            // Si está detenido (velocidad < 5 km/h)
            if (velocidad < VELOCIDAD_MAXIMA_PARADA) {
                if (trackInicio == null) {
                    // Inicio de una posible parada
                    trackInicio = track;
                    horaInicioParada = fechaTrack;
                }
            } else {
                // Está en movimiento
                if (trackInicio != null && horaInicioParada != null) {
                    // Fin de una parada
                    LocalDateTime horaFinParada = fechaTrack;
                    long minutosParada = Duration.between(horaInicioParada, horaFinParada).toMinutes();

                    // Solo registrar si duró más de 20 minutos
                    if (minutosParada >= MINUTOS_MINIMOS_PARADA) {
                        ViajeReporteDetalleDto.ParadaInfo parada = new ViajeReporteDetalleDto.ParadaInfo();
                        parada.setLatitud(trackInicio.getLatitude());
                        parada.setLongitud(trackInicio.getLongitude());
                        parada.setHoraInicio(horaInicioParada);
                        parada.setHoraFin(horaFinParada);
                        parada.setDuracionMinutos((int) minutosParada);
                        parada.setMotorApagado(trackInicio.getAccstatus() != null ? !trackInicio.getAccstatus() : null);
                        parada.setJustificada(false); // Por defecto no justificada
                        parada.setDireccion(""); // Podría agregarse geocodificación inversa

                        // Categorizar parada por duración
                        if (minutosParada >= 120) {
                            parada.setCategoria("ALTA");
                        } else if (minutosParada >= 60) {
                            parada.setCategoria("MEDIA");
                        } else {
                            parada.setCategoria("BAJA");
                        }

                        paradas.add(parada);
                        logger.info("🛑 Parada detectada: {} min en ({}, {})",
                                minutosParada, trackInicio.getLatitude(), trackInicio.getLongitude());
                    }

                    // Reset
                    trackInicio = null;
                    horaInicioParada = null;
                }
            }
        }

        // Verificar si hay una parada al final
        if (trackInicio != null && horaInicioParada != null) {
            LocalDateTime horaFinParada = LocalDateTime.ofEpochSecond(
                    tracks.get(tracks.size() - 1).getGpstime(), 0, java.time.ZoneOffset.ofHours(-5));
            long minutosParada = Duration.between(horaInicioParada, horaFinParada).toMinutes();

            if (minutosParada >= MINUTOS_MINIMOS_PARADA) {
                ViajeReporteDetalleDto.ParadaInfo parada = new ViajeReporteDetalleDto.ParadaInfo();
                parada.setLatitud(trackInicio.getLatitude());
                parada.setLongitud(trackInicio.getLongitude());
                parada.setHoraInicio(horaInicioParada);
                parada.setHoraFin(horaFinParada);
                parada.setDuracionMinutos((int) minutosParada);
                parada.setMotorApagado(trackInicio.getAccstatus() != null ? !trackInicio.getAccstatus() : null);
                parada.setJustificada(false);
                parada.setDireccion("");

                if (minutosParada >= 120) {
                    parada.setCategoria("ALTA");
                } else if (minutosParada >= 60) {
                    parada.setCategoria("MEDIA");
                } else {
                    parada.setCategoria("BAJA");
                }

                paradas.add(parada);
            }
        }

        logger.info("✅ Total de paradas detectadas: {}", paradas.size());
        return paradas;
    }

    /**
     * Obtiene tracks GPS muestreados cada 5 minutos - FASE 3
     */
    private List<ViajeReporteDetalleDto.TrackGpsInfo> obtenerTracksGpsDetallado(Viaje viaje) {
        List<ViajeReporteDetalleDto.TrackGpsInfo> tracksInfo = new ArrayList<>();

        // Verificar que tenemos vehículo y fechas
        if (viaje.getVehiculo() == null || viaje.getVehiculo().getImei() == null) {
            return tracksInfo;
        }

        if (viaje.getFechaInicioReal() == null || viaje.getFechaFinReal() == null) {
            return tracksInfo;
        }

        String imei = viaje.getVehiculo().getImei();
        long timestampInicio = viaje.getFechaInicioReal().atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
        long timestampFin = viaje.getFechaFinReal().atZone(java.time.ZoneId.systemDefault()).toEpochSecond();

        // Obtener todos los tracks
        List<Track> todosLosTracks = trackRepository.findTracksByImeiInTimeRange(imei, timestampInicio, timestampFin);

        if (todosLosTracks.isEmpty()) {
            return tracksInfo;
        }

        logger.info("📊 Generando reporte GPS detallado con {} tracks totales", todosLosTracks.size());

        // Muestrear tracks cada 5 minutos (300 segundos)
        final int INTERVALO_SEGUNDOS = 300; // 5 minutos
        Track ultimoTrackIncluido = null;

        for (Track track : todosLosTracks) {
            // Incluir el primer track siempre
            if (ultimoTrackIncluido == null) {
                tracksInfo.add(convertirTrackAInfo(track));
                ultimoTrackIncluido = track;
                continue;
            }

            // Verificar si han pasado 5 minutos desde el último track incluido
            long diferenciaSegundos = track.getGpstime() - ultimoTrackIncluido.getGpstime();
            if (diferenciaSegundos >= INTERVALO_SEGUNDOS) {
                tracksInfo.add(convertirTrackAInfo(track));
                ultimoTrackIncluido = track;
            }
        }

        // Incluir el último track si no está ya incluido
        Track ultimoTrack = todosLosTracks.get(todosLosTracks.size() - 1);
        if (ultimoTrackIncluido == null || !ultimoTrackIncluido.getId().equals(ultimoTrack.getId())) {
            tracksInfo.add(convertirTrackAInfo(ultimoTrack));
        }

        logger.info("✅ Tracks GPS muestreados: {} de {} totales", tracksInfo.size(), todosLosTracks.size());
        return tracksInfo;
    }

    /**
     * Convierte un Track a TrackGpsInfo
     */
    private ViajeReporteDetalleDto.TrackGpsInfo convertirTrackAInfo(Track track) {
        ViajeReporteDetalleDto.TrackGpsInfo info = new ViajeReporteDetalleDto.TrackGpsInfo();

        LocalDateTime fechaHora = LocalDateTime.ofEpochSecond(
                track.getGpstime(), 0, java.time.ZoneOffset.ofHours(-5));

        info.setFechaHora(fechaHora);
        info.setLatitud(track.getLatitude());
        info.setLongitud(track.getLongitude());
        info.setVelocidad(track.getSpeed() != null ? track.getSpeed() : 0.0);
        info.setMotorEncendido(track.getAccstatus() != null ? track.getAccstatus() : false);

        // Determinar evento basado en velocidad
        if (track.getSpeed() == null || track.getSpeed() < 5.0) {
            info.setEvento("Detenido");
        } else if (track.getSpeed() < 40.0) {
            info.setEvento("En movimiento");
        } else {
            info.setEvento("Alta velocidad");
        }

        return info;
    }

    /**
     * Genera HTML del reporte
     */
    private String generarHtml(ViajeReporteDetalleDto datos) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset='UTF-8'/>");
        html.append("<style>");
        html.append(getStyles());
        html.append("</style>");
        html.append("</head><body>");

        // Cabecera
        html.append("<div class='header'>");
        // html.append("<h1>VILOX</h1>");
         html.append("<h1>").append(appName).append("</h1>");
        html.append("<h2>REPORTE DE TRAZABILIDAD</h2>");
        html.append("<p class='subtitle'>Código de Viaje: ").append(datos.getCodigoViaje()).append("</p>");
        html.append("<p class='subtitle'>Fecha de Emisión: ").append(LocalDateTime.now().format(DATETIME_FORMAT))
                .append("</p>");
        html.append("</div>");

        // Información General
        html.append("<div class='section'>");
        html.append("<h3>INFORMACIÓN GENERAL</h3>");
        html.append("<table class='info-table'>");
        html.append("<tr><td class='label'>Tipo de Operación:</td><td>").append(safe(datos.getTipoOperacion()))
                .append("</td></tr>");
        html.append("<tr><td class='label'>Contenedor:</td><td>").append(safe(datos.getContenedor()))
                .append("</td></tr>");
        html.append("<tr><td class='label'>Documento de Embarque:</td><td>").append(safe(datos.getDocumentoEmbarque()))
                .append("</td></tr>");
        html.append("<tr><td class='label'>Estado:</td><td class='estado-").append(safe(datos.getEstado())).append("'>")
                .append(safe(datos.getEstado()).toUpperCase()).append("</td></tr>");
        html.append("</table>");
        html.append("</div>");

        // Actores
        html.append("<div class='section'>");
        html.append("<h3>ACTORES</h3>");
        html.append("<table class='info-table'>");
        if (datos.getCliente() != null) {
            html.append("<tr><td class='label'>Cliente:</td><td>").append(datos.getCliente().getNombre())
                    .append("</td></tr>");
        }
        if (datos.getTransportista() != null) {
            html.append("<tr><td class='label'>Transportista:</td><td>").append(datos.getTransportista().getNombre())
                    .append("</td></tr>");
        }
        if (datos.getOperadorLogistico() != null) {
            html.append("<tr><td class='label'>Generador De Carga:</td><td>")
                    .append(datos.getOperadorLogistico().getNombre()).append("</td></tr>");
        }
        if (datos.getNaviera() != null) {
            html.append("<tr><td class='label'>Naviera:</td><td>").append(datos.getNaviera().getNombre())
                    .append("</td></tr>");
        }
        html.append("</table>");
        html.append("</div>");

        // Recursos
        html.append("<div class='section'>");
        html.append("<h3>RECURSOS</h3>");
        html.append("<table class='info-table'>");
        if (datos.getConductor() != null) {
            html.append("<tr><td class='label'>Conductor:</td><td>").append(datos.getConductor().getNombre());
            if (datos.getConductor().getLicencia() != null) {
                html.append(" - Licencia: ").append(datos.getConductor().getLicencia());
            }
            html.append("</td></tr>");
        }
        if (datos.getVehiculo() != null) {
            html.append("<tr><td class='label'>Remolcador:</td><td>").append(datos.getVehiculo().getPlaca());
            if (datos.getVehiculo().getMarca() != null || datos.getVehiculo().getModelo() != null) {
                html.append(" (").append(safe(datos.getVehiculo().getMarca())).append(" ")
                        .append(safe(datos.getVehiculo().getModelo())).append(")");
            }
            html.append("</td></tr>");
        }
        if (datos.getCarreta() != null) {
            html.append("<tr><td class='label'>Carreta:</td><td>").append(datos.getCarreta().getPlaca())
                    .append("</td></tr>");
        }
        html.append("</table>");
        html.append("</div>");

        // CRONOLOGÍA DEL VIAJE - Igual que en el frontend
        html.append("<div class='section'>");
        html.append("<h3>CRONOLOGÍA</h3>");
        html.append("<table class='info-table'>");

        if (datos.getFechaInicioProgramada() != null) {
            html.append("<tr><td class='label'>Posicionamiento En Inicio:</td><td>")
                    .append(formatHora(datos.getFechaInicioProgramada())).append("</td></tr>");
        }

        if (datos.getFechaFinProgramada() != null) {
            html.append("<tr><td class='label'>Posicionamiento En Destino:</td><td>")
                    .append(formatHora(datos.getFechaFinProgramada())).append("</td></tr>");
        }

        if (datos.getFechaInicioReal() != null) {
            html.append("<tr><td class='label'>Inicio Real:</td><td style='color: #059669; font-weight: bold;'>")
                    .append(formatHora(datos.getFechaInicioReal())).append("</td></tr>");
        }

        if (datos.getFechaFinReal() != null) {
            html.append("<tr><td class='label'>Finalizado:</td><td style='color: #059669; font-weight: bold;'>")
                    .append(formatHora(datos.getFechaFinReal())).append("</td></tr>");
        }

        html.append("</table>");
        html.append("</div>");

        // Ruta y Tiempos - Tabla expandida con llegada y salida
        if (datos.getTramos() != null && !datos.getTramos().isEmpty()) {
            html.append("<div class='section'>");
            html.append("<h3>RUTA Y TIEMPOS</h3>");
            html.append("<table class='route-table'>");
            html.append("<thead>");
            html.append("<tr>");
            html.append("<th style='width: 3%'>#</th>");
            html.append("<th style='width: 13%'>Origen</th>");
            html.append("<th style='width: 13%'>Destino</th>");
            html.append("<th style='width: 10%'>Llegada Prog.</th>");
            html.append("<th style='width: 10%'>Llegada Real</th>");
            html.append("<th style='width: 10%'>Salida Prog.</th>");
            html.append("<th style='width: 10%'>Salida Real</th>");
            html.append("<th style='width: 7%'>Tardanza</th>");
            html.append("<th style='width: 8%'>Permanencia</th>"); // Tiempo total en el punto
            html.append("<th style='width: 8%'>T. Atención</th>"); // Diferencia vs SLA
            html.append("</tr>");
            html.append("</thead>");
            html.append("<tbody>");

            for (ViajeReporteDetalleDto.TramoInfo tramo : datos.getTramos()) {
                html.append("<tr>");
                html.append("<td style='text-align: center'>").append(tramo.getNumero()).append("</td>");
                html.append("<td>").append(safe(tramo.getNombreOrigen())).append("</td>");
                html.append("<td>").append(safe(tramo.getNombreDestino())).append("</td>");

                // Llegada Programada (Cita1)
                html.append("<td style='font-size: 8pt;'>").append(formatHora(tramo.getFechaHoraCita1()))
                        .append("</td>");

                // Llegada Real (Cita1)
                html.append("<td style='font-size: 8pt;'>").append(formatHora(tramo.getHoraInicioReal()))
                        .append("</td>");

                // Salida Programada (Cita2)
                html.append("<td style='font-size: 8pt;'>").append(formatHora(tramo.getFechaHoraCita2()))
                        .append("</td>");

                // Salida Real (Cita2)
                html.append("<td style='font-size: 8pt;'>").append(formatHora(tramo.getHoraFinReal())).append("</td>");

                // Tardanza (mayor de las dos tardanzas)
                Integer tardanza1 = tramo.getTardanzaCita1() != null ? tramo.getTardanzaCita1() : 0;
                Integer tardanza2 = tramo.getTardanzaCita2() != null ? tramo.getTardanzaCita2() : 0;
                Integer tardanzaMaxima = Math.max(tardanza1, tardanza2);

                String tardanzaClass = tardanzaMaxima > 0 ? " class='tardanza'" : "";
                html.append("<td").append(tardanzaClass).append(" style='text-align: center; font-weight: bold;'>");
                html.append(tardanzaMaxima > 0 ? tardanzaMaxima + " min" : "-");
                html.append("</td>");

                // Permanencia (tiempo total en el punto - máximo de ambas permanencias)
                Integer permanencia1 = tramo.getTiempoPermanenciaCita1() != null ? tramo.getTiempoPermanenciaCita1()
                        : 0;
                Integer permanencia2 = tramo.getTiempoPermanenciaCita2() != null ? tramo.getTiempoPermanenciaCita2()
                        : 0;
                Integer permanenciaMaxima = Math.max(permanencia1, permanencia2);

                html.append("<td style='text-align: center; font-weight: bold;'>")
                        .append(permanenciaMaxima > 0 ? permanenciaMaxima + " min" : "-").append("</td>");

                // Tiempo de Atención (diferencia vs SLA - máximo de ambos tiempos)
                Integer atencion1 = tramo.getTiempoAtencionCita1() != null ? tramo.getTiempoAtencionCita1() : 0;
                Integer atencion2 = tramo.getTiempoAtencionCita2() != null ? tramo.getTiempoAtencionCita2() : 0;
                Integer atencionMaxima = Math.max(atencion1, atencion2);

                // Resaltar en rojo si excede SLA
                String atencionClass = atencionMaxima > 30 ? " class='tardanza'" : "";
                html.append("<td").append(atencionClass).append(" style='text-align: center; font-weight: bold;'>")
                        .append(atencionMaxima > 0 ? atencionMaxima + " min" : "-").append("</td>");

                html.append("</tr>");
            }
            html.append("</tbody>");
            html.append("</table>");
            html.append("</div>");
        }

        // PARADAS MAYORES A 20 MINUTOS (GPS) - FASE 2
        if (datos.getParadasDetectadas() != null && !datos.getParadasDetectadas().isEmpty()) {
            html.append("<div class='section'>");
            html.append("<h3>PARADAS MAYORES A 20 MINUTOS (GPS)</h3>");
            html.append("<table class='route-table'>");
            html.append("<thead>");
            html.append("<tr>");
            html.append("<th style='width: 5%'>#</th>");
            html.append("<th style='width: 15%'>Hora Inicio</th>");
            html.append("<th style='width: 15%'>Hora Fin</th>");
            html.append("<th style='width: 10%'>Duración</th>");
            html.append("<th style='width: 15%'>Latitud</th>");
            html.append("<th style='width: 15%'>Longitud</th>");
            html.append("<th style='width: 10%'>Motor</th>");
            html.append("<th style='width: 15%'>Categoría</th>");
            html.append("</tr>");
            html.append("</thead>");
            html.append("<tbody>");

            int paradaNum = 1;
            for (ViajeReporteDetalleDto.ParadaInfo parada : datos.getParadasDetectadas()) {
                html.append("<tr>");
                html.append("<td style='text-align: center'>").append(paradaNum++).append("</td>");
                html.append("<td>").append(formatHora(parada.getHoraInicio())).append("</td>");
                html.append("<td>").append(formatHora(parada.getHoraFin())).append("</td>");

                // Duración en rojo si es alta
                String duracionClass = parada.getDuracionMinutos() >= 60 ? " class='tardanza'" : "";
                html.append("<td").append(duracionClass).append(" style='text-align: center; font-weight: bold;'>");
                html.append(parada.getDuracionMinutos()).append(" min</td>");

                html.append("<td style='text-align: center; font-size: 7pt;'>")
                        .append(String.format("%.6f", parada.getLatitud())).append("</td>");
                html.append("<td style='text-align: center; font-size: 7pt;'>")
                        .append(String.format("%.6f", parada.getLongitud())).append("</td>");

                // Estado del motor
                String motorText = parada.getMotorApagado() != null
                        ? (parada.getMotorApagado() ? "❌ Apagado" : "✅ Encendido")
                        : "-";
                html.append("<td style='text-align: center; font-size: 8pt;'>").append(motorText).append("</td>");

                // Categoría con color
                String categoriaColor = switch (parada.getCategoria()) {
                    case "ALTA" -> "color: #dc2626; font-weight: bold;";
                    case "MEDIA" -> "color: #f59e0b; font-weight: bold;";
                    default -> "color: #059669;";
                };
                html.append("<td style='text-align: center; ").append(categoriaColor).append("'>");
                html.append(parada.getCategoria()).append("</td>");

                html.append("</tr>");
            }

            html.append("</tbody>");
            html.append("</table>");
            html.append("</div>");
        }

        // SECCIONES DE EVIDENCIAS POR HITO — igual que el modal del frontend
        if (datos.getEvidencias() != null && !datos.getEvidencias().isEmpty()) {
            logger.info("📸 Total de evidencias encontradas: {}", datos.getEvidencias().size());

            // Indexar evidencias por (hito, secuencia) para búsqueda O(1)
            Map<String, ViajeReporteDetalleDto.EvidenciaInfo> evidenciaMap = new LinkedHashMap<>();
            List<ViajeReporteDetalleDto.EvidenciaInfo> unmappedEvidencias = new ArrayList<>(datos.getEvidencias());
            
            for (ViajeReporteDetalleDto.EvidenciaInfo ev : datos.getEvidencias()) {
                String key = ev.getHito() + "__" + ev.getSecuencia();
                evidenciaMap.put(key, ev);
                logger.info("  📎 Evidencia leída del DTO: Hito={} | Secuencia={} | Nombre={} | Bytes={}",
                    ev.getHito(),
                    ev.getSecuencia(),
                    ev.getNombreArchivo(),
                    ev.getImagenData() != null ? ev.getImagenData().length : "null");
            }

            renderEvidenciasOrganizadas(html, evidenciaMap, unmappedEvidencias);
            
            // Si quedaron evidencias sin mapear, mostrarlas al final para que no se pierdan
            if (!unmappedEvidencias.isEmpty()) {
                logger.warn("⚠️ Se encontraron {} evidencias que no encajaron en el mapa estándar", unmappedEvidencias.size());
                renderOtrasEvidencias(html, unmappedEvidencias);
            }
        } else {
            logger.warn("⚠️ No se encontraron evidencias para el viaje {}", datos.getCodigoViaje());
        }

        // REPORTE GPS DETALLADO - FASE 3
        if (datos.getTracksGps() != null && !datos.getTracksGps().isEmpty()) {
            html.append("<div class='section' style='page-break-before: always;'>");
            html.append("<h3>REPORTE GPS DETALLADO (Cada 5 minutos)</h3>");
            html.append("<p style='font-size: 8pt; color: #6b7280; margin-bottom: 10px;'>")
                    .append("Total de registros: ").append(datos.getTracksGps().size()).append("</p>");
            html.append("<table class='gps-table'>");
            html.append("<thead>");
            html.append("<tr>");
            html.append("<th style='width: 5%'>#</th>");
            html.append("<th style='width: 20%'>Fecha/Hora</th>");
            html.append("<th style='width: 15%'>Latitud</th>");
            html.append("<th style='width: 15%'>Longitud</th>");
            html.append("<th style='width: 12%'>Velocidad</th>");
            html.append("<th style='width: 13%'>Motor</th>");
            html.append("<th style='width: 20%'>Evento</th>");
            html.append("</tr>");
            html.append("</thead>");
            html.append("<tbody>");

            int trackNum = 1;
            for (ViajeReporteDetalleDto.TrackGpsInfo track : datos.getTracksGps()) {
                html.append("<tr>");
                html.append("<td style='text-align: center'>").append(trackNum++).append("</td>");
                html.append("<td>").append(formatHora(track.getFechaHora())).append("</td>");
                html.append("<td style='text-align: center; font-size: 7pt;'>")
                        .append(String.format("%.6f", track.getLatitud())).append("</td>");
                html.append("<td style='text-align: center; font-size: 7pt;'>")
                        .append(String.format("%.6f", track.getLongitud())).append("</td>");

                // Velocidad con color según valor
                String velocidadColor = "";
                if (track.getVelocidad() >= 80) {
                    velocidadColor = "color: #dc2626; font-weight: bold;";
                } else if (track.getVelocidad() < 5) {
                    velocidadColor = "color: #6b7280;";
                }
                html.append("<td style='text-align: center; ").append(velocidadColor).append("'>")
                        .append(String.format("%.1f km/h", track.getVelocidad())).append("</td>");

                // Motor
                String motorIcon = track.getMotorEncendido() ? "✅" : "❌";
                html.append("<td style='text-align: center; font-size: 8pt;'>").append(motorIcon).append("</td>");

                // Evento
                html.append("<td style='font-size: 8pt;'>").append(track.getEvento()).append("</td>");
                html.append("</tr>");
            }

            html.append("</tbody>");
            html.append("</table>");
            html.append("</div>");
        }

        // Resumen y Métricas - Formato del PDF de referencia
        if (datos.getMetricas() != null) {
            html.append("<div class='section' style='page-break-before: avoid;'>");
            html.append("<h3>RESUMEN Y MÉTRICAS</h3>");

            html.append("<div class='metrics-container'>");

            // Tiempo Total (formato grande)
            if (datos.getMetricas().getTiempoTotalMinutos() != null) {
                html.append("<div class='metric-large'>");
                html.append("<div class='metric-value-large'>")
                        .append(formatTiempoGrande(datos.getMetricas().getTiempoTotalMinutos() != null ? datos.getMetricas().getTiempoTotalMinutos() : 0)).append("</div>");
                html.append("<div class='metric-label-large'>Tiempo Total</div>");
                html.append("</div>");
            }

            // Tardanza Total (formato minutos)
            if (datos.getMetricas().getTardanzaTotalMinutos() != null
                    && datos.getMetricas().getTardanzaTotalMinutos() > 0) {
                html.append("<div class='metric-large'>");
                html.append("<div class='metric-value-large' style='color: #dc2626;'>")
                        .append(datos.getMetricas().getTardanzaTotalMinutos()).append(" min</div>");
                html.append("<div class='metric-label-large'>Tardanza Total</div>");
                html.append("</div>");
            }

            html.append("</div>");

            // Tabla de métricas adicionales
            html.append("<table class='info-table' style='margin-top: 20px;'>");
            if (datos.getMetricas().getPorcentajeCumplimiento() != null) {
                html.append("<tr><td class='label'>Cumplimiento:</td><td style='font-weight: bold; color: #059669;'>")
                        .append(String.format("%.1f%%", datos.getMetricas().getPorcentajeCumplimiento()))
                        .append("</td></tr>");
            }
            if (datos.getMetricas().getKilometrajeTotal() != null && datos.getMetricas().getKilometrajeTotal() > 0) {
                html.append("<tr><td class='label'>Kilometraje Total:</td><td>")
                        .append(datos.getMetricas().getKilometrajeTotal()).append(" km</td></tr>");
            }
            if (datos.getMetricas().getParadasTotales() != null && datos.getMetricas().getParadasTotales() > 0) {
                html.append("<tr><td class='label'>Paradas Totales:</td><td>")
                        .append(datos.getMetricas().getParadasTotales()).append("</td></tr>");
            }
            html.append("</table>");
            html.append("</div>");
        }

        html.append("</body></html>");

        return html.toString();
    }

    /**
     * Convierte HTML a PDF
     */
    private byte[] htmlToPdf(String html) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(PageSize.A4);

            ConverterProperties props = new ConverterProperties();
            HtmlConverter.convertToPdf(html, pdfDoc, props);

            return baos.toByteArray();
        }
    }

    /**
     * Estilos CSS para el PDF
     */
    private String getStyles() {
        return """
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { font-family: Arial, sans-serif; font-size: 10pt; color: #333; padding: 20px; }
                .header { text-align: center; margin-bottom: 30px; border-bottom: 3px solid #2563eb; padding-bottom: 15px; }
                .header h1 { font-size: 24pt; color: #2563eb; margin-bottom: 5px; }
                .header h2 { font-size: 16pt; color: #1e40af; margin-bottom: 10px; }
                .subtitle { font-size: 9pt; color: #6b7280; }
                .section { margin-bottom: 25px; page-break-inside: auto; }
                .section h3 { font-size: 12pt; color: #ffffff; background-color: #2563eb; padding: 10px 15px; margin-bottom: 0; }
                .info-table { width: 100%; border-collapse: collapse; margin-bottom: 15px; }
                .info-table td { padding: 6px; border-bottom: 1px solid #e5e7eb; }
                .info-table .label { font-weight: bold; width: 30%; color: #4b5563; }
                .route-table { width: 100%; border-collapse: collapse; font-size: 9pt; margin-top: 10px; }
                .route-table th { background-color: #2563eb; color: white; padding: 8px 4px; text-align: center; font-size: 8pt; }
                .route-table td { padding: 6px 4px; border-bottom: 1px solid #e5e7eb; font-size: 8pt; }
                .route-table tbody tr:nth-child(even) { background-color: #f9fafb; }
                .gps-table { width: 100%; border-collapse: collapse; font-size: 8pt; margin-top: 10px; }
                .gps-table th { background-color: #059669; color: white; padding: 6px 3px; text-align: center; font-size: 7pt; }
                .gps-table td { padding: 4px 3px; border-bottom: 1px solid #e5e7eb; font-size: 7pt; }
                .gps-table tbody tr:nth-child(even) { background-color: #f0fdf4; }
                .metrics-container { display: flex; justify-content: center; gap: 40px; margin: 20px 0; }
                .metric-large { text-align: center; }
                .metric-value-large { font-size: 36pt; font-weight: bold; color: #2563eb; line-height: 1; }
                .metric-label-large { font-size: 11pt; color: #6b7280; margin-top: 8px; }
                .photo-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 15px; margin-top: 10px; }
                .photo-item { text-align: center; border: 1px solid #e5e7eb; padding: 10px; background-color: #f9fafb; }
                .photo-item img { width: 100%; height: auto; max-height: 300px; object-fit: contain; }
                .photo-caption { font-size: 8pt; color: #6b7280; margin-top: 5px; }
                .no-image { padding: 20px; background-color: #e5e7eb; color: #9ca3af; font-size: 9pt; text-align: center; }
                .no-image-req { padding: 20px 8px; background-color: #fef2f2; color: #dc2626; font-size: 8pt; text-align: center; border: 1px dashed #fca5a5; border-radius: 4px; }
                .no-image-opt { padding: 20px 8px; background-color: #fefce8; color: #ca8a04; font-size: 8pt; text-align: center; border: 1px dashed #fde047; border-radius: 4px; }
                .evidencia-table { width: 100%; border-collapse: collapse; font-size: 9pt; table-layout: fixed; }
                .evidencia-table th { background-color: #2563eb; color: white; padding: 8px 6px; text-align: left; font-size: 8pt; border: 1px solid #2563eb; }
                .evidencia-table td { padding: 8px 6px; border-bottom: 1px solid #e5e7eb; vertical-align: middle; }
                .evidencia-table tr { page-break-inside: avoid; }
                .evidencia-table tbody tr:nth-child(even) { background-color: #f9fafb; }
                .badge-oblig { display: inline-block; padding: 2px 8px; background-color: #fee2e2; color: #b91c1c; border: 1px solid #fca5a5; border-radius: 9999px; font-size: 7pt; font-weight: bold; }
                .badge-opt { display: inline-block; padding: 2px 8px; background-color: #dbeafe; color: #1d4ed8; border: 1px solid #93c5fd; border-radius: 9999px; font-size: 7pt; font-weight: bold; }
                .tardanza { color: #dc2626; font-weight: bold; }
                .estado-completado { color: #059669; font-weight: bold; }
                .estado-en_curso { color: #2563eb; font-weight: bold; }
                """;
    }

    private String safe(String value) {
        return value != null ? value : "-";
    }

    private String formatHora(LocalDateTime fecha) {
        return fecha != null ? fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-";
    }

    private String formatTiempoGrande(int minutos) {
        int horas = minutos / 60;
        int mins = minutos % 60;
        return horas + "h " + mins + "m";
    }

    /**
     * Renderiza las evidencias organizadas exactamente igual que el modal del
     * frontend: una sección por hito, tabla con #, descripción, tipo y preview.
     * Solo se renderizan los hitos que tienen al menos una foto subida.
     *
     * NOTA: iText HtmlConverter no respeta CSS max-width/max-height en imágenes.
     * Para evitar overflow/solapamiento, cada evidencia se divide en 2 filas:
     *   Fila A: #, Descripción, Tipo
     *   Fila B: columna imagen (colspan 3) centrada y con tamaño fijo
     * Las imágenes se redimensionan en Java antes de incrustar.
     */
    private void renderEvidenciasOrganizadas(
            StringBuilder html,
            Map<String, ViajeReporteDetalleDto.EvidenciaInfo> evidenciaMap,
            List<ViajeReporteDetalleDto.EvidenciaInfo> unmappedList) {

        for (HitoSet hitoSet : EVIDENCIAS_SETS) {
            // Buscamos si hay alguna evidencia para este hito usando fallbacks
            boolean tieneAlguna = false;
            for (int i = 0; i < hitoSet.fotos().size(); i++) {
                if (buscarEvidenciaConFallbacks(hitoSet.hito(), i + 1, evidenciaMap) != null) {
                    tieneAlguna = true;
                    break;
                }
            }
            
            if (!tieneAlguna) {
                logger.info("  ⏭️ Saltando hito {} (sin evidencias mapeadas)", hitoSet.hito());
                continue;
            }

            html.append("<div class='section'>");
            html.append("<h3>EVIDENCIAS: ").append(hitoSet.nombreUi().toUpperCase()).append("</h3>");

            html.append("<table class='evidencia-table'>");
            html.append("<thead><tr>");
            html.append("<th style='width:8%'>#</th>");
            html.append("<th style='width:40%'>Descripción</th>");
            html.append("<th style='width:20%'>Tipo</th>");
            html.append("<th style='width:32%'>Estado</th>");
            html.append("</tr></thead><tbody>");

            for (int i = 0; i < hitoSet.fotos().size(); i++) {
                FotoSet foto = hitoSet.fotos().get(i);
                int secuencia = i + 1;
                
                ViajeReporteDetalleDto.EvidenciaInfo evidencia = buscarEvidenciaConFallbacks(hitoSet.hito(), secuencia, evidenciaMap);
                
                // Si la encontramos, la quitamos de la lista de "unmapped" para que no salga repetida al final
                if (evidencia != null) {
                    final String evHito = evidencia.getHito();
                    final int evSec = evidencia.getSecuencia();
                    unmappedList.removeIf(e -> e.getHito().equals(evHito) && e.getSecuencia() == evSec);
                    logger.info("  ✅ Mapeado: {}__ {} -> Set: {}", evHito, evSec, hitoSet.hito());
                }

                // ─── Fila A: número + descripción + tipo + estado ───
                String rowBg = (i % 2 == 0) ? "" : "background-color:#f9fafb;";
                html.append("<tr style='").append(rowBg).append("'>");

                // Número
                html.append("<td style='text-align:center;font-weight:bold;vertical-align:middle;padding:8px 4px'>")
                    .append(secuencia).append(".</td>");

                // Descripción
                html.append("<td style='vertical-align:middle;padding:8px 6px'>").append(foto.nombre());
                if (foto.obligatorio()) {
                    html.append(" <span style='color:#dc2626;font-weight:bold'>*</span>");
                }
                html.append("</td>");

                // Badge Obligatorio / Opcional
                html.append("<td style='text-align:center;vertical-align:middle;padding:8px 4px'>");
                if (foto.obligatorio()) {
                    html.append("<span class='badge-oblig'>Obligatorio</span>");
                } else {
                    html.append("<span class='badge-opt'>Opcional</span>");
                }
                html.append("</td>");

                // Columna Estado (si hay imagen o no)
                if (evidencia != null && evidencia.getImagenData() != null && evidencia.getImagenData().length > 0) {
                    html.append("<td style='text-align:center;vertical-align:middle;padding:8px 4px'>")
                        .append("<span style='color:#059669;font-weight:bold;font-size:8pt'>&#10003; Cargada</span>")
                        .append("</td>");
                } else {
                    if (foto.obligatorio()) {
                        html.append("<td style='text-align:center;vertical-align:middle;padding:8px 4px'>")
                            .append("<span style='color:#dc2626;font-size:8pt'>Pendiente</span>")
                            .append("</td>");
                    } else {
                        html.append("<td style='text-align:center;vertical-align:middle;padding:8px 4px'>")
                            .append("<span style='color:#ca8a04;font-size:8pt'>Opcional</span>")
                            .append("</td>");
                    }
                }
                html.append("</tr>");

                // ─── Fila B: imagen centrada (solo si tiene imagen) ───
                if (evidencia != null && evidencia.getImagenData() != null && evidencia.getImagenData().length > 0) {
                    html.append("<tr style='").append(rowBg).append("border-bottom:2px solid #e5e7eb;'>");
                    html.append("<td colspan='4' style='text-align:center;padding:8px 16px 16px 16px;'>");

                    try {
                        // PASO 1: intentar leer como imagen (JPEG/PNG/GIF/BMP)
                        java.awt.image.BufferedImage testImg;
                        try (java.io.InputStream testIs = new java.io.ByteArrayInputStream(evidencia.getImagenData())) {
                            testImg = javax.imageio.ImageIO.read(testIs);
                        }

                        if (testImg != null) {
                            // Es una imagen válida: redimensionar a máx 400x300 manteniendo aspecto.
                            // NO hay límite de MB previo — fotos de alta resolución se achican aquí.
                            byte[] imgResized = resizeImageBytes(evidencia.getImagenData(), 400, 300);
                            int[] dims = readImageDimensions(imgResized);
                            String b64 = java.util.Base64.getEncoder().encodeToString(imgResized);
                            String fname = evidencia.getNombreArchivo() != null
                                    ? evidencia.getNombreArchivo().toLowerCase() : "";
                            String mime = fname.endsWith(".png") ? "image/png" : "image/jpeg";
                            logger.info("  🖼  Imagen embebida: {}x{} px | mime: {} | bytes: {}",
                                    dims[0], dims[1], mime, imgResized.length);
                            html.append("<img src='data:").append(mime).append(";base64,").append(b64)
                                .append("' width='").append(dims[0])
                                .append("' height='").append(dims[1])
                                .append("' style='display:block;margin:0 auto;border:1px solid #d1d5db' />");
                        } else {
                            // No es imagen parseable (PDF u otro doc) — placeholder simple
                            String nombreMostrar = evidencia.getNombreArchivo() != null
                                    ? evidencia.getNombreArchivo() : "Archivo adjunto";
                            logger.info("  📄 Archivo no-imagen embebido como placeholder: {}", nombreMostrar);
                            html.append("<table style='margin:0 auto;border:2px solid #2563eb;")
                                .append("background:#eff6ff;border-radius:4px;'><tr><td ")
                                .append("style='padding:16px 32px;text-align:center;'>")
                                .append("<p style='font-size:20pt;color:#2563eb;margin:0'>[ PDF ]</p>")
                                .append("<p style='font-size:8pt;color:#1d4ed8;font-weight:bold;margin:4px 0'>")
                                .append("Documento adjunto</p>")
                                .append("<p style='font-size:7pt;color:#4b5563;margin:0'>")
                                .append(nombreMostrar)
                                .append("</p></td></tr></table>");
                        }
                    } catch (Exception ex) {
                        logger.error("Error procesando archivo para PDF: {}", ex.getMessage());
                        html.append("<p style='color:#dc2626;font-size:8pt'>Error al procesar archivo</p>");
                    }
                    html.append("</td>");
                    html.append("</tr>");
                }
            }

            html.append("</tbody></table>");
            html.append("</div>"); // section
        }
    }

    /**
     * Busca una evidencia en el mapa usando el nombre exacto del hito o variantes comunes.
     */
    private ViajeReporteDetalleDto.EvidenciaInfo buscarEvidenciaConFallbacks(
            String hitoBusqueda, int secuencia, Map<String, ViajeReporteDetalleDto.EvidenciaInfo> map) {
        
        // 1. Intento exacto
        String key = hitoBusqueda + "__" + secuencia;
        if (map.containsKey(key)) return map.get(key);
        
        // 2. Fallbacks específicos por hito
        if ("LLEGADA_DEPOT".equals(hitoBusqueda)) {
            if (map.containsKey("LLEGADA_AL_DEPOT__" + secuencia)) return map.get("LLEGADA_AL_DEPOT__" + secuencia);
            if (map.containsKey("RETIRO_DE_VACIO__" + secuencia)) return map.get("RETIRO_DE_VACIO__" + secuencia);
        }
        if ("SALIDA_DEPOT".equals(hitoBusqueda)) {
            if (map.containsKey("SALIDA_AL_DEPOT__" + secuencia)) return map.get("SALIDA_AL_DEPOT__" + secuencia);
        }
        if ("LLEGADA_PLANTA".equals(hitoBusqueda)) {
            if (map.containsKey("LLEGADA_A_PLANTA__" + secuencia)) return map.get("LLEGADA_A_PLANTA__" + secuencia);
        }
        if ("LLEGADA_PUERTO".equals(hitoBusqueda)) {
            if (map.containsKey("ENTREGA_A_PUERTO__" + secuencia)) return map.get("ENTREGA_A_PUERTO__" + secuencia);
        }
        
        return null;
    }

    /**
     * Renderiza evidencias que no coincidieron con ninguna sección estándar.
     * Útil para diagnosticar problemas de hito/secuencia.
     */
    private void renderOtrasEvidencias(StringBuilder html, List<ViajeReporteDetalleDto.EvidenciaInfo> evidencias) {
        html.append("<div class='section'>");
        html.append("<h3 style='background-color: #6b7280;'>OTRAS EVIDENCIAS (Hitos no mapeados)</h3>");
        html.append("<table class='evidencia-table'>");
        html.append("<thead><tr><th style='width:6%'>#</th><th style='width:42%'>Hito / Archivo</th><th style='width:16%'>Tipo</th><th style='width:36%'>Estado</th></tr></thead><tbody>");

        for (int i = 0; i < evidencias.size(); i++) {
            ViajeReporteDetalleDto.EvidenciaInfo ev = evidencias.get(i);
            String rowBg = (i % 2 == 0) ? "" : "background-color:#f9fafb;";
            html.append("<tr style='").append(rowBg).append("'>");
            html.append("<td style='text-align:center;'>").append(i+1).append(".</td>");
            html.append("<td>").append(ev.getHito()).append(" / ").append(ev.getNombreArchivo()).append("</td>");
            html.append("<td style='text-align:center;'>").append(ev.getTipoAdjunto()).append("</td>");
            html.append("<td style='text-align:center;'>Cargada</td></tr>");

            if (ev.getImagenData() != null) {
                html.append("<tr style='").append(rowBg).append("border-bottom:2px solid #e5e7eb;'>");
                html.append("<td colspan='4' style='text-align:center;padding:10px;'>");
                try {
                    byte[] imgResized = resizeImageBytes(ev.getImagenData(), 400, 300);
                    int[] dims = readImageDimensions(imgResized);
                    String b64 = java.util.Base64.getEncoder().encodeToString(imgResized);
                    String mime = (ev.getNombreArchivo() != null && ev.getNombreArchivo().toLowerCase().endsWith(".png")) ? "image/png" : "image/jpeg";
                    html.append("<img src='data:").append(mime).append(";base64,").append(b64).append("' width='").append(dims[0]).append("' height='").append(dims[1]).append("' style='display:block;margin:0 auto;border:1px solid #ccc' />");
                } catch (Exception e) {
                    html.append("[ Error al renderizar imagen ]");
                }
                html.append("</td></tr>");
            }
        }
        html.append("</tbody></table></div>");
    }

    /**
     * Redimensiona una imagen (bytes) a un máximo de maxW×maxH píxeles
     * manteniendo la proporción. Devuelve los bytes originales si no es
     * una imagen reconocible (pdf/svg/etc.).
     */
    private byte[] resizeImageBytes(byte[] originalBytes, int maxW, int maxH) throws Exception {
        try (java.io.InputStream is = new java.io.ByteArrayInputStream(originalBytes)) {
            java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(is);
            if (original == null) {
                return originalBytes;
            }

            int srcW = original.getWidth();
            int srcH = original.getHeight();

            double ratioW = (double) maxW / srcW;
            double ratioH = (double) maxH / srcH;
            double ratio  = Math.min(ratioW, ratioH);

            // Si ya cabe, devolver tal cual
            if (ratio >= 1.0) return originalBytes;

            int dstW = Math.max(1, (int) (srcW * ratio));
            int dstH = Math.max(1, (int) (srcH * ratio));

            java.awt.image.BufferedImage resized =
                new java.awt.image.BufferedImage(dstW, dstH, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2d = resized.createGraphics();
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                                 java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, dstW, dstH);
            g2d.drawImage(original, 0, 0, dstW, dstH, null);
            g2d.dispose();

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(resized, "JPEG", out);
            return out.toByteArray();
        }
    }

    /**
     * Lee las dimensiones reales de una imagen en bytes.
     * Devuelve [width, height]. Si no es imagen, devuelve [320, 240] como fallback.
     */
    private int[] readImageDimensions(byte[] imageBytes) {
        try (java.io.InputStream is = new java.io.ByteArrayInputStream(imageBytes)) {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(is);
            if (img != null) {
                return new int[]{img.getWidth(), img.getHeight()};
            }
        } catch (Exception ignored) {}
        return new int[]{320, 240}; // fallback seguro
    }
}

