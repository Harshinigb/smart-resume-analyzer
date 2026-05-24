package com.resumeanalyzer.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "job_matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String resumeFileName;

    @Column(columnDefinition = "LONGTEXT")
    private String jobDescription;

    private Integer matchPercentage;

    @Column(columnDefinition = "TEXT")
    private String missingSkills;

    @Column(columnDefinition = "LONGTEXT")
    private String suggestions;

    private LocalDateTime matchedAt;

    @PrePersist
    public void prePersist() {
        this.matchedAt = LocalDateTime.now();
    }
}