package com.resumeanalyzer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.resumeanalyzer.service.JobMatcherService;
import com.resumeanalyzer.service.ResumeAnalyzerService;

@Controller
public class DashboardController {

    @Autowired
    private ResumeAnalyzerService resumeAnalyzerService;

    @Autowired
    private JobMatcherService jobMatcherService;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("totalAnalyses", resumeAnalyzerService.getAllResumes().size());
        model.addAttribute("totalMatches",  jobMatcherService.getAllMatches().size());
        model.addAttribute("recentResumes", resumeAnalyzerService.getAllResumes()
                .stream().limit(5).toList());
        model.addAttribute("recentMatches", jobMatcherService.getAllMatches()
                .stream().limit(5).toList());
        return "dashboard";
    }
}