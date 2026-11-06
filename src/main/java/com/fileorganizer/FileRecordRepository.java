package com.fileorganizer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface FileRecordRepository
        extends JpaRepository<FileRecord, Long> {

    Optional<FileRecord> findBySha256(String sha256);

    List<FileRecord> findByCategory(String category);

    List<FileRecord> findByNameContainingIgnoreCase(String name);

    Optional<FileRecord> findByNameAndCategory(
            String name, String category);
}