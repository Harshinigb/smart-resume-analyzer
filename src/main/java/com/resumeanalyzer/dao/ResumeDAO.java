package com.resumeanalyzer.dao;

import java.util.List;
import java.util.Optional;

import com.resumeanalyzer.model.Resume;

public interface ResumeDAO {
    Resume save(Resume resume);
    Optional<Resume> findById(Integer id);
    List<Resume> findAll();
    void deleteById(Integer id);
}