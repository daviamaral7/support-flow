package davi.spf.supportflow.category.controller;

import davi.spf.supportflow.category.dto.CategoryRequestDTO;
import davi.spf.supportflow.category.dto.CategoryResponseDTO;
import davi.spf.supportflow.category.dto.UpdateCategoryRequestDTO;
import davi.spf.supportflow.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<Page<CategoryResponseDTO>> listCategories(Pageable pageable) {
        return ResponseEntity.ok(categoryService.listCategories(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findCategoryById(id));
    }

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> createCategory(@RequestBody @Valid CategoryRequestDTO dto) {
        return ResponseEntity.ok(categoryService.createCategory(dto));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateCategory(@PathVariable Long id) {
        categoryService.activateCategory(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateCategory(@PathVariable Long id) {
        categoryService.deactivateCategory(id);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> updateCategory(@PathVariable Long id,
                                                              @RequestBody UpdateCategoryRequestDTO dto) {

        return ResponseEntity.ok(categoryService.updateCategory(id, dto));
    }
}
