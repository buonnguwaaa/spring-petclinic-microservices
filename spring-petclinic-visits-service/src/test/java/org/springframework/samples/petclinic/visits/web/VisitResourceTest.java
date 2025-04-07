package org.springframework.samples.petclinic.visits.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.visits.model.Visit;
import org.springframework.samples.petclinic.visits.model.VisitRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
class VisitResource {

    private final VisitRepository visitRepository;

    @PostMapping("/owners/{ownerId}/pets/{petId}/visits")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> create(
            @Valid @RequestBody Visit visit,
            @PathVariable("petId") int petId) {
        
        // Validate petId
        if (petId <= 0) {
            return ResponseEntity.badRequest().build();
        }
        
        // Set petId from path variable
        visit.setPetId(petId);
        
        // Validate required fields
        if (visit.getDate() == null || visit.getDescription() == null || visit.getDescription().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            log.info("Saving visit {}", visit);
            Visit savedVisit = visitRepository.save(visit);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedVisit);
        } catch (RuntimeException e) {
            log.error("Error saving visit", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/owners/*/pets/{petId}/visits")
    public ResponseEntity<?> read(@PathVariable("petId") int petId) {
        try {
            List<Visit> visits = visitRepository.findByPetId(petId);
            return ResponseEntity.ok(visits);
        } catch (RuntimeException e) {
            log.error("Error fetching visits for pet {}", petId, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/pets/visits")
    public ResponseEntity<Map<String, List<Visit>>> map(@RequestParam("petId") List<Integer> petIds) {
        try {
            final Map<String, List<Visit>> results = new HashMap<>();
            results.put("items", visitRepository.findByPetIdIn(petIds));
            return ResponseEntity.ok(results);
        } catch (RuntimeException e) {
            log.error("Error fetching visits for pets {}", petIds, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Map)errorResponse);
        }
    }
}