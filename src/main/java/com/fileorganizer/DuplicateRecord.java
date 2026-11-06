
package com.fileorganizer;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "duplicate_records")
public class DuplicateRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String duplicateName;
    private String originalName;
    private String category;
    private String sha256;

    private LocalDateTime detectedAt = LocalDateTime.now();

    public DuplicateRecord() {
    }

    public Long getId() {
        return id;
    }

    public String getDuplicateName() {
        return duplicateName;
    }

    public void setDuplicateName(String duplicateName) {
        this.duplicateName = duplicateName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }
}
