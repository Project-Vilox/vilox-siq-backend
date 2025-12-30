package com.example.fleetIq.repository;

import com.example.fleetIq.model.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackRepository extends JpaRepository<Track, Long> {

        // ✅ CORREGIDO: Usar gpstime en lugar de hearttime
        @Query("SELECT t FROM Track t INNER JOIN Vehiculo v ON t.imei = v.imei " +
                        "WHERE v.id = :vehiculoId " +
                        "AND t.gpstime BETWEEN :beginTime AND :endTime " +
                        "ORDER BY t.gpstime ASC")
        List<Track> findByVehiculoIdAndHearttimeBetween(
                        @Param("vehiculoId") String vehiculoId,
                        @Param("beginTime") Long beginTime,
                        @Param("endTime") Long endTime);

        // ✅ CORREGIDO: Latest track también debe usar gpstime
        @Query("SELECT t FROM Track t INNER JOIN Vehiculo v ON t.imei = v.imei " +
                        "WHERE v.id = :vehiculoId " +
                        "ORDER BY t.gpstime DESC")
        List<Track> findLatestTrackByVehiculoId(@Param("vehiculoId") String vehiculoId);

        // Resto de métodos sin cambios...
        @Query("SELECT t FROM Track t WHERE t.id IN " +
                        "(SELECT MAX(t2.id) FROM Track t2 GROUP BY t2.imei)")
        List<Track> findLatestTracksByImei();

        @Query("SELECT t FROM Track t WHERE t.gpstime IN " +
                        "(SELECT MAX(t2.gpstime) FROM Track t2 GROUP BY t2.imei)")
        List<Track> findLatestTracksByImeiAndTime();

        @Query("SELECT t FROM Track t WHERE t.gpstime > :timestamp")
        List<Track> findActiveTracksSince(@Param("timestamp") Long timestamp);

        @Query("SELECT t FROM Track t WHERE t.imei = :imei ORDER BY t.id DESC LIMIT 1")
        Track findLatestTrackByImei(@Param("imei") String imei);

        @Query(value = """
                            SELECT * FROM tracks
                            WHERE imei = :imei
                            AND gpstime BETWEEN :startTime AND :endTime
                            AND latitude IS NOT NULL
                            AND longitude IS NOT NULL
                            ORDER BY gpstime DESC, id DESC
                            LIMIT 1
                        """, nativeQuery = true)
        Track findLatestTrackByImeiInTimeRange(
                        @Param("imei") String imei,
                        @Param("startTime") Long startTime,
                        @Param("endTime") Long endTime);

        List<Track> findByImei(String imei);

        @Query("SELECT t FROM Track t ORDER BY t.gpstime DESC")
        List<Track> findAllOrderByGpstimeDesc();

        @Query("SELECT t FROM Track t WHERE t.imei = :imei " +
                        "AND t.gpstime BETWEEN :beginTime AND :endTime " +
                        "ORDER BY t.gpstime ASC")
        List<Track> findByImeiAndTimeBetween(
                        @Param("imei") String imei,
                        @Param("beginTime") Long beginTime,
                        @Param("endTime") Long endTime);

        @Query("SELECT t FROM Track t WHERE t.gpstime >= :timestamp " +
                        "AND t.id IN (SELECT MAX(t2.id) FROM Track t2 GROUP BY t2.imei)")
        List<Track> findLatestTracksByImeiWithinLastMinutes(@Param("timestamp") Long timestamp);

        @Query("SELECT t FROM Track t WHERE t.gpstime >= :timestamp " +
                        "AND t.alarmStatus = 'PENDING' " +
                        "AND t.id IN (SELECT MAX(t2.id) FROM Track t2 GROUP BY t2.imei)")
        List<Track> findLatestPendingTracksByImeiWithinLastMinutes(@Param("timestamp") Long timestamp);

        List<Track> findByAlarmStatus(String alarmStatus);

        Track findByImeiAndGpstimeAndLatitudeAndLongitudeAndGpstimeBetween(
                        String imei, Long gpstime, Double latitude,
                        Double longitude, Long startOfDay, Long endOfDay);

        @Query("SELECT t FROM Track t WHERE t.imei = ?1 " +
                        "AND t.gpstime >= ?2 AND t.gpstime <= ?3")
        List<Track> findLatestTracksByImeiInTimeRange(String imei, long inicio, long fin);

        @Query(value = "SELECT * FROM tracks WHERE imei = :imei AND gpstime >= :desde AND gpstime <= :hasta ORDER BY gpstime ASC", nativeQuery = true)
        List<Track> findTracksByImeiInTimeRange(@Param("imei") String imei, @Param("desde") Long desde,
                        @Param("hasta") Long hasta);
}