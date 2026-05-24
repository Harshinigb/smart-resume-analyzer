package com.resumeanalyzer.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.resumeanalyzer.dao.JobMatchDAO;
import com.resumeanalyzer.dto.JobMatchDTO;
import com.resumeanalyzer.model.JobMatch;

@Service
public class JobMatcherService {

    @Autowired
    private FileParserService fileParserService;

    @Autowired
    private GeminiApiService geminiApiService;

    @Autowired
    private JobMatchDAO jobMatchDAO;

    /**
     * Main method: parse resume, compare with JD via AI, save result, return DTO.
     */
    public JobMatchDTO matchResumeToJob(MultipartFile resumeFile, String jobDescription)
            throws IOException {

        // 1. Extract text from uploaded resume
        String resumeText = fileParserService.extractText(resumeFile);

        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException(
                "Could not extract text from the resume. Please upload a readable PDF or DOCX."
            );
        }
        if (jobDescription == null || jobDescription.isBlank()) {
            throw new IllegalArgumentException("Job description cannot be empty.");
        }

        // 2. Build prompt and call Groq AI
        String prompt = buildMatchPrompt(resumeText, jobDescription);
        String aiResponse = geminiApiService.generateContent(prompt);

        // 3. Parse AI response into DTO
        JobMatchDTO dto = parseMatchResponse(aiResponse);
        dto.setResumeFileName(resumeFile.getOriginalFilename());
        dto.setJobDescription(jobDescription);

        // 4. Save to database
        // Store matchedSkills in DB as well so history view can show it
        JobMatch entity = new JobMatch();
        entity.setResumeFileName(resumeFile.getOriginalFilename());
        entity.setJobDescription(jobDescription);
        entity.setMatchPercentage(dto.getMatchPercentage());
        entity.setMissingSkills(
            dto.getMissingSkills() != null && !dto.getMissingSkills().isEmpty()
                ? String.join(", ", dto.getMissingSkills()) : ""
        );
        entity.setSuggestions(
            dto.getSuggestions() != null && !dto.getSuggestions().isEmpty()
                ? String.join("||", dto.getSuggestions()) : ""
        );

        JobMatch saved = jobMatchDAO.save(entity);
        dto.setId(saved.getId());

        return dto;
    }

    /**
     * Retrieves a past match result from DB.
     * All lists are guaranteed to be non-null.
     */
    public JobMatchDTO getMatchById(Integer id) {
        JobMatch entity = jobMatchDAO.findById(id)
            .orElseThrow(() -> new RuntimeException("Match not found: " + id));

        JobMatchDTO dto = new JobMatchDTO();
        dto.setId(entity.getId());
        dto.setResumeFileName(entity.getResumeFileName());
        dto.setJobDescription(entity.getJobDescription());
        dto.setMatchPercentage(entity.getMatchPercentage());

        // ── Safe parsing — always returns a list, never null ──
        dto.setMissingSkills(parseSafe(entity.getMissingSkills(), ","));
        dto.setSuggestions(parseSafe(entity.getSuggestions(), "\\|\\|"));

        // matchedSkills is not stored in DB — set empty list safely
        dto.setMatchedSkills(new ArrayList<>());

        // Set a default summary for history views
        dto.setSummary("This is a saved match result. Run a new match for full AI analysis.");

        return dto;
    }

    /**
     * Returns all past job matches from DB.
     */
    public List<JobMatch> getAllMatches() {
        return jobMatchDAO.findAll();
    }

    // ── Prompt Builder ─────────────────────────────────────────
    private String buildMatchPrompt(String resumeText, String jobDescription) {
        return """
            You are an expert technical recruiter and career coach.
            Compare the following resume against the job description and provide a detailed analysis.

            RESUME:
            ---
            %s
            ---

            JOB DESCRIPTION:
            ---
            %s
            ---

            Provide your analysis in EXACTLY this format:

            MATCH_PERCENTAGE: [number between 0 and 100]

            SUMMARY:
            [2-3 sentence overall assessment of how well this candidate fits the role]

            MATCHED_SKILLS:
            [comma-separated list of skills/requirements from JD that the candidate has]

            MISSING_SKILLS:
            [comma-separated list of skills/requirements from JD that the candidate lacks]

            SUGGESTIONS:
            - [specific thing to add or improve in the resume before applying]
            - [specific thing to add or improve in the resume before applying]
            - [specific thing to add or improve in the resume before applying]
            - [specific thing to add or improve in the resume before applying]
            - [specific thing to add or improve in the resume before applying]

            Be honest, specific, and constructive. Focus on actionable improvements.
            """.formatted(
                resumeText.length() > 6000
                    ? resumeText.substring(0, 6000) : resumeText,
                jobDescription.length() > 3000
                    ? jobDescription.substring(0, 3000) : jobDescription
            );
    }

    // ── Response Parser ────────────────────────────────────────
    private JobMatchDTO parseMatchResponse(String aiResponse) {
        JobMatchDTO dto = new JobMatchDTO();

        try {
            dto.setMatchPercentage(parseIntSection(aiResponse, "MATCH_PERCENTAGE:"));
            dto.setSummary(parseTextSection(aiResponse, "SUMMARY:", "MATCHED_SKILLS:"));

            String matchedRaw = parseTextSection(aiResponse, "MATCHED_SKILLS:", "MISSING_SKILLS:");
            dto.setMatchedSkills(parseCommaSeparated(matchedRaw));

            String missingRaw = parseTextSection(aiResponse, "MISSING_SKILLS:", "SUGGESTIONS:");
            dto.setMissingSkills(parseCommaSeparated(missingRaw));

            String suggestionsRaw = parseTextSection(aiResponse, "SUGGESTIONS:", null);
            dto.setSuggestions(parseBulletPoints(suggestionsRaw));

        } catch (Exception e) {
            // Fallback — always return valid non-null lists
            dto.setMatchPercentage(0);
            dto.setSummary("Could not fully parse the AI response. Please try again.");
            dto.setMatchedSkills(new ArrayList<>());
            dto.setMissingSkills(new ArrayList<>());
            dto.setSuggestions(new ArrayList<>(List.of("Please try submitting again.")));
        }

        return dto;
    }

    // ── Safe DB string → List parser ───────────────────────────
    private List<String> parseSafe(String value, String delimiter) {
        if (value == null || value.isBlank()) return new ArrayList<>();
        return Arrays.stream(value.split(delimiter))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toList());
    }

    // ── Section Parsers ────────────────────────────────────────
    private Integer parseIntSection(String text, String header) {
        int idx = text.indexOf(header);
        if (idx == -1) return 0;
        String after = text.substring(idx + header.length()).trim();
        String line  = after.split("\\n")[0].trim();
        String numStr = line.replaceAll("[^0-9]", "");
        if (numStr.isEmpty()) return 0;
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
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toList());
    }

    private List<String> parseBulletPoints(String raw) {
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        return Arrays.stream(raw.split("\\n"))
            .map(line -> line.replaceAll("^[-*•\\d+\\.]\\s*", "").trim())
            .filter(s -> !s.isBlank())
            .collect(Collectors.toList());
    }
}