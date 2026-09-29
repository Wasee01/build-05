package com.rest.build_05;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.rest.build_05.model.Course;
import com.rest.build_05.model.Enrollment;
import com.rest.build_05.model.Section;
import com.rest.build_05.repository.InMemoryStore;

import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RestSurfaceTests {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired InMemoryStore store;

    private Long seedCourse(String code, String subject) {
        Course c = new Course(); c.setCode(code); c.setTitle(code); c.setSubject(subject);
        return store.saveCourse(c).getId();
    }

    private Long seedSection(Long courseId, String termId, int capacity) {
        Section s = new Section();
        s.setCourseId(courseId); s.setTermId(termId);
        s.setCapacity(capacity); s.setEnrolled(0);
        return store.saveSection(s).getId();
    }

    // Row 1: GET /api/courses (list, filterable)
    @Test void listCourses_filterBySubject() throws Exception {
        seedCourse("CS101", "CS");
        seedCourse("MA101", "MA");
        mvc.perform(get("/api/courses").param("subject", "CS"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].subject").value("CS"));
    }

    // Row 2: GET /api/courses/{id} — 200 and 404
    @Test void getCourse_byId_and404() throws Exception {
        Long id = seedCourse("CS102", "CS");
        mvc.perform(get("/api/courses/{id}", id)).andExpect(status().isOk());
        mvc.perform(get("/api/courses/{id}", 99999)).andExpect(status().isNotFound());
    }

    // Row 3: POST /api/courses — 201 + Location
    @Test void createCourse_201_location() throws Exception {
        Course c = new Course(); c.setCode("CS103"); c.setTitle("Algo"); c.setSubject("CS");
        mvc.perform(post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(c)))
           .andExpect(status().isCreated())
           .andExpect(header().exists("Location"));
    }

    // Row 4: GET /api/sections (filterable by termId, courseId)
    @Test void listSections_filter() throws Exception {
        Long courseId = seedCourse("CS200", "CS");
        seedSection(courseId, "2026SP", 30);
        seedSection(courseId, "2026FA", 30);
        mvc.perform(get("/api/sections")
                .param("termId", "2026SP")
                .param("courseId", courseId.toString()))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.length()").value(1));
    }

    // Row 5: GET /api/sections/{id} — 200 / 404
    @Test void getSection_byId_and404() throws Exception {
        Long id = seedSection(seedCourse("CS300", "CS"), "2026SP", 10);
        mvc.perform(get("/api/sections/{id}", id)).andExpect(status().isOk());
        mvc.perform(get("/api/sections/{id}", 88888)).andExpect(status().isNotFound());
    }

    // Row 6: POST /api/enrollments — 201 + Location; 409 on full/duplicate
    @Test void createEnrollment_201_409() throws Exception {
        Long courseId = seedCourse("CS400", "CS");
        Long sectionId = seedSection(courseId, "2026SP", 1);

        Enrollment e1 = new Enrollment(); e1.setStudentId(1L); e1.setSectionId(sectionId);
        mvc.perform(post("/api/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(e1)))
           .andExpect(status().isCreated())
           .andExpect(header().exists("Location"));

        // duplicate 409
        mvc.perform(post("/api/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(e1)))
           .andExpect(status().isConflict());

        // different student, section is full  409
        Enrollment e2 = new Enrollment(); e2.setStudentId(2L); e2.setSectionId(sectionId);
        mvc.perform(post("/api/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(e2)))
           .andExpect(status().isConflict());
    }

    // Row 7: DELETE /api/enrollments/{id} — 204 empty body; 404 if absent
    @Test void deleteEnrollment_204_empty_404() throws Exception {
        Long courseId = seedCourse("CS500", "CS");
        Long sectionId = seedSection(courseId, "2026SP", 5);
        Enrollment e = new Enrollment(); e.setStudentId(1L); e.setSectionId(sectionId);
        Long eid = store.saveEnrollment(e).getId();

        mvc.perform(delete("/api/enrollments/{id}", eid))
           .andExpect(status().isNoContent())
           .andExpect(content().string(""));

        mvc.perform(delete("/api/enrollments/{id}", 77777))
           .andExpect(status().isNotFound());
    }

    // Row 8: GET /api/students/{id}/enrollments
    @Test void studentEnrollments() throws Exception {
        Long courseId = seedCourse("CS600", "CS");
        Long sectionId = seedSection(courseId, "2026SP", 5);
        Enrollment e = new Enrollment(); e.setStudentId(42L); e.setSectionId(sectionId);
        store.saveEnrollment(e);

        mvc.perform(get("/api/students/{id}/enrollments", 42L))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].studentId").value(42));
    }

    // Extras: unknown query params ignored; unknown resource  404 not 500
    @Test void unknownQueryParamIgnored() throws Exception {
        mvc.perform(get("/api/courses").param("bogus", "x"))
           .andExpect(status().isOk());
    }

    @Test void unknownSectionEnrollmentReturns404() throws Exception {
        Enrollment e = new Enrollment(); e.setStudentId(1L); e.setSectionId(99999L);
        mvc.perform(post("/api/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(e)))
           .andExpect(status().isNotFound());
    }
}