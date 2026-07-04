package davi.spf.supportflow.category.service;

import davi.spf.supportflow.category.dto.CategoryResponseDTO;
import davi.spf.supportflow.category.entity.Category;
import davi.spf.supportflow.category.mapper.CategoryMapper;
import davi.spf.supportflow.category.repository.CategoryRepository;
import davi.spf.supportflow.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper mapper;

    @Transactional(readOnly = true)
    public Page<CategoryResponseDTO> listCategories(Pageable pageable) {
        return categoryRepository.findAllByActiveTrue(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CategoryResponseDTO findCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Category not found"));

        return mapper.toResponse(category);
    }
}
