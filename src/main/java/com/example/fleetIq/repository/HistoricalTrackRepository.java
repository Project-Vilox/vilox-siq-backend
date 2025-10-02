package com.example.fleetIq.repository;

import com.example.fleetIq.model.HistoricalTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoricalTrackRepository extends JpaRepository<HistoricalTrack, Long> {
    @Query("SELECT t FROM HistoricalTrack t WHERE t.imei = :imei AND t.time BETWEEN :beginTime AND :endTime ORDER BY t.time ASC")
    List<HistoricalTrack> findByImeiAndTimeBetween(String imei, Long beginTime, Long endTime);
}