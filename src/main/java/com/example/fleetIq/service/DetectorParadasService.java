// package com.example.fleetIq.service;

// import com.example.fleetIq.model.Track;
// import com.example.fleetIq.dto.ParadaDetectadaDto;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;
// import org.json.JSONArray;
// import org.json.JSONObject;

// import java.io.IOException;
// import java.net.URI;
// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;
// import java.time.Duration;
// import java.time.Instant;
// import java.time.LocalDateTime;
// import java.time.ZoneId;
// import java.util.*;
// import java.util.stream.Collectors;

// @Service
// public class DetectorParadasService {

// @Value("${google.maps.api.key}")
// private String googleMapsApiKey;

// // ⭐ CONFIGURACIÓN MEJORADA
// private static final double VELOCIDAD_MAXIMA_PARADA = 5.0; // km/h (aumentado
// para tolerar errores GPS)
// private static final int TIEMPO_MINIMO_PARADA = 5; // minutos
// private static final double RADIO_AGRUPACION = 100.0; // metros - Puntos
// dentro de este radio se consideran misma
// // parada
// private static final int RADIO_BUSQUEDA_POI = 500; // metros
// private static final long MAX_GAP_SECONDS = 300; // 5 minutos - máximo gap
// entre tracks para considerar misma parada

// // MÉTODO PRINCIPAL MEJORADO: Detecta paradas usando clustering espacial

// public List<ParadaDetectadaDto> detectarParadasConMotivo(List<Track> tracks)
// {
// if (tracks == null || tracks.isEmpty()) {
// System.out.println("⚠️ No hay tracks para analizar");
// return new ArrayList<>();
// }

// // Ordenar tracks por tiempo GPS
// List<Track> tracksSortedByTime = tracks.stream()
// .sorted(Comparator.comparing(Track::getGpstime))
// .collect(Collectors.toList());

// System.out.println("📊 Analizando " + tracksSortedByTime.size() + " tracks
// GPS");
// System.out.println(" 🕐 Desde: " +
// LocalDateTime.ofInstant(Instant.ofEpochSecond(tracksSortedByTime.get(0).getGpstime()),
// ZoneId.systemDefault()));
// System.out.println(" 🕐 Hasta: " +
// LocalDateTime.ofInstant(
// Instant.ofEpochSecond(tracksSortedByTime.get(tracksSortedByTime.size() -
// 1).getGpstime()),
// ZoneId.systemDefault()));

// // 1. DETECTAR CLUSTERS (grupos de puntos GPS cercanos en el espacio)
// List<ClusterParada> clusters =
// detectarClustersEspaciales(tracksSortedByTime);
// System.out.println("🔍 Clusters espaciales detectados: " + clusters.size());

// // 2. FILTRAR PARADAS VÁLIDAS (duración mínima)
// List<ClusterParada> paradasValidas = clusters.stream()
// .filter(c -> c.getDuracionMinutos() >= TIEMPO_MINIMO_PARADA)
// .collect(Collectors.toList());

// System.out.println("🛑 Paradas válidas (≥" + TIEMPO_MINIMO_PARADA + " min): "
// + paradasValidas.size());

// // 3. CONVERTIR A DTOs CON ANÁLISIS COMPLETO
// List<ParadaDetectadaDto> paradasDetectadas = new ArrayList<>();

// for (int i = 0; i < paradasValidas.size(); i++) {
// ClusterParada cluster = paradasValidas.get(i);
// ParadaDetectadaDto parada = new ParadaDetectadaDto();

// // Datos básicos
// parada.setLatitud(cluster.getLatitudCentro());
// parada.setLongitud(cluster.getLongitudCentro());
// parada.setInicio(cluster.getHoraInicio());
// parada.setFin(cluster.getHoraFin());
// parada.setDuracionMinutos(cluster.getDuracionMinutos());

// // Determinar motivo
// String motivo = determinarMotivoParada(cluster, tracksSortedByTime);
// parada.setMotivo(motivo);
// parada.setCategoria(categorizarMotivo(motivo));

// // Calcular severidad
// parada.calcularSeveridad();

// // Buscar lugares cercanos
// List<LugarCercano> lugaresCercanos = buscarLugaresCercanos(
// cluster.getLatitudCentro(),
// cluster.getLongitudCentro());
// parada.setLugaresCercanos(lugaresCercanos);

// paradasDetectadas.add(parada);

