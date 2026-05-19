package com.example.fleetIq.repository;

import com.example.fleetIq.model.DemoSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemoSessionRepository extends JpaRepository<DemoSession, String> {
    List<DemoSession> findByStatus(String status);
}
