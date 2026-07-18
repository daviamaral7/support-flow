package davi.spf.supportflow.category.service;

import davi.spf.supportflow.category.dto.CategoryRequestDTO;
import davi.spf.supportflow.category.dto.CategoryResponseDTO;
import davi.spf.supportflow.category.dto.UpdateCategoryRequestDTO;
import davi.spf.supportflow.category.entity.Category;
import davi.spf.supportflow.category.mapper.CategoryMapper;
import davi.spf.supportflow.category.repository.CategoryRepository;
import davi.spf.supportflow.common.exception.BusinessRuleException;
import davi.spf.supportflow.common.exception.ResourceAlreadyExistsException;
import davi.spf.supportflow.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper mapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void shouldListActiveCategoriesUsingPageableAndMapResponses() {
        Pageable pageable = PageRequest.of(0, 10);
        Category hardware = category(1L, "Hardware", "Hardware support", true);
        Category software = category(2L, "Software", "Software support", true);
        CategoryResponseDTO hardwareResponse = response(hardware);
        CategoryResponseDTO softwareResponse = response(software);

        when(categoryRepository.findAllByActiveTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(hardware, software), pageable, 2));
        when(mapper.toResponse(hardware)).thenReturn(hardwareResponse);
        when(mapper.toResponse(software)).thenReturn(softwareResponse);

        Page<CategoryResponseDTO> result = categoryService.listCategories(pageable);

        assertThat(result.getContent()).containsExactly(hardwareResponse, softwareResponse);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(categoryRepository).findAllByActiveTrue(pageable);
        verify(mapper).toResponse(hardware);
        verify(mapper).toResponse(software);
    }

    @Test
    void shouldReturnCategoryResponseWhenFindingExistingCategoryById() {
        Category category = category(1L, "Hardware", "Hardware support", true);
        CategoryResponseDTO response = response(category);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(mapper.toResponse(category)).thenReturn(response);

        CategoryResponseDTO result = categoryService.findCategoryById(category.getId());

        assertThat(result).isSameAs(response);
        verify(categoryRepository).findById(category.getId());
        verify(mapper).toResponse(category);
    }

    @Test
    void shouldReturnInactiveCategoryResponseWhenFindingCategoryById() {
        Category inactiveCategory = category(1L, "Hardware", "Hardware support", false);
        CategoryResponseDTO response = response(inactiveCategory);

        when(categoryRepository.findById(inactiveCategory.getId())).thenReturn(Optional.of(inactiveCategory));
        when(mapper.toResponse(inactiveCategory)).thenReturn(response);

        CategoryResponseDTO result = categoryService.findCategoryById(inactiveCategory.getId());

        assertThat(result).isSameAs(response);
        verify(categoryRepository).findById(inactiveCategory.getId());
        verify(mapper).toResponse(inactiveCategory);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenFindingCategoryByIdAndCategoryDoesNotExist() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findCategoryById(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository).findById(99L);
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldCreateCategoryWithTrimmedNameAndReturnResponse() {
        CategoryRequestDTO request = categoryRequest("  Hardware  ", "  Hardware support  ");
        Category categoryFromMapper = category(null, request.name(), request.description(), true);
        CategoryResponseDTO response = response(1L, "Hardware", request.description(), true);

        when(categoryRepository.existsByNameIgnoreCase("Hardware")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(categoryFromMapper);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category savedCategory = invocation.getArgument(0);
            savedCategory.setId(1L);
            return savedCategory;
        });
        when(mapper.toResponse(categoryFromMapper)).thenReturn(response);

        CategoryResponseDTO result = categoryService.createCategory(request);

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        Category savedCategory = categoryCaptor.getValue();

        assertThat(result).isSameAs(response);
        assertThat(savedCategory.getName()).isEqualTo("Hardware");
        assertThat(savedCategory.getDescription()).isEqualTo(request.description());
        assertThat(savedCategory.isActive()).isTrue();

        verify(categoryRepository).existsByNameIgnoreCase("Hardware");
        verify(mapper).toEntity(request);
        verify(mapper).toResponse(savedCategory);
    }

    @Test
    void shouldThrowResourceAlreadyExistsExceptionWhenCreatingCategoryWithExistingName() {
        CategoryRequestDTO request = categoryRequest("  HARDWARE  ", "Hardware support");

        when(categoryRepository.existsByNameIgnoreCase("HARDWARE")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(ResourceAlreadyExistsException.class);

        verify(categoryRepository).existsByNameIgnoreCase("HARDWARE");
        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldUpdateNameAndDescriptionWithTrimmedValues() {
        Category category = category(1L, "Old name", "Old description", true);
        UpdateCategoryRequestDTO request = updateRequest("  Hardware  ", "  Hardware support  ");
        CategoryResponseDTO response = response(1L, "Hardware", "Hardware support", true);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(categoryRepository.findByNameIgnoreCase("Hardware")).thenReturn(Optional.empty());
        when(mapper.toResponse(category)).thenReturn(response);

        CategoryResponseDTO result = categoryService.updateCategory(category.getId(), request);

        assertThat(result).isSameAs(response);
        assertThat(category.getName()).isEqualTo("Hardware");
        assertThat(category.getDescription()).isEqualTo("Hardware support");
        verify(categoryRepository).findById(category.getId());
        verify(categoryRepository).findByNameIgnoreCase("Hardware");
        verify(categoryRepository, never()).save(any());
        verify(mapper).toResponse(category);
    }

    @Test
    void shouldUpdateOnlyDescriptionWhenNameIsNull() {
        Category category = category(1L, "Hardware", "Old description", true);
        UpdateCategoryRequestDTO request = updateRequest(null, "  Updated description  ");
        CategoryResponseDTO response = response(1L, "Hardware", "Updated description", true);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(mapper.toResponse(category)).thenReturn(response);

        CategoryResponseDTO result = categoryService.updateCategory(category.getId(), request);

        assertThat(result).isSameAs(response);
        assertThat(category.getName()).isEqualTo("Hardware");
        assertThat(category.getDescription()).isEqualTo("Updated description");
        verify(categoryRepository).findById(category.getId());
        verify(categoryRepository, never()).findByNameIgnoreCase(anyString());
        verify(categoryRepository, never()).save(any());
        verify(mapper).toResponse(category);
    }

    @Test
    void shouldUpdateOnlyNameWhenDescriptionIsNull() {
        Category category = category(1L, "Old name", "Hardware support", true);
        UpdateCategoryRequestDTO request = updateRequest("  Hardware  ", null);
        CategoryResponseDTO response = response(1L, "Hardware", "Hardware support", true);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(categoryRepository.findByNameIgnoreCase("Hardware")).thenReturn(Optional.empty());
        when(mapper.toResponse(category)).thenReturn(response);

        CategoryResponseDTO result = categoryService.updateCategory(category.getId(), request);

        assertThat(result).isSameAs(response);
        assertThat(category.getName()).isEqualTo("Hardware");
        assertThat(category.getDescription()).isEqualTo("Hardware support");
        verify(categoryRepository).findById(category.getId());
        verify(categoryRepository).findByNameIgnoreCase("Hardware");
        verify(categoryRepository, never()).save(any());
        verify(mapper).toResponse(category);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenUpdatingWithBlankName() {
        Category category = category(1L, "Hardware", "Hardware support", true);
        UpdateCategoryRequestDTO request = updateRequest("   ", "Updated description");

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.updateCategory(category.getId(), request))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(category.getName()).isEqualTo("Hardware");
        assertThat(category.getDescription()).isEqualTo("Hardware support");
        verify(categoryRepository).findById(category.getId());
        verify(categoryRepository, never()).findByNameIgnoreCase(anyString());
        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenUpdatingWithBlankDescription() {
        Category category = category(1L, "Hardware", "Hardware support", true);
        UpdateCategoryRequestDTO request = updateRequest(null, "   ");

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.updateCategory(category.getId(), request))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(category.getName()).isEqualTo("Hardware");
        assertThat(category.getDescription()).isEqualTo("Hardware support");
        verify(categoryRepository).findById(category.getId());
        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenUpdatingNameUsedByAnotherCategory() {
        Category category = category(1L, "Hardware", "Hardware support", true);
        Category anotherCategory = category(2L, "Software", "Software support", true);
        UpdateCategoryRequestDTO request = updateRequest("  Software  ", null);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(categoryRepository.findByNameIgnoreCase("Software")).thenReturn(Optional.of(anotherCategory));

        assertThatThrownBy(() -> categoryService.updateCategory(category.getId(), request))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(category.getName()).isEqualTo("Hardware");
        assertThat(category.getDescription()).isEqualTo("Hardware support");
        verify(categoryRepository).findById(category.getId());
        verify(categoryRepository).findByNameIgnoreCase("Software");
        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldUpdateNameWhenExistingNameBelongsToSameCategory() {
        Category category = category(1L, "Hardware", "Hardware support", true);
        UpdateCategoryRequestDTO request = updateRequest("  HARDWARE  ", null);
        CategoryResponseDTO response = response(1L, "HARDWARE", "Hardware support", true);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(categoryRepository.findByNameIgnoreCase("HARDWARE")).thenReturn(Optional.of(category));
        when(mapper.toResponse(category)).thenReturn(response);

        CategoryResponseDTO result = categoryService.updateCategory(category.getId(), request);

        assertThat(result).isSameAs(response);
        assertThat(category.getName()).isEqualTo("HARDWARE");
        assertThat(category.getDescription()).isEqualTo("Hardware support");
        verify(categoryRepository).findById(category.getId());
        verify(categoryRepository).findByNameIgnoreCase("HARDWARE");
        verify(categoryRepository, never()).save(any());
        verify(mapper).toResponse(category);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUpdatingUnknownCategory() {
        UpdateCategoryRequestDTO request = updateRequest("Hardware", "Hardware support");

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository).findById(99L);
        verify(categoryRepository, never()).findByNameIgnoreCase(anyString());
        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldActivateInactiveCategory() {
        Category category = category(1L, "Hardware", "Hardware support", false);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        categoryService.activateCategory(category.getId());

        assertThat(category.isActive()).isTrue();
        verify(categoryRepository).findById(category.getId());
        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenActivatingAlreadyActiveCategory() {
        Category category = category(1L, "Hardware", "Hardware support", true);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.activateCategory(category.getId()))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(category.isActive()).isTrue();
        verify(categoryRepository).findById(category.getId());
        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenActivatingUnknownCategory() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.activateCategory(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository).findById(99L);
        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldDeactivateActiveCategory() {
        Category category = category(1L, "Hardware", "Hardware support", true);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        categoryService.deactivateCategory(category.getId());

        assertThat(category.isActive()).isFalse();
        verify(categoryRepository).findById(category.getId());
        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenDeactivatingAlreadyInactiveCategory() {
        Category category = category(1L, "Hardware", "Hardware support", false);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.deactivateCategory(category.getId()))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(category.isActive()).isFalse();
        verify(categoryRepository).findById(category.getId());
        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenDeactivatingUnknownCategory() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deactivateCategory(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository).findById(99L);
        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    private Category category(Long id, String name, String description, boolean active) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDescription(description);
        category.setActive(active);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return category;
    }

    private CategoryRequestDTO categoryRequest(String name, String description) {
        return new CategoryRequestDTO(name, description);
    }

    private UpdateCategoryRequestDTO updateRequest(String name, String description) {
        return new UpdateCategoryRequestDTO(name, description);
    }

    private CategoryResponseDTO response(Category category) {
        return response(category.getId(), category.getName(), category.getDescription(), category.isActive());
    }

    private CategoryResponseDTO response(Long id, String name, String description, boolean active) {
        return new CategoryResponseDTO(
                id,
                name,
                description,
                active,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
