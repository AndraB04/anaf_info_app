package com.backend.repository;

import com.backend.model.entity.CompanyData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyDataRepository extends JpaRepository<CompanyData, Long> {
    
    Optional<CompanyData> findByCui(String cui);
    
    boolean existsByCui(String cui);
}
