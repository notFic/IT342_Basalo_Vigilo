package edu.cit.basalo.vigilo.repository;

import edu.cit.basalo.vigilo.entity.VisitorLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitorLogRepository extends JpaRepository<VisitorLog, Long> {
    List<VisitorLog> findByStatusOrderByTimeInDesc(String status);
}
