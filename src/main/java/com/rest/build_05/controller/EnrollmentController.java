package com.rest.build_05.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.rest.build_05.model.Enrollment;
import com.rest.build_05.model.Section;
import com.rest.build_05.repository.InMemoryStore;

@RestController
@RequestMapping("/api")
public class EnrollmentController {

    private final InMemoryStore store;

    public EnrollmentController(InMemoryStore store) { this.store = store; }

    @PostMapping("/enrollments")
    public ResponseEntity<?> create(@RequestBody Enrollment req) {
        Section section = store.section(req.getSectionId()).orElse(null);
        if (section == null) return ResponseEntity.notFound().build();

        boolean duplicate = store.enrollmentExists(req.getStudentId(), req.getSectionId());
        boolean full = section.getEnrolled() >= section.getCapacity();
        if (duplicate || full) return ResponseEntity.status(HttpStatus.CONFLICT).build();

        Enrollment saved = store.saveEnrollment(req);
        section.setEnrolled(section.getEnrolled() + 1);
        store.saveSection(section);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @DeleteMapping("/enrollments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (store.enrollment(id).isEmpty()) return ResponseEntity.notFound().build();
        store.deleteEnrollment(id);
        return ResponseEntity.noContent().build();  // 204, empty body
    }

    @GetMapping("/students/{id}/enrollments")
    public List<Enrollment> forStudent(@PathVariable Long id) {
        return store.enrollmentsForStudent(id);
    }
}