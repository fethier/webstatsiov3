package com.webstats.controller;

import com.webstats.model.Organization;
import com.webstats.repository.OrganizationRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/organizations")
@CrossOrigin(origins = "*")
public class OrganizationController {
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @PostMapping
    public ResponseEntity<Organization> createOrganization(@Valid @RequestBody Organization organization) {
        try {
            Organization savedOrg = organizationRepository.save(organization);
            return ResponseEntity.ok(savedOrg);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Organization> getOrganization(@PathVariable String id) {
        Optional<Organization> org = organizationRepository.findById(id);
        return org.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<Organization>> getAllOrganizations() {
        List<Organization> organizations = organizationRepository.findByIsActiveTrue();
        return ResponseEntity.ok(organizations);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Organization> updateOrganization(
            @PathVariable String id,
            @Valid @RequestBody Organization organization) {
        
        Optional<Organization> existingOrg = organizationRepository.findById(id);
        if (existingOrg.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        organization.setId(id);
        Organization updatedOrg = organizationRepository.save(organization);
        return ResponseEntity.ok(updatedOrg);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganization(@PathVariable String id) {
        Optional<Organization> org = organizationRepository.findById(id);
        if (org.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Organization organization = org.get();
        organization.setIsActive(false);
        organizationRepository.save(organization);
        
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/by-subscription/{plan}")
    public ResponseEntity<List<Organization>> getOrganizationsBySubscriptionPlan(
            @PathVariable Organization.SubscriptionPlan plan) {
        
        List<Organization> organizations = organizationRepository.findBySubscriptionPlan(plan);
        return ResponseEntity.ok(organizations);
    }
}