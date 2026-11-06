
package com.fileorganizer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FileRecordService {

    private final FileRecordRepository repository;

    public FileRecordService(FileRecordRepository repository) {
        this.repository = repository;
    }

    public FileRecord saveFile(FileRecord file) {
        return repository.save(file);
    }

    public List<FileRecord> getAllFiles() {
        return repository.findAll();
    }

    public Optional<FileRecord> findByHash(String hash) {
        return repository.findBySha256(hash);
    }

    @Transactional
    public void recordAccess(String category, String fileName) {

        Optional<FileRecord> result =
                repository.findByNameAndCategory(fileName, category);

        if (result.isPresent()) {

            FileRecord file = result.get();

            file.setAccessCount(file.getAccessCount() + 1);

            file.setLastAccessed(LocalDateTime.now());

            repository.save(file);
        }
    }

    public List<FileRecord> getAccessHistory() {
        return repository.findAll()
                .stream()
                .sorted((a, b) ->
                        Integer.compare(
                                b.getAccessCount(),
                                a.getAccessCount()
                        ))
                .toList();
    }
}
