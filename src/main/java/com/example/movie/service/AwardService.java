package com.example.movie.service;

import com.example.movie.dto.AwardBodyRequest;
import com.example.movie.dto.AwardBodyResponse;
import com.example.movie.dto.AwardCategoryRequest;
import com.example.movie.dto.AwardCategoryResponse;
import com.example.movie.dto.AwardEditionRequest;
import com.example.movie.dto.AwardEditionResponse;
import com.example.movie.dto.AwardNominationRequest;
import com.example.movie.dto.AwardNominationResponse;
import com.example.movie.entity.AwardBody;
import com.example.movie.entity.AwardCategory;
import com.example.movie.entity.AwardEdition;
import com.example.movie.entity.AwardNomination;
import com.example.movie.entity.Movie;
import com.example.movie.entity.Person;
import com.example.movie.entity.TvSeason;
import com.example.movie.exception.ResourceNotFoundException;
import com.example.movie.repository.AwardBodyRepository;
import com.example.movie.repository.AwardCategoryRepository;
import com.example.movie.repository.AwardEditionRepository;
import com.example.movie.repository.AwardNominationRepository;
import com.example.movie.repository.MovieRepository;
import com.example.movie.repository.PersonRepository;
import com.example.movie.repository.TvSeasonRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AwardService {

    private final AwardBodyRepository awardBodyRepository;
    private final AwardEditionRepository awardEditionRepository;
    private final AwardCategoryRepository awardCategoryRepository;
    private final AwardNominationRepository awardNominationRepository;
    private final MovieRepository movieRepository;
    private final TvSeasonRepository tvSeasonRepository;
    private final PersonRepository personRepository;

    public AwardService(
            AwardBodyRepository awardBodyRepository,
            AwardEditionRepository awardEditionRepository,
            AwardCategoryRepository awardCategoryRepository,
            AwardNominationRepository awardNominationRepository,
            MovieRepository movieRepository,
            TvSeasonRepository tvSeasonRepository,
            PersonRepository personRepository
    ) {
        this.awardBodyRepository = awardBodyRepository;
        this.awardEditionRepository = awardEditionRepository;
        this.awardCategoryRepository = awardCategoryRepository;
        this.awardNominationRepository = awardNominationRepository;
        this.movieRepository = movieRepository;
        this.tvSeasonRepository = tvSeasonRepository;
        this.personRepository = personRepository;
    }

    public List<AwardBodyResponse> getAwardBodies() {
        return awardBodyRepository.findAllByOrderBySortOrderAscIdAsc()
                .stream()
                .map(this::convertToBodyResponse)
                .toList();
    }

    @Transactional
    public AwardBodyResponse createAwardBody(AwardBodyRequest request) {
        String name = normalizeRequiredText(request.getName(), "Award body name cannot be blank");
        if (awardBodyRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Award body already exists: " + name);
        }

        AwardBody awardBody = new AwardBody();
        shiftAwardBodiesForInsert(request.getSortOrder());
        applyBodyRequest(awardBody, request, name);
        return convertToBodyResponse(awardBodyRepository.save(awardBody));
    }

    @Transactional
    public AwardBodyResponse updateAwardBody(Long id, AwardBodyRequest request) {
        AwardBody awardBody = awardBodyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Award body not found with id:" + id));
        String name = normalizeRequiredText(request.getName(), "Award body name cannot be blank");

        awardBodyRepository.findByName(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Award body already exists: " + name);
                });

        shiftAwardBodiesForUpdate(awardBody, request.getSortOrder());
        applyBodyRequest(awardBody, request, name);
        return convertToBodyResponse(awardBodyRepository.save(awardBody));
    }

    @Transactional
    public void deleteAwardBody(Long id) {
        AwardBody awardBody = awardBodyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Award body not found with id:" + id));
        Integer deletedSortOrder = awardBody.getSortOrder();

        awardNominationRepository.deleteByAwardBodyId(id);
        awardCategoryRepository.deleteByAwardBodyId(id);
        awardEditionRepository.deleteByAwardBodyId(id);
        awardBodyRepository.delete(awardBody);

        if (deletedSortOrder != null) {
            List<AwardBody> followingAwardBodies = awardBodyRepository
                    .findBySortOrderGreaterThanOrderBySortOrderAscIdAsc(deletedSortOrder);
            for (AwardBody followingAwardBody : followingAwardBodies) {
                followingAwardBody.setSortOrder(followingAwardBody.getSortOrder() - 1);
            }
            awardBodyRepository.saveAll(followingAwardBodies);
        }
    }

    public List<AwardEditionResponse> getAwardEditions(Long awardBodyId) {
        List<AwardEdition> awardEditions = awardBodyId == null
                ? awardEditionRepository.findAllByOrderByAwardYearDescEditionNumberDescIdDesc()
                : awardEditionRepository.findByAwardBodyIdOrderByEditionNumberDesc(awardBodyId);

        return awardEditions.stream()
                .map(this::convertToEditionResponse)
                .toList();
    }

    public AwardEditionResponse getAwardEdition(Long id) {
        AwardEdition awardEdition = awardEditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Award edition not found with id:" + id));
        return convertToEditionResponse(awardEdition);
    }

    @Transactional
    public AwardEditionResponse createAwardEdition(AwardEditionRequest request) {
        AwardBody awardBody = findAwardBody(request.getAwardBodyId());
        validateEditionUniqueness(awardBody.getId(), request.getEditionNumber(), null);

        AwardEdition awardEdition = new AwardEdition();
        awardEdition.setAwardBody(awardBody);
        applyEditionRequest(awardEdition, request);
        return convertToEditionResponse(awardEditionRepository.save(awardEdition));
    }

    @Transactional
    public AwardEditionResponse updateAwardEdition(Long id, AwardEditionRequest request) {
        AwardEdition awardEdition = awardEditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Award edition not found with id:" + id));
        AwardBody awardBody = findAwardBody(request.getAwardBodyId());
        validateEditionUniqueness(awardBody.getId(), request.getEditionNumber(), id);

        awardEdition.setAwardBody(awardBody);
        applyEditionRequest(awardEdition, request);
        return convertToEditionResponse(awardEditionRepository.save(awardEdition));
    }

    @Transactional
    public void deleteAwardEdition(Long id) {
        AwardEdition awardEdition = awardEditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Award edition not found with id:" + id));

        awardNominationRepository.deleteByAwardEditionId(id);
        awardCategoryRepository.deleteByAwardEditionId(id);
        awardEditionRepository.delete(awardEdition);
    }

    public List<AwardCategoryResponse> getAwardCategories(Long awardEditionId) {
        return awardCategoryRepository.findByAwardEditionIdOrderBySortOrderAscIdAsc(awardEditionId)
                .stream()
                .map(this::convertToCategoryResponse)
                .toList();
    }

    public AwardCategoryResponse getAwardCategory(Long id) {
        AwardCategory awardCategory = awardCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Award category not found with id:" + id));
        return convertToCategoryResponse(awardCategory);
    }

    @Transactional
    public AwardCategoryResponse createAwardCategory(AwardCategoryRequest request) {
        AwardEdition awardEdition = findAwardEdition(request.getAwardEditionId());
        String name = normalizeRequiredText(request.getName(), "Award category name cannot be blank");
        validateCategoryUniqueness(awardEdition.getId(), name, null);

        AwardCategory awardCategory = new AwardCategory();
        awardCategory.setAwardEdition(awardEdition);
        shiftAwardCategoriesForInsert(awardEdition.getId(), request.getSortOrder());
        applyCategoryRequest(awardCategory, request, name);
        return convertToCategoryResponse(awardCategoryRepository.save(awardCategory));
    }

    @Transactional
    public AwardCategoryResponse updateAwardCategory(Long id, AwardCategoryRequest request) {
        AwardCategory awardCategory = awardCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Award category not found with id:" + id));
        AwardEdition awardEdition = findAwardEdition(request.getAwardEditionId());
        String name = normalizeRequiredText(request.getName(), "Award category name cannot be blank");
        validateCategoryUniqueness(awardEdition.getId(), name, id);

        shiftAwardCategoriesForUpdate(awardCategory, awardEdition.getId(), request.getSortOrder());
        awardCategory.setAwardEdition(awardEdition);
        applyCategoryRequest(awardCategory, request, name);
        return convertToCategoryResponse(awardCategoryRepository.save(awardCategory));
    }

    @Transactional
    public void deleteAwardCategory(Long id) {
        AwardCategory awardCategory = awardCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Award category not found with id:" + id));
        Long awardEditionId = awardCategory.getAwardEdition().getId();
        Integer deletedSortOrder = awardCategory.getSortOrder();

        awardNominationRepository.deleteByAwardCategoryId(id);
        awardCategoryRepository.delete(awardCategory);

        if (deletedSortOrder != null) {
            List<AwardCategory> followingAwardCategories = awardCategoryRepository
                    .findByAwardEditionIdAndSortOrderGreaterThanOrderBySortOrderAscIdAsc(awardEditionId, deletedSortOrder);
            for (AwardCategory followingAwardCategory : followingAwardCategories) {
                followingAwardCategory.setSortOrder(followingAwardCategory.getSortOrder() - 1);
            }
            awardCategoryRepository.saveAll(followingAwardCategories);
        }
    }

    public List<AwardNominationResponse> getAwardNominations(Long awardCategoryId) {
        return awardNominationRepository.findByAwardCategoryIdOrderBySortOrderAscIdAsc(awardCategoryId)
                .stream()
                .map(this::convertToNominationResponse)
                .toList();
    }

    public AwardNominationResponse getAwardNomination(Long id) {
        AwardNomination awardNomination = awardNominationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Award nomination not found with id:" + id));
        return convertToNominationResponse(awardNomination);
    }

    @Transactional
    public AwardNominationResponse createAwardNomination(AwardNominationRequest request) {
        AwardCategory awardCategory = findAwardCategory(request.getAwardCategoryId());

        AwardNomination awardNomination = new AwardNomination();
        awardNomination.setAwardCategory(awardCategory);
        applyNominationRequest(awardNomination, request);
        return convertToNominationResponse(awardNominationRepository.save(awardNomination));
    }

    @Transactional
    public AwardNominationResponse updateAwardNomination(Long id, AwardNominationRequest request) {
        AwardNomination awardNomination = awardNominationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Award nomination not found with id:" + id));
        AwardCategory awardCategory = findAwardCategory(request.getAwardCategoryId());

        awardNomination.setAwardCategory(awardCategory);
        applyNominationRequest(awardNomination, request);
        return convertToNominationResponse(awardNominationRepository.save(awardNomination));
    }

    @Transactional
    public void deleteAwardNomination(Long id) {
        AwardNomination awardNomination = awardNominationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Award nomination not found with id:" + id));
        Long awardCategoryId = awardNomination.getAwardCategory().getId();
        Integer deletedSortOrder = awardNomination.getSortOrder();

        awardNominationRepository.delete(awardNomination);

        if (deletedSortOrder != null) {
            List<AwardNomination> followingNominations = awardNominationRepository
                    .findByAwardCategoryIdAndSortOrderGreaterThanOrderBySortOrderAscIdAsc(awardCategoryId, deletedSortOrder);
            for (AwardNomination followingNomination : followingNominations) {
                followingNomination.setSortOrder(followingNomination.getSortOrder() - 1);
            }
            awardNominationRepository.saveAll(followingNominations);
        }
    }

    private AwardBody findAwardBody(Long awardBodyId) {
        return awardBodyRepository.findById(awardBodyId)
                .orElseThrow(() -> new ResourceNotFoundException("Award body not found with id:" + awardBodyId));
    }

    private AwardEdition findAwardEdition(Long awardEditionId) {
        return awardEditionRepository.findById(awardEditionId)
                .orElseThrow(() -> new ResourceNotFoundException("Award edition not found with id:" + awardEditionId));
    }

    private AwardCategory findAwardCategory(Long awardCategoryId) {
        return awardCategoryRepository.findById(awardCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Award category not found with id:" + awardCategoryId));
    }

    private void validateEditionUniqueness(Long awardBodyId, Integer editionNumber, Long currentEditionId) {
        awardEditionRepository.findByAwardBodyIdAndEditionNumber(awardBodyId, editionNumber)
                .filter(existing -> currentEditionId == null || !existing.getId().equals(currentEditionId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Award edition already exists for this award body and edition number");
                });
    }

    private void validateCategoryUniqueness(Long awardEditionId, String name, Long currentCategoryId) {
        awardCategoryRepository.findByAwardEditionIdAndName(awardEditionId, name)
                .filter(existing -> currentCategoryId == null || !existing.getId().equals(currentCategoryId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Award category already exists for this award edition: " + name);
                });
    }

    private void applyBodyRequest(AwardBody awardBody, AwardBodyRequest request, String normalizedName) {
        awardBody.setName(normalizedName);
        awardBody.setOriginalName(normalizeOptionalText(request.getOriginalName()));
        awardBody.setSortOrder(request.getSortOrder());
        awardBody.setDescription(normalizeOptionalText(request.getDescription()));
    }

    private void shiftAwardBodiesForInsert(Integer sortOrder) {
        if (sortOrder == null) {
            return;
        }

        List<AwardBody> awardBodiesToShift = awardBodyRepository
                .findBySortOrderGreaterThanEqualOrderBySortOrderDesc(sortOrder);
        for (AwardBody existingAwardBody : awardBodiesToShift) {
            existingAwardBody.setSortOrder(existingAwardBody.getSortOrder() + 1);
        }
        awardBodyRepository.saveAll(awardBodiesToShift);
    }

    private void shiftAwardBodiesForUpdate(AwardBody awardBody, Integer requestedSortOrder) {
        Integer currentSortOrder = awardBody.getSortOrder();
        if (requestedSortOrder == null || requestedSortOrder.equals(currentSortOrder)) {
            return;
        }
        if (currentSortOrder == null) {
            shiftAwardBodiesForInsert(requestedSortOrder);
            return;
        }

        List<AwardBody> awardBodies = awardBodyRepository.findAllByOrderBySortOrderAscIdAsc();
        for (AwardBody existingAwardBody : awardBodies) {
            if (existingAwardBody.getId().equals(awardBody.getId()) || existingAwardBody.getSortOrder() == null) {
                continue;
            }
            Integer existingSortOrder = existingAwardBody.getSortOrder();
            if (requestedSortOrder < currentSortOrder
                    && existingSortOrder >= requestedSortOrder
                    && existingSortOrder < currentSortOrder) {
                existingAwardBody.setSortOrder(existingSortOrder + 1);
            } else if (requestedSortOrder > currentSortOrder
                    && existingSortOrder <= requestedSortOrder
                    && existingSortOrder > currentSortOrder) {
                existingAwardBody.setSortOrder(existingSortOrder - 1);
            }
        }
        awardBodyRepository.saveAll(awardBodies);
    }

    private void applyEditionRequest(AwardEdition awardEdition, AwardEditionRequest request) {
        awardEdition.setName(normalizeRequiredText(request.getName(), "Award edition name cannot be blank"));
        awardEdition.setEditionNumber(request.getEditionNumber());
        awardEdition.setAwardYear(request.getAwardYear());
        awardEdition.setCeremonyDate(request.getCeremonyDate());
        awardEdition.setDescription(normalizeOptionalText(request.getDescription()));
    }

    private void applyCategoryRequest(AwardCategory awardCategory, AwardCategoryRequest request, String normalizedName) {
        awardCategory.setName(normalizedName);
        awardCategory.setSortOrder(request.getSortOrder());
        awardCategory.setDescription(normalizeOptionalText(request.getDescription()));
    }

    private void shiftAwardCategoriesForInsert(Long awardEditionId, Integer sortOrder) {
        if (sortOrder == null) {
            return;
        }

        List<AwardCategory> awardCategoriesToShift = awardCategoryRepository
                .findByAwardEditionIdAndSortOrderGreaterThanEqualOrderBySortOrderDesc(awardEditionId, sortOrder);
        for (AwardCategory existingAwardCategory : awardCategoriesToShift) {
            existingAwardCategory.setSortOrder(existingAwardCategory.getSortOrder() + 1);
        }
        awardCategoryRepository.saveAll(awardCategoriesToShift);
    }

    private void shiftAwardCategoriesForUpdate(
            AwardCategory awardCategory,
            Long requestedAwardEditionId,
            Integer requestedSortOrder
    ) {
        Integer currentSortOrder = awardCategory.getSortOrder();
        Long currentAwardEditionId = awardCategory.getAwardEdition().getId();
        boolean editionChanged = !currentAwardEditionId.equals(requestedAwardEditionId);
        if (requestedSortOrder == null) {
            return;
        }
        if (editionChanged || currentSortOrder == null) {
            shiftAwardCategoriesForInsert(requestedAwardEditionId, requestedSortOrder);
            return;
        }
        if (requestedSortOrder.equals(currentSortOrder)) {
            return;
        }

        List<AwardCategory> awardCategories = awardCategoryRepository
                .findByAwardEditionIdOrderBySortOrderAscIdAsc(requestedAwardEditionId);
        for (AwardCategory existingAwardCategory : awardCategories) {
            if (existingAwardCategory.getId().equals(awardCategory.getId())
                    || existingAwardCategory.getSortOrder() == null) {
                continue;
            }
            Integer existingSortOrder = existingAwardCategory.getSortOrder();
            if (requestedSortOrder < currentSortOrder
                    && existingSortOrder >= requestedSortOrder
                    && existingSortOrder < currentSortOrder) {
                existingAwardCategory.setSortOrder(existingSortOrder + 1);
            } else if (requestedSortOrder > currentSortOrder
                    && existingSortOrder <= requestedSortOrder
                    && existingSortOrder > currentSortOrder) {
                existingAwardCategory.setSortOrder(existingSortOrder - 1);
            }
        }
        awardCategoryRepository.saveAll(awardCategories);
    }

    private void applyNominationRequest(AwardNomination awardNomination, AwardNominationRequest request) {
        validateNominationTarget(request);

        Movie movie = request.getMovieId() == null ? null : movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id:" + request.getMovieId()));
        TvSeason tvSeason = request.getTvSeasonId() == null ? null : tvSeasonRepository.findById(request.getTvSeasonId())
                .orElseThrow(() -> new ResourceNotFoundException("TV season not found with id:" + request.getTvSeasonId()));
        Person person = request.getPersonId() == null ? null : personRepository.findById(request.getPersonId())
                .orElseThrow(() -> new ResourceNotFoundException("Person not found with id:" + request.getPersonId()));

        awardNomination.setMovie(movie);
        awardNomination.setTvSeason(tvSeason);
        awardNomination.setPerson(person);
        awardNomination.setWinner(Boolean.TRUE.equals(request.getWinner()));
        awardNomination.setNote(normalizeOptionalText(request.getNote()));
        awardNomination.setSortOrder(request.getSortOrder());
    }

    private void validateNominationTarget(AwardNominationRequest request) {
        if (request.getMovieId() != null && request.getTvSeasonId() != null) {
            throw new IllegalArgumentException("A nomination cannot reference both a movie and a TV season");
        }
        if (request.getMovieId() == null && request.getTvSeasonId() == null && request.getPersonId() == null) {
            throw new IllegalArgumentException("A nomination must reference a movie, TV season, or person");
        }
    }

    private AwardBodyResponse convertToBodyResponse(AwardBody awardBody) {
        AwardBodyResponse response = new AwardBodyResponse();
        response.setId(awardBody.getId());
        response.setName(awardBody.getName());
        response.setOriginalName(awardBody.getOriginalName());
        response.setSortOrder(awardBody.getSortOrder());
        response.setDescription(awardBody.getDescription());
        response.setCreatedAt(awardBody.getCreatedAt());
        response.setUpdatedAt(awardBody.getUpdatedAt());
        return response;
    }

    private AwardEditionResponse convertToEditionResponse(AwardEdition awardEdition) {
        AwardEditionResponse response = new AwardEditionResponse();
        response.setId(awardEdition.getId());
        response.setAwardBodyId(awardEdition.getAwardBody().getId());
        response.setAwardBodyName(awardEdition.getAwardBody().getName());
        response.setName(awardEdition.getName());
        response.setEditionNumber(awardEdition.getEditionNumber());
        response.setAwardYear(awardEdition.getAwardYear());
        response.setCeremonyDate(awardEdition.getCeremonyDate());
        response.setDescription(awardEdition.getDescription());
        response.setCreatedAt(awardEdition.getCreatedAt());
        response.setUpdatedAt(awardEdition.getUpdatedAt());
        return response;
    }

    private AwardCategoryResponse convertToCategoryResponse(AwardCategory awardCategory) {
        AwardCategoryResponse response = new AwardCategoryResponse();
        response.setId(awardCategory.getId());
        response.setAwardEditionId(awardCategory.getAwardEdition().getId());
        response.setAwardEditionName(awardCategory.getAwardEdition().getName());
        response.setAwardBodyId(awardCategory.getAwardEdition().getAwardBody().getId());
        response.setAwardBodyName(awardCategory.getAwardEdition().getAwardBody().getName());
        response.setName(awardCategory.getName());
        response.setSortOrder(awardCategory.getSortOrder());
        response.setDescription(awardCategory.getDescription());
        response.setCreatedAt(awardCategory.getCreatedAt());
        response.setUpdatedAt(awardCategory.getUpdatedAt());
        return response;
    }

    private AwardNominationResponse convertToNominationResponse(AwardNomination awardNomination) {
        AwardNominationResponse response = new AwardNominationResponse();
        response.setId(awardNomination.getId());
        response.setAwardCategoryId(awardNomination.getAwardCategory().getId());
        response.setAwardCategoryName(awardNomination.getAwardCategory().getName());
        response.setAwardEditionId(awardNomination.getAwardCategory().getAwardEdition().getId());
        response.setAwardEditionName(awardNomination.getAwardCategory().getAwardEdition().getName());
        response.setAwardBodyId(awardNomination.getAwardCategory().getAwardEdition().getAwardBody().getId());
        response.setAwardBodyName(awardNomination.getAwardCategory().getAwardEdition().getAwardBody().getName());
        response.setWinner(awardNomination.getWinner());
        response.setNote(awardNomination.getNote());
        response.setSortOrder(awardNomination.getSortOrder());
        response.setCreatedAt(awardNomination.getCreatedAt());
        response.setUpdatedAt(awardNomination.getUpdatedAt());

        if (awardNomination.getMovie() != null) {
            response.setMovieId(awardNomination.getMovie().getId());
            response.setMovieTitle(awardNomination.getMovie().getTitle());
            response.setMoviePosterPath(awardNomination.getMovie().getPosterPath());
        }
        if (awardNomination.getTvSeason() != null) {
            response.setTvSeasonId(awardNomination.getTvSeason().getId());
            response.setTvSeasonTitle(awardNomination.getTvSeason().getTitle());
            response.setTvSeasonShowName(awardNomination.getTvSeason().getShowName());
            response.setTvSeasonNumber(awardNomination.getTvSeason().getSeasonNumber());
            response.setTvSeasonPosterPath(awardNomination.getTvSeason().getPosterPath());
        }
        if (awardNomination.getPerson() != null) {
            response.setPersonId(awardNomination.getPerson().getId());
            response.setPersonName(awardNomination.getPerson().getName());
            response.setPersonProfilePath(awardNomination.getPerson().getProfilePath());
        }
        return response;
    }

    private String normalizeRequiredText(String value, String message) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