// // LOG detallado
// System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
// System.out.println("🛑 PARADA #" + (i + 1) + ":");
// System.out.println(" 📍 Ubicación: [" + String.format("%.6f",
// parada.getLatitud()) +
// ", " + String.format("%.6f", parada.getLongitud()) + "]");
// System.out.println(" ⏱️ Duración: " + parada.getDuracionMinutos() + "
// minutos");
// System.out.println(" 🕐 Inicio: " + parada.getInicio());
// System.out.println(" 🕐 Fin: " + parada.getFin());
// System.out.println(" 📊 Puntos GPS: " + cluster.getTracks().size());
// System.out.println(" 💡 Motivo: " + parada.getMotivo());
// System.out.println(" 📂 Categoría: " + parada.getCategoria());
// System.out.println(" 🚨 Severidad: " + parada.getSeveridad());

// if (!lugaresCercanos.isEmpty()) {
// System.out.println(" 🏪 Lugares cercanos:");
// lugaresCercanos.forEach(lugar -> System.out.println(
// " - " + lugar.nombre + " (" + lugar.tipo + ") a " + lugar.distancia + "m"));
// }
// System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
// }

// return paradasDetectadas;
// }

// // ALGORITMO DE CLUSTERING ESPACIAL: Agrupa puntos GPS cercanos
// // Similar a DBSCAN simplificado

// private List<ClusterParada> detectarClustersEspaciales(List<Track> tracks) {
// List<ClusterParada> clusters = new ArrayList<>();
// boolean[] procesado = new boolean[tracks.size()];

// for (int i = 0; i < tracks.size(); i++) {
// if (procesado[i])
// continue;

// Track trackActual = tracks.get(i);
// ClusterParada cluster = new ClusterParada();
// cluster.agregarTrack(trackActual);
// procesado[i] = true;

// // Buscar tracks cercanos espacialmente Y temporalmente
// for (int j = i + 1; j < tracks.size(); j++) {
// if (procesado[j])
// continue;

// Track trackComparar = tracks.get(j);

// // VALIDACIÓN 1: Gap temporal no muy grande
// long gapTemporal = trackComparar.getGpstime() - trackActual.getGpstime();
// if (gapTemporal > MAX_GAP_SECONDS) {
// // Si hay un gap muy grande, probablemente ya salió de la parada
// break;
// }

// // VALIDACIÓN 2: Distancia espacial
// double distancia = calcularDistancia(
// trackActual.getLatitude(), trackActual.getLongitude(),
// trackComparar.getLatitude(), trackComparar.getLongitude()) * 1000; //
// convertir a metros

// // VALIDACIÓN 3: Velocidad baja (criterio adicional)
// double velocidadComparar = trackComparar.getSpeed() != null ?
// trackComparar.getSpeed() : 0.0;

// // Si está dentro del radio Y tiene velocidad baja, agregarlo al cluster
// if (distancia <= RADIO_AGRUPACION && velocidadComparar <=
// VELOCIDAD_MAXIMA_PARADA) {
// cluster.agregarTrack(trackComparar);
// procesado[j] = true;
// trackActual = trackComparar; // Actualizar punto de referencia
// }
// // Si está lejos y tiene velocidad alta, probablemente ya salió
// else if (distancia > RADIO_AGRUPACION * 2 && velocidadComparar >
// VELOCIDAD_MAXIMA_PARADA) {
// break;
// }
// }

// // Solo agregar si tiene al menos 2 puntos
// if (cluster.getTracks().size() >= 2) {
// cluster.calcularMetricas();
// clusters.add(cluster);
// }
// }

// return clusters;
// }

// // HEURÍSTICAS MEJORADAS para determinar motivo de parada

// private String determinarMotivoParada(ClusterParada cluster, List<Track>
// todosLosTracks) {
// int duracion = cluster.getDuracionMinutos();
// int hora = cluster.getHoraInicio().getHour();
// int diaSemana = cluster.getHoraInicio().getDayOfWeek().getValue(); //
// 1=Lunes, 7=Domingo

// // HEURÍSTICA 1: Paradas nocturnas largas (descanso obligatorio)
// if ((hora >= 22 || hora <= 5) && duracion >= 360) { // +6 horas
// return "Descanso nocturno obligatorio";
// }

// // HEURÍSTICA 2: Horarios de comida
// if ((hora >= 7 && hora <= 9) && duracion >= 15 && duracion <= 60) {
// return "Desayuno del conductor";
// }
// if ((hora >= 12 && hora <= 14) && duracion >= 20 && duracion <= 90) {
// return "Almuerzo del conductor";
// }
// if ((hora >= 19 && hora <= 21) && duracion >= 20 && duracion <= 90) {
// return "Cena del conductor";
// }

