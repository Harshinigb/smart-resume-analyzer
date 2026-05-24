package com.resumeanalyzer.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.resumeanalyzer.model.Resume;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
@Transactional
public class ResumeDAOImpl implements ResumeDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Resume save(Resume resume) {
        if (resume.getId() == null || resume.getId() == 0) {
            em.persist(resume);
            return resume;
        }
        return em.merge(resume);
    }

    @Override
    public Optional<Resume> findById(Integer id) {
        return Optional.ofNullable(em.find(Resume.class, id));
    }

    @Override
    public List<Resume> findAll() {
        return em.createQuery("FROM Resume r ORDER BY r.uploadedAt DESC", Resume.class)
                 .getResultList();
    }

    @Override
    public void deleteById(Integer id) {
        Resume r = em.find(Resume.class, id);
        if (r != null) em.remove(r);
    }
}