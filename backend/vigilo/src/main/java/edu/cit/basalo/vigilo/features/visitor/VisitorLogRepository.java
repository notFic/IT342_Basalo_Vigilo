package edu.cit.basalo.vigilo.features.visitor;

import edu.cit.basalo.vigilo.features.visitor.VisitorLog;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface VisitorLogRepository extends JpaRepository<VisitorLog, Long> {
    List<VisitorLog> findByStatusOrderByTimeInDesc(String status);
    
    Page<VisitorLog> findByStatusNotOrderByTimeInDesc(String status, Pageable pageable);

    Page<VisitorLog> findByStatusNotAndFullNameContainingIgnoreCaseOrderByTimeInDesc(String status, String fullName, Pageable pageable);
    
    List<VisitorLog> findByStatusAndExtendedVisitFalse(String status);
}
