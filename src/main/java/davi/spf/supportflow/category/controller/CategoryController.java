package davi.spf.supportflow.category.controller;

import davi.spf.supportflow.category.dto.CategoryRequestDTO;
import davi.spf.supportflow.category.dto.CategoryResponseDTO;
import davi.spf.supportflow.category.dto.UpdateCategoryRequestDTO;
import davi.spf.supportflow.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/categories")
@Tag(name = "3. Categories", description = "Administração de categorias de chamados")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Lista categorias ativas")
    public ResponseEntity<Page<CategoryResponseDTO>> listCategories(Pageable pageable) {
        return ResponseEntity.ok(categoryService.listCategories(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca categoria por id")
    public ResponseEntity<CategoryResponseDTO> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findCategoryById(id));
    }

    @PostMapping
    @Operation(summary = "Cria uma categoria")
    public ResponseEntity<CategoryResponseDTO> createCategory(@RequestBody @Valid CategoryRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(dto));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Ativa uma categoria")
    public ResponseEntity<Void> activateCategory(@PathVariable Long id) {
        categoryService.activateCategory(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Desativa uma categoria")
    public ResponseEntity<Void> deactivateCategory(@PathVariable Long id) {
        categoryService.deactivateCategory(id);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma categoria")
    public ResponseEntity<CategoryResponseDTO> updateCategory(@PathVariable Long id,
                                                              @RequestBody UpdateCategoryRequestDTO dto) {

        return ResponseEntity.ok(categoryService.updateCategory(id, dto));
    }
}
