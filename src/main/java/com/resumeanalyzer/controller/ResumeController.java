package com.resumeanalyzer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.resumeanalyzer.dto.ResumeAnalysisDTO;
import com.resumeanalyzer.service.ResumeAnalyzerService;

@Controller
@RequestMapping("/resume")
public class ResumeController {

    @Autowired
    private ResumeAnalyzerService resumeAnalyzerService;

    // Show the resume upload form
    @GetMapping("/analyze")
    public String showAnalyzeForm() {
        return "resume-upload";
    }

    // Handle the upload and trigger AI analysis
    @PostMapping("/analyze")
    public String analyzeResume(
            @RequestParam("resumeFile") MultipartFile file,
            @RequestParam(value = "interests", required = false) String interests,
            Model model,
            RedirectAttributes redirectAttrs) {

        // Validate file
        if (file == null || file.isEmpty()) {
            redirectAttrs.addFlashAttribute("errorMsg",
                "Please select a PDF or DOCX file to upload.");
            return "redirect:/resume/analyze";
        }

        try {
            ResumeAnalysisDTO result = resumeAnalyzerService.analyzeResume(file, interests);
            model.addAttribute("analysis", result);
            return "resume-result";

        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/resume/analyze";

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg",
                "Something went wrong during analysis. Please try again. Error: "
                + e.getMessage());
            return "redirect:/resume/analyze";
        }
    }

    // View a past analysis by ID
    @GetMapping("/result/{id}")
    public String viewResult(@PathVariable Integer id, Model model) {
        try {
            ResumeAnalysisDTO result = resumeAnalyzerService.getAnalysisById(id);
            model.addAttribute("analysis", result);
            return "resume-result";
        } catch (Exception e) {
            model.addAttribute("errorMsg", "Analysis not found.");
            return "redirect:/dashboard";
        }
    }

    // History page - all past analyses
    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("resumes", resumeAnalyzerService.getAllResumes());
        return "resume-history";
    }
}