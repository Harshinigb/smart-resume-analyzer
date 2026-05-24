package com.resumeanalyzer.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAnalysisDTO {

    private Integer id;
    private String fileName;

    // ATS score 0-100
    private Integer atsScore;

    // Overall summary paragraph from AI
    private String summary;

    // List of improvement suggestions
    private List<String> suggestions;

    // Skills the AI recommends based on resume + interests
    private List<String> recommendedSkills;

    // Current skills detected in resume
    private List<String> detectedSkills;

    // Interests the user entered
    private String interests;

    // Raw extracted text (not shown in UI, used internally)
    private String extractedText;

    // Score label: Excellent / Good / Average / Needs Work
    public String getScoreLabel() {
        if (atsScore == null) return "Unknown";
        if (atsScore >= 80) return "Excellent";
        if (atsScore >= 60) return "Good";
        if (atsScore >= 40) return "Average";
        return "Needs Work";
    }

    // Score CSS class for colour coding in Thymeleaf
    public String getScoreClass() {
        if (atsScore == null) return "score-unknown";
        if (atsScore >= 80) return "score-excellent";
        if (atsScore >= 60) return "score-good";
        if (atsScore >= 40) return "score-average";
        return "score-poor";
    }
}