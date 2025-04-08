package org.springframework.samples.petclinic.visits.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.visits.model.Visit;
import org.springframework.samples.petclinic.visits.model.VisitRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(VisitResource.class)
@ActiveProfiles("test")
class VisitResourceTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    VisitRepository visitRepository;

    @Test
    void shouldFetchVisitsByPetId() throws Exception {
        given(visitRepository.findByPetId(111))
            .willReturn(asList(
                Visit.VisitBuilder.aVisit().id(1).petId(111).description("Visit 1").build(),
                Visit.VisitBuilder.aVisit().id(2).petId(111).description("Visit 2").build()
            ));

        mvc.perform(get("/owners/1/pets/111/visits"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].description").value("Visit 1"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].description").value("Visit 2"));
    }

    @Test
    void shouldReturnEmptyListWhenNoVisitsFoundByPetId() throws Exception {
        given(visitRepository.findByPetId(999)).willReturn(Collections.emptyList());

        mvc.perform(get("/owners/1/pets/999/visits"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldFailToCreateVisitWithInvalidPetId() throws Exception {
        mvc.perform(post("/owners/1/pets/0/visits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2025-04-07\",\"description\":\"Routine checkup\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldHandleRepositoryExceptionForCreate() throws Exception {
        doThrow(new RuntimeException("Database error")).when(visitRepository).save(any(Visit.class));

        mvc.perform(post("/owners/1/pets/111/visits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2025-04-07\",\"description\":\"Routine checkup\"}"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldHandleRepositoryExceptionForFetchByPetId() throws Exception {
        given(visitRepository.findByPetId(111)).willThrow(new RuntimeException("Database error"));

        mvc.perform(get("/owners/1/pets/111/visits"))
            .andExpect(status().isInternalServerError());
    }
}