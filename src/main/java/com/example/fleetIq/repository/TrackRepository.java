package com.example.fleetIq.repository;

import com.example.fleetIq.model.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackRepository extends JpaRepository<Track, Long> {
    // Existing methods (unchanged)
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

    List<Track> findByImei(String imei);

    @Query("SELECT t FROM Track t ORDER BY t.gpstime DESC")
    List<Track> findAllOrderByGpstimeDesc();

    @Query("SELECT t FROM Track t WHERE t.imei = :imei AND t.gpstime BETWEEN :beginTime AND :endTime ORDER BY t.gpstime ASC")
    List<Track> findByImeiAndTimeBetween(@Param("imei") String imei, @Param("beginTime") Long beginTime, @Param("endTime") Long endTime);

    @Query("SELECT t FROM Track t WHERE t.gpstime >= :timestamp AND t.id IN (SELECT MAX(t2.id) FROM Track t2 GROUP BY t2.imei)")
    List<Track> findLatestTracksByImeiWithinLastMinutes(@Param("timestamp") Long timestamp);

    @Query("SELECT t FROM Track t INNER JOIN Vehiculo v ON t.imei = v.imei WHERE v.id = :vehiculoId AND t.hearttime BETWEEN :beginTime AND :endTime ORDER BY t.hearttime ASC")
    List<Track> findByVehiculoIdAndHearttimeBetween(@Param("vehiculoId") String vehiculoId, @Param("beginTime") Long beginTime, @Param("endTime") Long endTime);

    // New method for latest track by vehiculoId
    @Query("SELECT t FROM Track t INNER JOIN Vehiculo v ON t.imei = v.imei WHERE v.id = :vehiculoId ORDER BY t.hearttime DESC")
    List<Track> findLatestTrackByVehiculoId(@Param("vehiculoId") String vehiculoId);

    // NEW: Find latest tracks within last minutes with alarm_status = 'PENDING'
    @Query("SELECT t FROM Track t WHERE t.gpstime >= :timestamp AND t.alarmStatus = 'PENDING' AND t.id IN (SELECT MAX(t2.id) FROM Track t2 GROUP BY t2.imei)")
    List<Track> findLatestPendingTracksByImeiWithinLastMinutes(@Param("timestamp") Long timestamp);

    List<Track> findByAlarmStatus(String alarmStatus);

    Track findByImeiAndGpstimeAndLatitudeAndLongitudeAndGpstimeBetween(String imei, Long gpstime, Double latitude, Double longitude, Long startOfDay, Long endOfDay);
}