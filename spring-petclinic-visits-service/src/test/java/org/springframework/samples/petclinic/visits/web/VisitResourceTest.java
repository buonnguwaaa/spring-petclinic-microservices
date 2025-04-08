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
import java.util.Date;
import java.util.List;
import java.util.Map;

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
        // Given
        Visit visit = new Visit();
        visit.setDate(new Date());
        visit.setDescription("Annual checkup");

        when(visitRepository.save(any(Visit.class))).thenReturn(visit);

        // When
        Visit result = visitResource.create(visit, 1);

        // Then
        assertEquals(1, result.getPetId());
        verify(visitRepository).save(visit);
    }

    @Test
    void testCreateVisitWithInvalidPetId() {
        // Given
        Visit visit = new Visit();
        visit.setDate(new Date());
        visit.setDescription("Annual checkup");

        // When & Then
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> visitResource.create(visit, 0)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Invalid pet ID", exception.getReason());
    }

    @Test
    void testCreateVisitWithNullDescription() {
        // Given
        Visit visit = new Visit();
        visit.setDate(new Date());
        visit.setDescription(null);  // Null description

        // When & Then
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> visitResource.create(visit, 1)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Visit description is required", exception.getReason());
    }

    @Test
    void testCreateVisitWithNullDate() {
        // Given
        Visit visit = new Visit();
        visit.setDate(null);  // Null date
        visit.setDescription("Annual checkup");

        // When & Then
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> visitResource.create(visit, 1)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Visit date is required", exception.getReason());
    }

    @Test
    void testReadVisitsForPet() {
        // Given
        Visit visit1 = new Visit();
        visit1.setPetId(1);
        Visit visit2 = new Visit();
        visit2.setPetId(1);
        List<Visit> expected = Arrays.asList(visit1, visit2);

        when(visitRepository.findByPetId(1)).thenReturn(expected);

        // When
        List<Visit> result = visitResource.read(1);

        // Then
        assertEquals(expected, result);
        verify(visitRepository).findByPetId(1);
    }

    @Test
    void testGetVisitsForMultiplePets() {
        // Given
        List<Integer> petIds = Arrays.asList(1, 2);
        Visit visit1 = new Visit();
        visit1.setPetId(1);
        Visit visit2 = new Visit();
        visit2.setPetId(2);
        List<Visit> visits = Arrays.asList(visit1, visit2);

        when(visitRepository.findByPetIdIn(petIds)).thenReturn(visits);

        // When
        VisitResource.Visits result = visitResource.read(petIds);

        // Then
        assertEquals(visits, result.items());
        verify(visitRepository).findByPetIdIn(petIds);
    }

    @Test
    void testGetVisitsForMultiplePetsEmptyList() {
        // Given
        List<Integer> petIds = Arrays.asList(1, 2);
        List<Visit> visits = Arrays.asList(); // Empty list

        when(visitRepository.findByPetIdIn(petIds)).thenReturn(visits);

        // When
        VisitResource.Visits result = visitResource.read(petIds);

        // Then
        assertTrue(result.items().isEmpty());
        verify(visitRepository).findByPetIdIn(petIds);
    }

    @Test
    void testRepositoryExceptionHandling() {
        // Arrange
        when(visitRepository.findByPetId(anyInt())).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            visitResource.read(1);
        });

        // Kiểm tra mã lỗi và thông báo lỗi
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("Database error", exception.getReason());
    }
}