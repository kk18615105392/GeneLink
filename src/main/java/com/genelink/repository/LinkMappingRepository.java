package com.genelink.repository;

import com.genelink.entity.LinkMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LinkMappingRepository extends JpaRepository<LinkMapping, Long> {

    Optional<LinkMapping> findByShortCode(String shortCode);
}
