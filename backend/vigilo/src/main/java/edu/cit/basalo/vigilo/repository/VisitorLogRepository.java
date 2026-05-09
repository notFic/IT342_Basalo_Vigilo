package edu.cit.basalo.vigilo.repository;

import edu.cit.basalo.vigilo.entity.VisitorLog;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface VisitorLogRepository extends JpaRepository<VisitorLog, Long> {
    List<VisitorLog> findByStatusOrderByTimeInDesc(String status);
    
    Page<VisitorLog> findByStatusNotOrderByTimeInDesc(String status, Pageable pageable);
    
    List<VisitorLog> findByStatusAndExtendedVisitFalse(String status);
}
