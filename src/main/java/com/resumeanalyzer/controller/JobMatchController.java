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

import com.resumeanalyzer.dto.JobMatchDTO;
import com.resumeanalyzer.service.JobMatcherService;

@Controller
@RequestMapping("/match")
public class JobMatchController {

    @Autowired
    private JobMatcherService jobMatcherService;

    // Show the job matcher form
    @GetMapping
    public String showMatchForm() {
        return "job-match-upload";
    }

    // Handle resume + JD submission and trigger AI match
    @PostMapping
    public String matchJob(
            @RequestParam("resumeFile") MultipartFile resumeFile,
            @RequestParam("jobDescription") String jobDescription,
            Model model,
            RedirectAttributes redirectAttrs) {

        if (resumeFile == null || resumeFile.isEmpty()) {
            redirectAttrs.addFlashAttribute("errorMsg",
                "Please upload your resume (PDF or DOCX).");
            return "redirect:/match";
        }
        if (jobDescription == null || jobDescription.isBlank()) {
            redirectAttrs.addFlashAttribute("errorMsg",
                "Please paste the job description.");
            return "redirect:/match";
        }

        try {
            JobMatchDTO result = jobMatcherService.matchResumeToJob(resumeFile, jobDescription);
            model.addAttribute("match", result);
            return "job-match-result";

        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/match";

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg",
                "Something went wrong. Please try again. Error: " + e.getMessage());
            return "redirect:/match";
        }
    }

    // View a past match result by ID
    @GetMapping("/result/{id}")
    public String viewResult(@PathVariable Integer id, Model model) {
        try {
            JobMatchDTO result = jobMatcherService.getMatchById(id);
            model.addAttribute("match", result);
            return "job-match-result";
        } catch (Exception e) {
            return "redirect:/dashboard";
        }
    }

    // History page - all past match results
    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("matches", jobMatcherService.getAllMatches());
        return "job-match-history";
    }
}