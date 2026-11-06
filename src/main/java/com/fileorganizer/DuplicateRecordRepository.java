
package com.fileorganizer;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DuplicateRecordRepository
        extends JpaRepository<DuplicateRecord, Long> {
}
