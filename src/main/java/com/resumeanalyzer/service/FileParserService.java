package com.resumeanalyzer.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileParserService {

    public String extractText(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new IllegalArgumentException("File has no name.");
        }

        String lowerName = originalName.toLowerCase();

        if (lowerName.endsWith(".pdf")) {
            return extractFromPdf(file);
        } else if (lowerName.endsWith(".docx")) {
            return extractFromDocx(file.getInputStream());
        } else {
            throw new IllegalArgumentException(
                "Unsupported file type. Please upload a PDF or DOCX file."
            );
        }
    }

    // ── PDF Extraction — PDFBox 3.x uses Loader.loadPDF() ────
    private String extractFromPdf(MultipartFile file) throws IOException {
        // PDFBox 3.x requires a byte array, not an InputStream
        byte[] bytes = file.getBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            return cleanText(text);
        }
    }

    // ── DOCX Extraction — Apache POI (unchanged) ──────────────
    private String extractFromDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder sb = new StringBuilder();
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                String text = paragraph.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text).append("\n");
                }
            }
            return cleanText(sb.toString());
        }
    }

    // ── Clean up excessive whitespace ─────────────────────────
    private String cleanText(String raw) {
        if (raw == null) return "";
        return raw
            .replaceAll("\\r\\n", "\n")
            .replaceAll("\\r", "\n")
            .replaceAll("[ \\t]+", " ")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
    }
}