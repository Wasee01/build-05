package com.rest.build_05.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.rest.build_05.model.Course;
import com.rest.build_05.repository.InMemoryStore;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final InMemoryStore store;

    public CourseController(InMemoryStore store) { this.store = store; }

    @GetMapping
    public List<Course> list(@RequestParam(required = false) String subject) {
        return store.allCourses().stream()
            .filter(c -> subject == null || subject.equalsIgnoreCase(c.getSubject()))
            .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Course> get(@PathVariable Long id) {
        return store.course(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Course> create(@RequestBody Course course) {
        Course saved = store.saveCourse(course);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(saved);
    }
}