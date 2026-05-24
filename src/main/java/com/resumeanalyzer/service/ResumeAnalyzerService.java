package com.resumeanalyzer.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.resumeanalyzer.dao.ResumeDAO;
import com.resumeanalyzer.dto.ResumeAnalysisDTO;
import com.resumeanalyzer.model.Resume;

@Service
public class ResumeAnalyzerService {

    @Autowired
    private FileParserService fileParserService;

    @Autowired
    private GeminiApiService geminiApiService;

    @Autowired
    private ResumeDAO resumeDAO;

    /**
     * Main method: parses file, calls AI, saves to DB, returns DTO.
     */
    public ResumeAnalysisDTO analyzeResume(MultipartFile file, String interests)
            throws IOException {

        // 1. Extract text from PDF or DOCX
        String extractedText = fileParserService.extractText(file);

        if (extractedText == null || extractedText.isBlank()) {
            throw new IllegalArgumentException(
                "Could not extract any text from the uploaded file. "
                + "Please ensure it is a readable PDF or DOCX."
            );
        }

        // 2. Build the AI prompt
        String prompt = buildAnalysisPrompt(extractedText, interests);

        // 3. Call Gemini AI
        String aiResponse = geminiApiService.generateContent(prompt);

        // 4. Parse the structured AI response
        ResumeAnalysisDTO dto = parseAnalysisResponse(aiResponse);
        dto.setFileName(file.getOriginalFilename());
        dto.setInterests(interests);
        dto.setExtractedText(extractedText);

        // 5. Save to database
        Resume resume = new Resume();
        resume.setFileName(file.getOriginalFilename());
        resume.setExtractedText(extractedText);
        resume.setAtsScore(dto.getAtsScore());
        resume.setSuggestions(String.join("||", dto.getSuggestions()));
        resume.setRecommendedSkills(String.join(", ", dto.getRecommendedSkills()));
        resume.setInterests(interests);

        Resume saved = resumeDAO.save(resume);
        dto.setId(saved.getId());

        return dto;
    }

    /**
     * Retrieves a past analysis from DB and reconstructs the DTO.
     */
    public ResumeAnalysisDTO getAnalysisById(Integer id) {
        Resume resume = resumeDAO.findById(id)
            .orElseThrow(() -> new RuntimeException("Analysis not found: " + id));

        ResumeAnalysisDTO dto = new ResumeAnalysisDTO();
        dto.setId(resume.getId());
        dto.setFileName(resume.getFileName());
        dto.setAtsScore(resume.getAtsScore());
        dto.setInterests(resume.getInterests());

        if (resume.getSuggestions() != null && !resume.getSuggestions().isBlank()) {
            dto.setSuggestions(Arrays.asList(resume.getSuggestions().split("\\|\\|")));
        }
        if (resume.getRecommendedSkills() != null) {
            dto.setRecommendedSkills(
                Arrays.asList(resume.getRecommendedSkills().split(",\\s*"))
            );
        }
        return dto;
    }

    public List<Resume> getAllResumes() {
        return resumeDAO.findAll();
    }

    // ── Prompt Builder ──────────────────────────────────────────
    private String buildAnalysisPrompt(String resumeText, String interests) {
        return """
            You are an expert ATS (Applicant Tracking System) resume analyzer and career coach.
            Analyze the following resume and provide a structured response.

            USER INTERESTS / TARGET ROLES: %s

            RESUME TEXT:
            ---
            %s
            ---

            Provide your analysis in EXACTLY this format (use the exact section headers):

            ATS_SCORE: [number between 0 and 100]

            SUMMARY:
            [2-3 sentence overall assessment of the resume]

            DETECTED_SKILLS:
            [comma-separated list of skills found in the resume]

            SUGGESTIONS:
            - [specific actionable improvement suggestion 1]
            - [specific actionable improvement suggestion 2]
            - [specific actionable improvement suggestion 3]
            - [specific actionable improvement suggestion 4]
            - [specific actionable improvement suggestion 5]

            RECOMMENDED_SKILLS:
            [comma-separated list of 8-10 skills the candidate should learn based on their background and interests]

            Be specific, practical, and encouraging. Focus on what will actually improve ATS scoring.
            """.formatted(
                (interests != null && !interests.isBlank()) ? interests : "Not specified",
                resumeText.length() > 8000 ? resumeText.substring(0, 8000) : resumeText
            );
    }

    // ── Response Parser ─────────────────────────────────────────
    private ResumeAnalysisDTO parseAnalysisResponse(String aiResponse) {
        ResumeAnalysisDTO dto = new ResumeAnalysisDTO();

        try {
            // Parse ATS Score
            dto.setAtsScore(parseIntSection(aiResponse, "ATS_SCORE:"));

            // Parse Summary
            dto.setSummary(parseTextSection(aiResponse, "SUMMARY:", "DETECTED_SKILLS:"));

            // Parse Detected Skills
            String detectedSkillsRaw = parseTextSection(aiResponse, "DETECTED_SKILLS:", "SUGGESTIONS:");
            dto.setDetectedSkills(parseCommaSeparated(detectedSkillsRaw));

            // Parse Suggestions (bullet points)
            String suggestionsRaw = parseTextSection(aiResponse, "SUGGESTIONS:", "RECOMMENDED_SKILLS:");
            dto.setSuggestions(parseBulletPoints(suggestionsRaw));

            // Parse Recommended Skills
            String recommendedRaw = parseTextSection(aiResponse, "RECOMMENDED_SKILLS:", null);
            dto.setRecommendedSkills(parseCommaSeparated(recommendedRaw));

        } catch (Exception e) {
            // Fallback if AI response format is unexpected
            dto.setAtsScore(50);
            dto.setSummary("Analysis completed. Please review the full response.");
            dto.setSuggestions(List.of(
                "Ensure your resume includes relevant keywords from job descriptions.",
                "Add measurable achievements with numbers and percentages.",
                "Use a clean single-column format for better ATS parsing.",
                "Include a professional summary at the top.",
                "Tailor your skills section to match target job requirements."
            ));
            dto.setRecommendedSkills(List.of("Java", "Spring Boot", "MySQL", "REST APIs"));
            dto.setDetectedSkills(List.of());
        }

        return dto;
    }

    private Integer parseIntSection(String text, String header) {
        int idx = text.indexOf(header);
        if (idx == -1) return 50;
        String after = text.substring(idx + header.length()).trim();
        String line = after.split("\\n")[0].trim();
        // Extract first number found
        String numStr = line.replaceAll("[^0-9]", "");
        if (numStr.isEmpty()) return 50;
        int val = Integer.parseInt(numStr.substring(0, Math.min(numStr.length(), 3)));
        return Math.min(100, Math.max(0, val));
    }

    private String parseTextSection(String text, String startHeader, String endHeader) {
        int start = text.indexOf(startHeader);
        if (start == -1) return "";
        start += startHeader.length();
        if (endHeader != null) {
            int end = text.indexOf(endHeader, start);
            if (end != -1) return text.substring(start, end).trim();
        }
        return text.substring(start).trim();
    }

    private List<String> parseCommaSeparated(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toList());
    }

    private List<String> parseBulletPoints(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split("\\n"))
            .map(line -> line.replaceAll("^[-*•]\\s*", "").trim())
            .filter(s -> !s.isBlank())
            .collect(Collectors.toList());
    }
}