// // HEURÍSTICA 3: Paradas en horario laboral de lunes a viernes
// (carga/descarga)
// if (diaSemana >= 1 && diaSemana <= 5 &&
// hora >= 8 && hora <= 18 &&
// duracion >= 30 && duracion <= 480) {
// return "Operación de carga/descarga";
// }

// // HEURÍSTICA 4: Paradas cortas (combustible, baño)
// if (duracion >= 5 && duracion <= 20) {
// return "Parada técnica (combustible/baño)";
// }
// if (duracion >= 20 && duracion <= 45) {
// return "Abastecimiento de combustible";
// }

// // HEURÍSTICA 5: Paradas medias (puede ser tráfico o espera)
// if (duracion >= 15 && duracion <= 60) {
// // Analizar velocidad promedio antes de la parada
// Track primerTrack = cluster.getTracks().get(0);
// int indiceInicio = todosLosTracks.indexOf(primerTrack);

// if (indiceInicio > 10) {
// List<Track> tracksAnteriores = todosLosTracks.subList(
// Math.max(0, indiceInicio - 10), indiceInicio);

// double velocidadPromAnterior = tracksAnteriores.stream()
// .mapToDouble(t -> t.getSpeed() != null ? t.getSpeed() : 0)
// .average()
// .orElse(0);

// // Si venía a velocidad de carretera, probablemente sea tráfico/peaje
// if (velocidadPromAnterior > 50) {
// return "Tráfico vehicular / Peaje / Control";
// }
// // Si venía lento, puede ser búsqueda de estacionamiento
// else if (velocidadPromAnterior < 20) {
// return "Búsqueda de estacionamiento / Maniobras";
// }
// }
// }

// // HEURÍSTICA 6: Paradas largas fuera de horario
// if (duracion >= 60 && duracion <= 180) {
// return "Espera prolongada / Posible avería";
// }

// // HEURÍSTICA 7: Paradas muy largas (problema o pernocta)
// if (duracion >= 180 && duracion <= 600) {
// if (hora >= 20 || hora <= 6) {
// return "Pernocta / Descanso prolongado";
// }
// return "Espera prolongada / Posible avería mecánica";
// }

// // HEURÍSTICA 8: Paradas extremadamente largas
// if (duracion > 600) {
// return "Vehículo inactivo por tiempo extendido";
// }

// // DEFAULT
// return "Parada sin clasificar (" + duracion + " min)";
// }

// /**
// * Categorizar el motivo
// */
// private String categorizarMotivo(String motivo) {
// String motivoLower = motivo.toLowerCase();

// if (motivoLower.contains("almuerzo") || motivoLower.contains("desayuno") ||
// motivoLower.contains("cena") || motivoLower.contains("comida")) {
// return "ALIMENTACIÓN";
// }
// if (motivoLower.contains("combustible") || motivoLower.contains("gasolina")
// ||
// motivoLower.contains("abastecimiento")) {
// return "ABASTECIMIENTO";
// }
// if (motivoLower.contains("tráfico") || motivoLower.contains("congestión") ||
// motivoLower.contains("peaje") || motivoLower.contains("control")) {
// return "TRÁFICO";
// }
// if (motivoLower.contains("descanso") || motivoLower.contains("nocturno") ||
// motivoLower.contains("pernocta")) {
// return "DESCANSO_LEGAL";
// }
// if (motivoLower.contains("avería") || motivoLower.contains("emergencia") ||
// motivoLower.contains("problema")) {
// return "EMERGENCIA";
// }
// if (motivoLower.contains("carga") || motivoLower.contains("descarga") ||
// motivoLower.contains("operación")) {
// return "OPERATIVO";
// }
// if (motivoLower.contains("baño") || motivoLower.contains("técnica") ||
// motivoLower.contains("descanso")) {
// return "NECESIDAD_FISIOLOGICA";
// }
// if (motivoLower.contains("estacionamiento") ||
// motivoLower.contains("maniobras")) {
// return "MANIOBRAS";
// }
// return "OTROS";
// }

// /**
// * Buscar lugares cercanos usando Google Places API
// */
// private List<LugarCercano> buscarLugaresCercanos(double lat, double lon) {
// List<LugarCercano> lugares = new ArrayList<>();

// try {
// if (googleMapsApiKey == null || googleMapsApiKey.isEmpty()) {
// System.err.println("❌ API KEY DE GOOGLE MAPS NO CONFIGURADA");
// return lugares;
// }

// String url = String.format(Locale.US,
// "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
// "location=%f,%f&radius=%d&key=%s",
// lat, lon, RADIO_BUSQUEDA_POI, googleMapsApiKey);

// HttpClient client = HttpClient.newBuilder()
// .connectTimeout(Duration.ofSeconds(5))
// .build();

