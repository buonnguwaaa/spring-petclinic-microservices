package org.springframework.samples.petclinic.visits.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.visits.model.Visit;
import org.springframework.samples.petclinic.visits.model.VisitRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for visit endpoints.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
class VisitResource {

    private final VisitRepository visitRepository;

    /**
     * Create a new visit.
     *
     * @param visit  Visit object to create
     * @param petId  ID of the pet to associate with visit
     * @return Created visit
     */
    @PostMapping("/owners/{ownerId}/pets/{petId}/visits")
    @ResponseStatus(HttpStatus.CREATED)
    public Visit create(
            @Valid @RequestBody Visit visit,
            @PathVariable("petId") int petId) {
        
        // Validate petId
        if (petId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pet ID");
        }
        
        // Set petId from path variable
        visit.setPetId(petId);
        
        // Validate required fields
        if (visit.getDate() == null || (visit.getDescription() == null || visit.getDescription().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required fields");
        }
        
        try {
            log.info("Saving visit {}", visit);
            return visitRepository.save(visit);
        } catch (RuntimeException e) {
            log.error("Error saving visit", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    /**
     * Read visits for a pet.
     *
     * @param petId ID of the pet
     * @return List of visits
     */
    @GetMapping("/owners/*/pets/{petId}/visits")
    public List<Visit> read(@PathVariable("petId") int petId) {
        try {
            return visitRepository.findByPetId(petId);
        } catch (RuntimeException e) {
            log.error("Error fetching visits for pet {}", petId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }

    /**
     * Read visits for multiple pets.
     *
     * @param petIds List of pet IDs
     * @return Map with items key containing list of visits
     */
    @GetMapping("/pets/visits")
    public Map<String, List<Visit>> map(@RequestParam("petId") List<Integer> petIds) {
        try {
            final Map<String, List<Visit>> visits = new HashMap<>();
            visits.put("items", visitRepository.findByPetIdIn(petIds));
            return visits;
        } catch (RuntimeException e) {
            log.error("Error fetching visits for pets {}", petIds, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", e);
        }
    }
}