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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper mapper;

    Category findCategoryByIdOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponseDTO> listCategories(Pageable pageable) {
        return categoryRepository.findAllByActiveTrue(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CategoryResponseDTO findCategoryById(Long id) {
        Category category = findCategoryByIdOrThrow(id);

        return mapper.toResponse(category);
    }

    public CategoryResponseDTO createCategory(CategoryRequestDTO dto) {
        String normalizedName = dto.name().trim();

        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ResourceAlreadyExistsException("Category already exists");
        }


        Category category = mapper.toEntity(dto);
        category.setName(normalizedName);

        Category savedCategory = categoryRepository.save(category);

        return mapper.toResponse(savedCategory);
    }

    public void activateCategory(Long id) {
        Category category = findCategoryByIdOrThrow(id);

        if (category.isActive()) {
            throw new BusinessRuleException("Category already active");
        }

        category.setActive(true);
    }

    public void deactivateCategory(Long id) {
        Category category = findCategoryByIdOrThrow(id);

        if (!category.isActive()) {
            throw new BusinessRuleException("Category is not active");
        }

        category.setActive(false);
    }

    public CategoryResponseDTO updateCategory(Long id, UpdateCategoryRequestDTO dto) {
        Category category = findCategoryByIdOrThrow(id);

        if (dto.name() != null) {
            if (dto.name().isBlank()) {
                throw new BusinessRuleException("Category name cannot be blank");
            }

            String normalizedName = dto.name().trim();

            Optional<Category> categoryByName = categoryRepository.findByNameIgnoreCase(normalizedName);

            if (categoryByName.isPresent() && !categoryByName.get().getId().equals(category.getId())) {
                throw new BusinessRuleException("Category name already taken");
            }

            category.setName(normalizedName);
        }

        if (dto.description() != null) {
            if (dto.description().isBlank()) {
                throw new BusinessRuleException("Description cannot be blank");
            }

            category.setDescription(dto.description().trim());
        }

        return mapper.toResponse(category);
    }
}
