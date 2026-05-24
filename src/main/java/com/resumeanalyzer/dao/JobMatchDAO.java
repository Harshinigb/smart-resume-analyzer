package com.resumeanalyzer.dao;

import java.util.List;
import java.util.Optional;

import com.resumeanalyzer.model.JobMatch;

public interface JobMatchDAO {
    JobMatch save(JobMatch jobMatch);
    Optional<JobMatch> findById(Integer id);
    List<JobMatch> findAll();
    void deleteById(Integer id);
}