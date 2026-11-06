
package com.fileorganizer;

import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/files")
public class FileAccessController {

    private final Path uploadDirectory =
            Paths.get(System.getProperty("user.home"),
                    "SmartVault", "uploads");

    private final FileRecordService fileRecordService;

    public FileAccessController(
            FileRecordService fileRecordService) {
        this.fileRecordService = fileRecordService;
    }

    // Open file and save access information in MySQL
    @GetMapping("/open/{category}/{fileName:.+}")
    public ResponseEntity<Resource> openFile(
            @PathVariable String category,
            @PathVariable String fileName) throws IOException {

        Path categoryPath =
                uploadDirectory.resolve(category).normalize();

        Path filePath =
                categoryPath.resolve(fileName).normalize();

        if (!categoryPath.startsWith(uploadDirectory.normalize())
                || !filePath.startsWith(categoryPath)) {
            return ResponseEntity.badRequest().build();
        }

        if (!Files.isRegularFile(filePath)) {
            return ResponseEntity.notFound().build();
        }

        // Update access history in MySQL
        fileRecordService.recordAccess(category, fileName);

        Resource resource =
                new FileSystemResource(filePath);

        MediaType mediaType =
                MediaTypeFactory.getMediaType(fileName)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + fileName + "\""
                )
                .body(resource);
    }

    // Display file access history from MySQL
    @GetMapping("/access-stats")
    public List<Map<String, Object>> getAccessStatistics() {

        List<Map<String, Object>> statistics =
                new ArrayList<>();

        List<FileRecord> savedRecords =
                fileRecordService.getAllFiles();

        for (FileRecord file : savedRecords) {

            Map<String, Object> record =
                    new HashMap<>();

            record.put("name", file.getName());
            record.put("category", file.getCategory());
            record.put("accessCount", file.getAccessCount());

            record.put(
                    "lastAccessed",
                    file.getLastAccessed() == null
                            ? "Never"
                            : file.getLastAccessed().toString()
            );

            statistics.add(record);
        }

        statistics.sort((a, b) ->
                Integer.compare(
                        (int) b.get("accessCount"),
                        (int) a.get("accessCount")
                )
        );

        return statistics;
    }
}