// HttpRequest request = HttpRequest.newBuilder()
// .uri(URI.create(url))
// .timeout(Duration.ofSeconds(10))
// .GET()
// .build();

// HttpResponse<String> response = client.send(request,
// HttpResponse.BodyHandlers.ofString());

// if (response.statusCode() == 200) {
// JSONObject json = new JSONObject(response.body());
// String status = json.optString("status", "UNKNOWN");

// if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
// System.err.println(" ⚠️ Google Places API Error: " + status);
// return lugares;
// }

// JSONArray results = json.optJSONArray("results");
// if (results == null || results.length() == 0) {
// return lugares;
// }

// for (int i = 0; i < Math.min(5, results.length()); i++) {
// JSONObject place = results.getJSONObject(i);

// LugarCercano lugar = new LugarCercano();
// lugar.nombre = place.getString("name");
// lugar.tipo = getTipoPrincipal(place.getJSONArray("types"));
// lugar.direccion = place.optString("vicinity", "N/A");

// JSONObject location =
// place.getJSONObject("geometry").getJSONObject("location");
// double distancia = calcularDistancia(
// lat, lon,
// location.getDouble("lat"),
// location.getDouble("lng"));
// lugar.distancia = (int) (distancia * 1000);

// lugares.add(lugar);
// }
// }
// } catch (Exception e) {
// System.err.println("❌ Error buscando lugares: " + e.getMessage());
// }

// return lugares;
// }

// /**
// * Calcular distancia entre dos puntos (Haversine) - Retorna KM
// */
// private double calcularDistancia(double lat1, double lon1, double lat2,
// double lon2) {
// final int R = 6371; // Radio de la Tierra en km

// double latDistance = Math.toRadians(lat2 - lat1);
// double lonDistance = Math.toRadians(lon2 - lon1);

// double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
// + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
// * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

// double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

// return R * c;
// }

// /**
// * Obtener tipo principal de un lugar
// */
// private String getTipoPrincipal(JSONArray types) {
// Map<String, String> tiposAmigables = Map.of(
// "restaurant", "Restaurante",
// "gas_station", "Gasolinera",
// "parking", "Estacionamiento",
// "lodging", "Hotel/Hospedaje",
// "store", "Tienda",
// "cafe", "Cafetería",
// "police", "Comisaría",
// "hospital", "Centro Médico");

// for (int i = 0; i < types.length(); i++) {
// String tipo = types.getString(i);
// if (tiposAmigables.containsKey(tipo)) {
// return tiposAmigables.get(tipo);
// }
// }

// return types.length() > 0 ? types.getString(0) : "Lugar";
// }

// // ========================================================================
// // CLASE AUXILIAR: ClusterParada (reemplaza GrupoParada)
// // ========================================================================

// private static class ClusterParada {
// private List<Track> tracks = new ArrayList<>();
// private double latitudCentro;
// private double longitudCentro;
// private LocalDateTime horaInicio;
// private LocalDateTime horaFin;
// private int duracionMinutos;

// public void agregarTrack(Track track) {
// tracks.add(track);
// }

// public void calcularMetricas() {
// if (tracks.isEmpty())
// return;

// // Centroide (promedio de coordenadas)
// latitudCentro = tracks.stream()
// .mapToDouble(Track::getLatitude)
// .average()
// .orElse(0);

// longitudCentro = tracks.stream()
// .mapToDouble(Track::getLongitude)
// .average()
// .orElse(0);

// // Ordenar por tiempo para obtener inicio/fin
// tracks.sort(Comparator.comparing(Track::getGpstime));

// Track primero = tracks.get(0);
// Track ultimo = tracks.get(tracks.size() - 1);

// horaInicio = LocalDateTime.ofInstant(
// Instant.ofEpochSecond(primero.getGpstime()),
// ZoneId.systemDefault());

// horaFin = LocalDateTime.ofInstant(
// Instant.ofEpochSecond(ultimo.getGpstime()),
// ZoneId.systemDefault());

// duracionMinutos = (int) java.time.Duration.between(horaInicio,
// horaFin).toMinutes();
// }

// // Getters
// public List<Track> getTracks() {
// return tracks;
// }

// public double getLatitudCentro() {
// return latitudCentro;
// }

// public double getLongitudCentro() {
// return longitudCentro;
// }

// public LocalDateTime getHoraInicio() {
// return horaInicio;
// }

// public LocalDateTime getHoraFin() {
// return horaFin;
// }

// public int getDuracionMinutos() {
// return duracionMinutos;
// }
// }

// public static class LugarCercano {
// public String nombre;
// public String tipo;
// public String direccion;
// public int distancia; // en metros
// }
// }