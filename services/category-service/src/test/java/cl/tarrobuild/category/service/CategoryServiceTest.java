package cl.tarrobuild.category.service;

import cl.tarrobuild.category.dto.AttributeDefinitionRequest;
import cl.tarrobuild.category.dto.AttributeDefinitionResponse;
import cl.tarrobuild.category.dto.CategoryRequest;
import cl.tarrobuild.category.dto.CategoryResponse;
import cl.tarrobuild.category.model.AttributeDefinition;
import cl.tarrobuild.category.model.AttributeValueType;
import cl.tarrobuild.category.model.Category;
import cl.tarrobuild.category.repository.AttributeDefinitionRepository;
import cl.tarrobuild.category.repository.CategoryRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AttributeDefinitionRepository attributeDefinitionRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;
    private CategoryRequest categoryRequest;
    private AttributeDefinition attributeDefinition;
    private AttributeDefinitionRequest attributeRequest;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Procesadores");
        category.setSlug("procesadores");
        category.setDescription("CPUs y procesadores");
        category.setIsActive(true);

        categoryRequest = new CategoryRequest(
                "Procesadores",
                "procesadores",
                "CPUs y procesadores",
                true
        );

        attributeDefinition = new AttributeDefinition();
        attributeDefinition.setId(10L);
        attributeDefinition.setAttributeName("Socket");
        attributeDefinition.setValueType(AttributeValueType.STRING);
        attributeDefinition.setIsRequired(true);
        attributeDefinition.setCategory(category);

        attributeRequest = new AttributeDefinitionRequest(
                "Socket",
                AttributeValueType.STRING,
                true
        );
    }

    // -------------------------------------------------------------------------
    // Tests CRUD — Categorías
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería crear una categoría exitosamente")
    void createCategory_Success() {
        when(categoryRepository.findBySlug(categoryRequest.slug())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponse response = categoryService.createCategory(categoryRequest);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Procesadores", response.name());
        assertEquals("procesadores", response.slug());
        assertEquals("CPUs y procesadores", response.description());
        assertTrue(response.isActive());

        verify(categoryRepository).findBySlug("procesadores");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Debería lanzar excepción al crear categoría con slug duplicado")
    void createCategory_DuplicateSlug_ThrowsException() {
        when(categoryRepository.findBySlug(categoryRequest.slug())).thenReturn(Optional.of(category));

        EntityExistsException exception = assertThrows(
                EntityExistsException.class,
                () -> categoryService.createCategory(categoryRequest)
        );

        assertEquals("Slug already exists", exception.getMessage());
        verify(categoryRepository).findBySlug("procesadores");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debería retornar una categoría existente por su ID")
    void getCategoryById_Found() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.getCategoryById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Procesadores", response.name());

        verify(categoryRepository).findById(1L);
    }

    @Test
    @DisplayName("Debería lanzar excepción al buscar categoría inexistente por ID")
    void getCategoryById_NotFound_ThrowsException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> categoryService.getCategoryById(99L)
        );

        assertEquals("Category not found", exception.getMessage());
        verify(categoryRepository).findById(99L);
    }

    @Test
    @DisplayName("Debería retornar todas las categorías")
    void getAllCategories_ReturnsList() {
        Category category2 = new Category();
        category2.setId(2L);
        category2.setName("Tarjetas de Video");
        category2.setSlug("tarjetas-de-video");
        category2.setDescription("GPUs");

        when(categoryRepository.findAll()).thenReturn(List.of(category, category2));

        List<CategoryResponse> responses = categoryService.getAllCategories();

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("Procesadores", responses.getFirst().name());
        assertEquals("Tarjetas de Video", responses.get(1).name());

        verify(categoryRepository).findAll();
    }

    @Test
    @DisplayName("Debería actualizar parcialmente una categoría exitosamente")
    void patchCategory_Success() {
        CategoryRequest patchRequest = new CategoryRequest(
                "Procesadores Actualizados",
                null,
                "Descripción actualizada",
                null
        );

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = categoryService.patchCategory(1L, patchRequest);

        assertNotNull(response);
        assertEquals("Procesadores Actualizados", response.name());
        assertEquals("procesadores", response.slug()); // unchanged
        assertEquals("Descripción actualizada", response.description());

        verify(categoryRepository).findById(1L);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Debería lanzar excepción al hacer patch con slug ya existente")
    void patchCategory_SlugConflict_ThrowsException() {
        CategoryRequest patchRequest = new CategoryRequest(
                null,
                "procesadores-nuevos",
                null,
                null
        );

        Category otherCategory = new Category();
        otherCategory.setId(2L);
        otherCategory.setSlug("procesadores-nuevos");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.findBySlug("procesadores-nuevos")).thenReturn(Optional.of(otherCategory));

        EntityExistsException exception = assertThrows(
                EntityExistsException.class,
                () -> categoryService.patchCategory(1L, patchRequest)
        );

        assertEquals("Slug already exists", exception.getMessage());
        verify(categoryRepository).findById(1L);
        verify(categoryRepository).findBySlug("procesadores-nuevos");
        verify(categoryRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Tests — Attribute Definitions
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería crear un atributo para una categoría exitosamente")
    void createAttribute_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(attributeDefinitionRepository.existsByAttributeNameAndCategoryId("Socket", 1L)).thenReturn(false);
        when(attributeDefinitionRepository.save(any(AttributeDefinition.class))).thenReturn(attributeDefinition);

        AttributeDefinitionResponse response = categoryService.createAttribute(1L, attributeRequest);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Socket", response.attributeName());
        assertEquals(AttributeValueType.STRING, response.valueType());
        assertTrue(response.isRequired());
        assertEquals(1L, response.categoryId());

        verify(categoryRepository).findById(1L);
        verify(attributeDefinitionRepository).existsByAttributeNameAndCategoryId("Socket", 1L);
        verify(attributeDefinitionRepository).save(any(AttributeDefinition.class));
    }

    @Test
    @DisplayName("Debería lanzar excepción al crear atributo duplicado en la misma categoría")
    void createAttribute_DuplicateName_ThrowsException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(attributeDefinitionRepository.existsByAttributeNameAndCategoryId("Socket", 1L)).thenReturn(true);

        EntityExistsException exception = assertThrows(
                EntityExistsException.class,
                () -> categoryService.createAttribute(1L, attributeRequest)
        );

        assertTrue(exception.getMessage().contains("Socket"));

        verify(categoryRepository).findById(1L);
        verify(attributeDefinitionRepository).existsByAttributeNameAndCategoryId("Socket", 1L);
        verify(attributeDefinitionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debería lanzar excepción al crear atributo para categoría inexistente")
    void createAttribute_CategoryNotFound_ThrowsException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> categoryService.createAttribute(99L, attributeRequest)
        );

        assertEquals("Category not found", exception.getMessage());
        verify(categoryRepository).findById(99L);
        verifyNoInteractions(attributeDefinitionRepository);
    }

    @Test
    @DisplayName("Debería retornar atributos de una categoría existente")
    void getAttributesByCategory_Success() {
        AttributeDefinition attr2 = new AttributeDefinition();
        attr2.setId(11L);
        attr2.setAttributeName("Núcleos");
        attr2.setValueType(AttributeValueType.NUMBER);
        attr2.setIsRequired(true);
        attr2.setCategory(category);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(attributeDefinitionRepository.findByCategoryId(1L)).thenReturn(List.of(attributeDefinition, attr2));

        List<AttributeDefinitionResponse> responses = categoryService.getAttributesByCategory(1L);

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("Socket", responses.getFirst().attributeName());
        assertEquals("Núcleos", responses.get(1).attributeName());

        verify(categoryRepository).findById(1L);
        verify(attributeDefinitionRepository).findByCategoryId(1L);
    }

    @Test
    @DisplayName("Debería lanzar excepción al consultar atributos de categoría inexistente")
    void getAttributesByCategory_CategoryNotFound_ThrowsException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> categoryService.getAttributesByCategory(99L)
        );

        assertEquals("Category not found", exception.getMessage());
        verify(categoryRepository).findById(99L);
        verifyNoInteractions(attributeDefinitionRepository);
    }
}
