package com.resumeanalyzer.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchDTO {

    private Integer id;
    private String resumeFileName;
    private String jobDescription;
    private Integer matchPercentage;

    // ── Always initialize to empty list — never null ──────────
    private List<String> missingSkills  = new ArrayList<>();
    private List<String> matchedSkills  = new ArrayList<>();
    private List<String> suggestions    = new ArrayList<>();

    private String summary;

    public String getMatchLabel() {
        if (matchPercentage == null) return "Unknown";
        if (matchPercentage >= 80) return "Strong Match";
        if (matchPercentage >= 60) return "Good Match";
        if (matchPercentage >= 40) return "Partial Match";
        return "Low Match";
    }

    public String getMatchClass() {
        if (matchPercentage == null) return "match-unknown";
        if (matchPercentage >= 80) return "match-strong";
        if (matchPercentage >= 60) return "match-good";
        if (matchPercentage >= 40) return "match-partial";
        return "match-low";
    }
}