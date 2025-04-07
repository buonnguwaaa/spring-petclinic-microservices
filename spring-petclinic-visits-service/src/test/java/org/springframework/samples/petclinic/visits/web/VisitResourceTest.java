package org.springframework.samples.petclinic.visits.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.visits.model.Visit;
import org.springframework.samples.petclinic.visits.model.VisitRepository;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitResourceTest {

    @Mock
    private VisitRepository visitRepository;

    @InjectMocks
    private VisitResource visitResource;

    @Test
    void testCreateVisit() {
        Visit visit = new Visit();
        visit.setDate(new Date());
        visit.setDescription("Annual checkup");

        when(visitRepository.save(any(Visit.class))).thenReturn(visit);

        Visit result = visitResource.create(visit, 1);

        assertEquals(1, result.getPetId());
        verify(visitRepository).save(visit);
    }

    @Test
    void testCreateVisitWithInvalidPetId() {
        Visit visit = new Visit();
        visit.setDate(new Date());
        visit.setDescription("Annual checkup");

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> visitResource.create(visit, 0)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Invalid pet ID", exception.getReason());
    }

    @Test
    void testCreateVisitWithMissingDate() {
        Visit visit = new Visit();
        visit.setDescription("No date");

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> visitResource.create(visit, 1)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Missing required fields", exception.getReason());
    }

    @Test
    void testCreateVisitWithMissingDescription() {
        Visit visit = new Visit();
        visit.setDate(new Date());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> visitResource.create(visit, 1)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Missing required fields", exception.getReason());
    }

    @Test
    void testCreateVisitReturnsNull() {
        Visit visit = new Visit();
        visit.setDate(new Date());
        visit.setDescription("Dental cleaning");

        when(visitRepository.save(any(Visit.class))).thenReturn(null);

        Visit result = visitResource.create(visit, 1);

        assertNull(result);
    }

    @Test
    void testReadVisitsForPet() {
        Visit visit1 = new Visit();
        visit1.setPetId(1);
        Visit visit2 = new Visit();
        visit2.setPetId(1);
        List<Visit> expected = Arrays.asList(visit1, visit2);

        when(visitRepository.findByPetId(1)).thenReturn(expected);

        List<Visit> result = visitResource.read(1);

        assertEquals(expected, result);
        verify(visitRepository).findByPetId(1);
    }

    @Test
    void testReadVisitsForPetException() {
        when(visitRepository.findByPetId(anyInt())).thenThrow(new RuntimeException("DB error"));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> visitResource.read(1)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("DB error", exception.getReason());
    }

    @Test
    void testGetVisitsForMultiplePets() {
        List<Integer> petIds = Arrays.asList(1, 2);
        Visit visit1 = new Visit();
        visit1.setPetId(1);
        Visit visit2 = new Visit();
        visit2.setPetId(2);
        List<Visit> visits = Arrays.asList(visit1, visit2);

        when(visitRepository.findByPetIdIn(petIds)).thenReturn(visits);

        VisitResource.Visits result = visitResource.read(petIds);

        assertEquals(visits, result.items());
        verify(visitRepository).findByPetIdIn(petIds);
    }

    @Test
    void testGetVisitsWithEmptyPetIds() {
        VisitResource.Visits result = visitResource.read(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.items().isEmpty());
    }

    @Test
    void testGetVisitsWithNullPetIds() {
        VisitResource.Visits result = visitResource.read(null);

        assertNotNull(result);
        assertTrue(result.items().isEmpty());
    }

    @Test
    void testVisitsRecordFunctionality() {
        Visit visit = new Visit();
        List<Visit> visits = List.of(visit);

        VisitResource.Visits wrapper = new VisitResource.Visits(visits);

        assertEquals(visits, wrapper.items());
    }
}
