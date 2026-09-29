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

import com.rest.build_05.model.Section;
import com.rest.build_05.repository.InMemoryStore;

@RestController
@RequestMapping("/api/sections")
public class SectionController {

    private final InMemoryStore store;

    public SectionController(InMemoryStore store) { this.store = store; }

 @GetMapping
    public List<Section> list(@RequestParam(required = false) String termId,
                              @RequestParam(required = false) Long courseId) {
        return store.allSections().stream()
                .filter(s -> termId == null || termId.equals(s.getTermId()))
                .filter(s -> courseId == null || courseId.equals(s.getCourseId()))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Section> get(@PathVariable Long id) {
        return store.section(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @PostMapping
    public ResponseEntity<Section> create(@RequestBody Section section) {
        if (section.getCourseId() == null
                || store.course(section.getCourseId()).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        section.setEnrolled(0);
        Section saved = store.saveSection(section);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(saved);
    }
}