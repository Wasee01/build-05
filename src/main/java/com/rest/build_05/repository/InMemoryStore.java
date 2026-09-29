package com.rest.build_05.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.rest.build_05.model.Course;
import com.rest.build_05.model.Enrollment;
import com.rest.build_05.model.Section;
import com.rest.build_05.model.Student;

@Repository
public class InMemoryStore {
    private final Map<Long, Course> courses = new ConcurrentHashMap<>();
    private final Map<Long, Section> sections = new ConcurrentHashMap<>();
    private final Map<Long, Enrollment> enrollments = new ConcurrentHashMap<>();
    private final Map<Long, Student> students = new ConcurrentHashMap<>();
    private final AtomicLong courseSeq = new AtomicLong(1);
    private final AtomicLong sectionSeq = new AtomicLong(1);
    private final AtomicLong enrollmentSeq = new AtomicLong(1);

    // courses
    public List<Course> allCourses() { return new ArrayList<>(courses.values()); }
    public Optional<Course> course(Long id) { return Optional.ofNullable(courses.get(id)); }
    public Course saveCourse(Course c) {
        if (c.getId() == null) c.setId(courseSeq.getAndIncrement());
        courses.put(c.getId(), c);
        return c;
    }

    public List<Section> allSections() { return new ArrayList<>(sections.values()); }
    public Optional<Section> section(Long id) { return Optional.ofNullable(sections.get(id)); }
    public Section saveSection(Section s) {
        if (s.getId() == null) s.setId(sectionSeq.getAndIncrement());
        sections.put(s.getId(), s);
        return s;
    }

    public Optional<Enrollment> enrollment(Long id) { return Optional.ofNullable(enrollments.get(id)); }
    public Enrollment saveEnrollment(Enrollment e) {
        if (e.getId() == null) e.setId(enrollmentSeq.getAndIncrement());
        enrollments.put(e.getId(), e);
        return e;
    }
    public void deleteEnrollment(Long id) { enrollments.remove(id); }

    public List<Enrollment> enrollmentsForStudent(Long studentId) {
        return enrollments.values().stream()
            .filter(e -> e.getStudentId().equals(studentId))
            .toList();
    }

    public boolean enrollmentExists(Long studentId, Long sectionId) {
        return enrollments.values().stream()
            .anyMatch(e -> e.getStudentId().equals(studentId)
                       && e.getSectionId().equals(sectionId));
    }
}