package com.backend.repository;

import com.backend.model.entity.FinancialRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {
    
    List<FinancialRecord> findByCui(String cui);
    
    List<FinancialRecord> findByCuiOrderByYearDesc(String cui);
    
    Optional<FinancialRecord> findByCuiAndYear(String cui, Integer year);
    
    @Query("SELECT fr FROM FinancialRecord fr WHERE fr.cui = :cui AND fr.year BETWEEN :startYear AND :endYear ORDER BY fr.year DESC")
    List<FinancialRecord> findByCuiAndYearRange(@Param("cui") String cui, 
                                               @Param("startYear") Integer startYear, 
                                               @Param("endYear") Integer endYear);
}
