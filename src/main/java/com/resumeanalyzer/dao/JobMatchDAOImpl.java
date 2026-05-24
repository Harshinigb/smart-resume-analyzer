package com.resumeanalyzer.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.resumeanalyzer.model.JobMatch;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
@Transactional
public class JobMatchDAOImpl implements JobMatchDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public JobMatch save(JobMatch jobMatch) {
        if (jobMatch.getId() == null || jobMatch.getId() == 0) {
            em.persist(jobMatch);
            return jobMatch;
        }
        return em.merge(jobMatch);
    }

    @Override
    public Optional<JobMatch> findById(Integer id) {
        return Optional.ofNullable(em.find(JobMatch.class, id));
    }

    @Override
    public List<JobMatch> findAll() {
        return em.createQuery("FROM JobMatch j ORDER BY j.matchedAt DESC", JobMatch.class)
                 .getResultList();
    }

    @Override
    public void deleteById(Integer id) {
        JobMatch j = em.find(JobMatch.class, id);
        if (j != null) em.remove(j);
    }
}