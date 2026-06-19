package com.example.movie.controller;

import com.example.movie.dto.AwardBodyRequest;
import com.example.movie.dto.AwardBodyResponse;
import com.example.movie.dto.AwardCategoryRequest;
import com.example.movie.dto.AwardCategoryResponse;
import com.example.movie.dto.AwardEditionRequest;
import com.example.movie.dto.AwardEditionResponse;
import com.example.movie.dto.AwardNominationRequest;
import com.example.movie.dto.AwardNominationResponse;
import com.example.movie.service.AwardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/awards")
@Tag(name = "Admin Awards API", description = "Admin operations for awards")
public class AdminAwardController {

    private final AwardService awardService;

    public AdminAwardController(AwardService awardService) {
        this.awardService = awardService;
    }

    @GetMapping("/bodies")
    @Operation(summary = "getAwardBodies: List award bodies")
    public List<AwardBodyResponse> getAwardBodies() {
        return awardService.getAwardBodies();
    }

    @PostMapping("/bodies")
    @Operation(summary = "createAwardBody: Create an award body")
    public AwardBodyResponse createAwardBody(@Valid @RequestBody AwardBodyRequest request) {
        return awardService.createAwardBody(request);
    }

    @PutMapping("/bodies/{id}")
    @Operation(summary = "updateAwardBody: Update an award body")
    public AwardBodyResponse updateAwardBody(
            @PathVariable Long id,
            @Valid @RequestBody AwardBodyRequest request
    ) {
        return awardService.updateAwardBody(id, request);
    }

    @DeleteMapping("/bodies/{id}")
    @Operation(summary = "deleteAwardBody: Delete an award body and its nested award data")
    public void deleteAwardBody(@PathVariable Long id) {
        awardService.deleteAwardBody(id);
    }

    @GetMapping("/editions")
    @Operation(summary = "getAwardEditions: List award editions")
    public List<AwardEditionResponse> getAwardEditions(@RequestParam(required = false) Long awardBodyId) {
        return awardService.getAwardEditions(awardBodyId);
    }

    @GetMapping("/editions/{id}")
    @Operation(summary = "getAwardEdition: Get an award edition")
    public AwardEditionResponse getAwardEdition(@PathVariable Long id) {
        return awardService.getAwardEdition(id);
    }

    @PostMapping("/editions")
    @Operation(summary = "createAwardEdition: Create an award edition")
    public AwardEditionResponse createAwardEdition(@Valid @RequestBody AwardEditionRequest request) {
        return awardService.createAwardEdition(request);
    }

    @PutMapping("/editions/{id}")
    @Operation(summary = "updateAwardEdition: Update an award edition")
    public AwardEditionResponse updateAwardEdition(
            @PathVariable Long id,
            @Valid @RequestBody AwardEditionRequest request
    ) {
        return awardService.updateAwardEdition(id, request);
    }

    @DeleteMapping("/editions/{id}")
    @Operation(summary = "deleteAwardEdition: Delete an award edition and its nested award data")
    public void deleteAwardEdition(@PathVariable Long id) {
        awardService.deleteAwardEdition(id);
    }

    @GetMapping("/categories")
    @Operation(summary = "getAwardCategories: List award categories for an edition")
    public List<AwardCategoryResponse> getAwardCategories(@RequestParam Long awardEditionId) {
        return awardService.getAwardCategories(awardEditionId);
    }

    @GetMapping("/categories/{id}")
    @Operation(summary = "getAwardCategory: Get an award category")
    public AwardCategoryResponse getAwardCategory(@PathVariable Long id) {
        return awardService.getAwardCategory(id);
    }

    @PostMapping("/categories")
    @Operation(summary = "createAwardCategory: Create an award category")
    public AwardCategoryResponse createAwardCategory(@Valid @RequestBody AwardCategoryRequest request) {
        return awardService.createAwardCategory(request);
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "updateAwardCategory: Update an award category")
    public AwardCategoryResponse updateAwardCategory(
            @PathVariable Long id,
            @Valid @RequestBody AwardCategoryRequest request
    ) {
        return awardService.updateAwardCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "deleteAwardCategory: Delete an award category and its nominations")
    public void deleteAwardCategory(@PathVariable Long id) {
        awardService.deleteAwardCategory(id);
    }

    @GetMapping("/nominations")
    @Operation(summary = "getAwardNominations: List nominations for a category")
    public List<AwardNominationResponse> getAwardNominations(@RequestParam Long awardCategoryId) {
        return awardService.getAwardNominations(awardCategoryId);
    }

    @GetMapping("/nominations/{id}")
    @Operation(summary = "getAwardNomination: Get an award nomination")
    public AwardNominationResponse getAwardNomination(@PathVariable Long id) {
        return awardService.getAwardNomination(id);
    }

    @PostMapping("/nominations")
    @Operation(summary = "createAwardNomination: Create an award nomination")
    public AwardNominationResponse createAwardNomination(@Valid @RequestBody AwardNominationRequest request) {
        return awardService.createAwardNomination(request);
    }

    @PutMapping("/nominations/{id}")
    @Operation(summary = "updateAwardNomination: Update an award nomination")
    public AwardNominationResponse updateAwardNomination(
            @PathVariable Long id,
            @Valid @RequestBody AwardNominationRequest request
    ) {
        return awardService.updateAwardNomination(id, request);
    }

    @DeleteMapping("/nominations/{id}")
    @Operation(summary = "deleteAwardNomination: Delete an award nomination")
    public void deleteAwardNomination(@PathVariable Long id) {
        awardService.deleteAwardNomination(id);
    }
}
