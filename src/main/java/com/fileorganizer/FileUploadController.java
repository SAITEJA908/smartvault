
package com.fileorganizer;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final Path uploadDirectory =
            Paths.get(System.getProperty("user.home"),
                    "SmartVault", "uploads");

    private final FileRecordService fileRecordService;
    private final DuplicateRecordRepository duplicateRepository;

    public FileUploadController(
            FileRecordService fileRecordService,
            DuplicateRecordRepository duplicateRepository) {

        this.fileRecordService = fileRecordService;
        this.duplicateRepository = duplicateRepository;
    }

    // Calculate SHA-256 hash
    private String calculateHash(byte[] data) throws Exception {

        MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

        byte[] hashBytes = digest.digest(data);

        StringBuilder result = new StringBuilder();

        for (byte b : hashBytes) {
            result.append(String.format("%02x", b));
        }

        return result.toString();
    }

    // Identify file category
    private String getCategory(String fileName) {

        String extension = "";
        int dot = fileName.lastIndexOf(".");

        if (dot >= 0) {
            extension = fileName.substring(dot + 1).toLowerCase();
        }

        if (Arrays.asList("pdf", "doc", "docx", "txt",
                "ppt", "pptx").contains(extension)) {
            return "Documents";
        }

        if (Arrays.asList("jpg", "jpeg", "png",
                "gif", "webp").contains(extension)) {
            return "Images";
        }

        if (Arrays.asList("mp4", "mkv", "avi").contains(extension)) {
            return "Videos";
        }

        if (Arrays.asList("mp3", "wav").contains(extension)) {
            return "Audio";
        }

        return "Others";
    }

    // Register existing files in MySQL
    private void syncExistingFilesToDatabase() throws Exception {

        if (!Files.exists(uploadDirectory)) {
            return;
        }

        try (var paths = Files.walk(uploadDirectory)) {

            for (Path path : (Iterable<Path>) paths
                    .filter(Files::isRegularFile)::iterator) {

                byte[] data = Files.readAllBytes(path);
                String hash = calculateHash(data);

                if (fileRecordService.findByHash(hash).isEmpty()) {

                    FileRecord record = new FileRecord();

                    record.setName(path.getFileName().toString());
                    record.setCategory(
                            path.getParent().getFileName().toString());
                    record.setFilePath(path.toString());
                    record.setSize(Files.size(path));
                    record.setSha256(hash);

                    fileRecordService.saveFile(record);
                }
            }
        }
    }

    // Upload files and detect duplicates
    @PostMapping("/upload")
    public Map<String, Object> uploadFiles(
            @RequestParam("files") MultipartFile[] files) {

        List<String> uploaded = new ArrayList<>();
        List<String> duplicates = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        try {

            Files.createDirectories(uploadDirectory);

            syncExistingFilesToDatabase();

            for (MultipartFile file : files) {

                if (file.isEmpty()) {
                    failed.add("Empty file");
                    continue;
                }

                String originalName = Paths.get(
                        file.getOriginalFilename() == null
                                ? "unnamed"
                                : file.getOriginalFilename()
                ).getFileName().toString();

                byte[] fileData = file.getBytes();

                String newHash = calculateHash(fileData);

                Optional<FileRecord> existing =
                        fileRecordService.findByHash(newHash);

                // Duplicate found
                if (existing.isPresent()) {

                    FileRecord original = existing.get();

                    DuplicateRecord duplicate =
                            new DuplicateRecord();

                    duplicate.setDuplicateName(originalName);
                    duplicate.setOriginalName(original.getName());
                    duplicate.setCategory(original.getCategory());
                    duplicate.setSha256(newHash);

                    // SAVE DUPLICATE HISTORY IN MYSQL
                    duplicateRepository.save(duplicate);

                    duplicates.add(
                            originalName + " matches " + original.getName()
                    );

                    continue;
                }

                // Categorize file
                String category = getCategory(originalName);

                Path categoryDirectory =
                        uploadDirectory.resolve(category);

                Files.createDirectories(categoryDirectory);

                Path destination =
                        categoryDirectory.resolve(originalName);

                if (Files.exists(destination)) {

                    int dot = originalName.lastIndexOf(".");

                    String baseName = dot >= 0
                            ? originalName.substring(0, dot)
                            : originalName;

                    String extension = dot >= 0
                            ? originalName.substring(dot)
                            : "";

                    destination = categoryDirectory.resolve(
                            baseName + "_" + UUID.randomUUID() + extension
                    );
                }

                // Save file on computer
                Files.write(destination, fileData);

                // Save normal file details in MySQL
                FileRecord record = new FileRecord();

                record.setName(destination.getFileName().toString());
                record.setCategory(category);
                record.setFilePath(destination.toString());
                record.setSize(fileData.length);
                record.setSha256(newHash);

                fileRecordService.saveFile(record);

                uploaded.add(originalName + " → " + category);
            }

        } catch (Exception e) {

            failed.add("Error: " + e.getMessage());
        }

        Map<String, Object> response = new HashMap<>();

        response.put("uploaded", uploaded);
        response.put("duplicates", duplicates);
        response.put("failed", failed);
        response.put("message", "Upload process completed");

        return response;
    }

    // Get all saved files
    @GetMapping("/list")
    public List<Map<String, Object>> getAllFiles()
            throws IOException {

        List<Map<String, Object>> fileList = new ArrayList<>();

        if (!Files.exists(uploadDirectory)) {
            return fileList;
        }

        try (var paths = Files.walk(uploadDirectory)) {

            for (Path path : (Iterable<Path>) paths
                    .filter(Files::isRegularFile)::iterator) {

                Map<String, Object> file = new HashMap<>();

                file.put("name", path.getFileName().toString());
                file.put("category",
                        path.getParent().getFileName().toString());
                file.put("size", Files.size(path));

                fileList.add(file);
            }
        }

        return fileList;
    }

    // Get duplicate history from MySQL
    @GetMapping("/duplicates")
    public List<Map<String, Object>> getDuplicates() {

        List<Map<String, Object>> result = new ArrayList<>();

        List<DuplicateRecord> records =
                duplicateRepository.findAll();

        for (DuplicateRecord record : records) {

            Map<String, Object> item = new HashMap<>();

            // Keep the field names expected by the existing HTML
            item.put("name", record.getDuplicateName());
            item.put("original", record.getOriginalName());
            item.put("category", record.getCategory());
            item.put("sha256", record.getSha256());
            item.put("detectedAt", record.getDetectedAt());

            result.add(item);
        }

        return result;
    }
}
