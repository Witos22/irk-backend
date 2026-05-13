package com.dom.irk_Backend.repository;

import com.dom.irk_Backend.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Integer>{
    Optional<Course> findByNameIgnoreCase(String name);
}
