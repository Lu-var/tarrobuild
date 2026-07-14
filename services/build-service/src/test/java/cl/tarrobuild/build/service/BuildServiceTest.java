package cl.tarrobuild.build.service;

import cl.tarrobuild.build.client.NotificationFeignClient;
import cl.tarrobuild.build.client.ProductFeignClient;
import cl.tarrobuild.build.dto.*;
import cl.tarrobuild.build.model.Build;
import cl.tarrobuild.build.model.BuildItem;
import cl.tarrobuild.build.model.BuildStatus;
import cl.tarrobuild.build.repository.BuildItemRepository;
import cl.tarrobuild.build.repository.BuildRepository;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildServiceTest {

    @Mock
    private BuildRepository buildRepository;

    @Mock
    private BuildItemRepository buildItemRepository;

    @Mock
    private ProductFeignClient productFeignClient;

    @Mock
    private NotificationFeignClient notificationFeignClient;

    @Mock
    private BuildHistoryService buildHistoryService;

    @InjectMocks
    private BuildService buildService;

    private Build sampleBuild;
    private BuildItem sampleItem;
    private BuildRequest buildRequest;
    private BuildItemRequest itemRequest;

    @BeforeEach
    void setUp() {
        sampleBuild = new Build();
        sampleBuild.setId(1L);
        sampleBuild.setUserId(10L);
        sampleBuild.setName("My Gaming Build");
        sampleBuild.setStatus(BuildStatus.DRAFT);
        sampleBuild.setCreatedAt(LocalDateTime.now());

        sampleItem = new BuildItem();
        sampleItem.setId(100L);
        sampleItem.setProductId(500L);
        sampleItem.setQuantity(2);
        sampleItem.setBuild(sampleBuild);

        sampleBuild.setItems(List.of(sampleItem));

        buildRequest = new BuildRequest(10L, "My Gaming Build");
        itemRequest = new BuildItemRequest(500L, 2);
    }

    // ========================================================================
    // createBuild
    // ========================================================================

    @Test
    @DisplayName("Should create a build successfully")
    void createBuild_Success() {
        when(buildRepository.save(any(Build.class))).thenReturn(sampleBuild);

        BuildResponse response = buildService.createBuild(buildRequest);

        assertNotNull(response);
        assertEquals(sampleBuild.getId(), response.id());
        assertEquals(sampleBuild.getUserId(), response.userId());
        assertEquals(sampleBuild.getName(), response.name());
        assertEquals(sampleBuild.getStatus().name(), response.status());
        assertNotNull(response.createdAt());

        verify(buildRepository).save(any(Build.class));
    }

    // ========================================================================
    // getBuildById
    // ========================================================================

    @Test
    @DisplayName("Should return build when found by ID")
    void getBuildById_Found() {
        when(buildRepository.findById(1L)).thenReturn(Optional.of(sampleBuild));

        BuildResponse response = buildService.getBuildById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(10L, response.userId());
        assertEquals("My Gaming Build", response.name());
        assertEquals("DRAFT", response.status());
        assertEquals(1, response.items().size());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when build not found by ID")
    void getBuildById_NotFound() {
        when(buildRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> buildService.getBuildById(99L));
        assertTrue(ex.getMessage().contains("99"));
    }

    // ========================================================================
    // getBuildsByUserId
    // ========================================================================

    @Test
    @DisplayName("Should return builds for a given user")
    void getBuildsByUserId_ReturnsBuilds() {
        when(buildRepository.findByUserId(10L)).thenReturn(List.of(sampleBuild));

        List<BuildResponse> responses = buildService.getBuildsByUserId(10L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.getFirst().id());
        assertEquals(10L, responses.getFirst().userId());
    }

    @Test
    @DisplayName("Should return empty list when user has no builds")
    void getBuildsByUserId_NoBuilds() {
        when(buildRepository.findByUserId(99L)).thenReturn(List.of());

        List<BuildResponse> responses = buildService.getBuildsByUserId(99L);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    // ========================================================================
    // getBuildByIdAndUserId
    // ========================================================================

    @Test
    @DisplayName("Should return build when found by ID and userId")
    void getBuildByIdAndUserId_Found() {
        when(buildRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(sampleBuild));

        BuildResponse response = buildService.getBuildByIdAndUserId(1L, 10L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(10L, response.userId());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when build not found for user")
    void getBuildByIdAndUserId_NotFound() {
        when(buildRepository.findByIdAndUserId(99L, 10L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> buildService.getBuildByIdAndUserId(99L, 10L));
        assertTrue(ex.getMessage().contains("99"));
        assertTrue(ex.getMessage().contains("10"));
    }

    // ========================================================================
    // updateBuild
    // ========================================================================

    @Test
    @DisplayName("Should update build successfully")
    void updateBuild_Success() {
        BuildRequest updateRequest = new BuildRequest(20L, "Updated Build");

        when(buildRepository.findById(1L)).thenReturn(Optional.of(sampleBuild));
        when(buildRepository.save(any(Build.class))).thenAnswer(invocation -> {
            Build b = invocation.getArgument(0);
            b.setCreatedAt(LocalDateTime.now());
            return b;
        });

        BuildResponse response = buildService.updateBuild(1L, updateRequest);

        assertNotNull(response);
        assertEquals(20L, response.userId());
        assertEquals("Updated Build", response.name());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating non-existent build")
    void updateBuild_NotFound() {
        when(buildRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> buildService.updateBuild(99L, buildRequest));
    }

    // ========================================================================
    // updateBuildStatus
    // ========================================================================

    @Test
    @DisplayName("Should update build status successfully")
    void updateBuildStatus_Success() {
        when(buildRepository.findById(1L)).thenReturn(Optional.of(sampleBuild));
        when(buildRepository.save(any(Build.class))).thenAnswer(invocation -> {
            Build b = invocation.getArgument(0);
            b.setCreatedAt(LocalDateTime.now());
            return b;
        });

        BuildResponse response = buildService.updateBuildStatus(1L, BuildStatus.VALIDATED);

        assertNotNull(response);
        assertEquals(BuildStatus.VALIDATED.name(), response.status());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating status of non-existent build")
    void updateBuildStatus_NotFound() {
        when(buildRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> buildService.updateBuildStatus(99L, BuildStatus.VALIDATED));
    }

    // ========================================================================
    // deleteBuild
    // ========================================================================

    @Test
    @DisplayName("Should delete build successfully and return true")
    void deleteBuild_Success() {
        when(buildRepository.existsById(1L)).thenReturn(true);

        boolean result = buildService.deleteBuild(1L);

        assertTrue(result);
        verify(buildRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should return false when deleting non-existent build")
    void deleteBuild_NotFound() {
        when(buildRepository.existsById(99L)).thenReturn(false);

        boolean result = buildService.deleteBuild(99L);

        assertFalse(result);
        verify(buildRepository, never()).deleteById(any());
    }

    // ========================================================================
    // createItem
    // ========================================================================

    @Test
    @DisplayName("Should create item successfully for an active product")
    void createItem_Success() {
        ProductClientResponse product = new ProductClientResponse(500L, "RTX 4090", true);

        when(buildRepository.findById(1L)).thenReturn(Optional.of(sampleBuild));
        when(productFeignClient.getProductById(500L)).thenReturn(product);
        when(buildItemRepository.save(any(BuildItem.class))).thenReturn(sampleItem);
        when(buildItemRepository.findByBuild_Id(1L)).thenReturn(List.of(sampleItem));

        BuildItemResponse response = buildService.createItem(1L, itemRequest);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(1L, response.buildId());
        assertEquals(500L, response.productId());
        assertEquals(2, response.quantity());

        verify(buildItemRepository).save(any(BuildItem.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when build not found for item creation")
    void createItem_BuildNotFound() {
        when(buildRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> buildService.createItem(99L, itemRequest));

        verify(productFeignClient, never()).getProductById(any());
        verify(buildItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when product not found via Feign client")
    void createItem_ProductNotFoundViaFeign() {
        FeignException.NotFound notFound = mock(FeignException.NotFound.class);

        when(buildRepository.findById(1L)).thenReturn(Optional.of(sampleBuild));
        when(productFeignClient.getProductById(500L)).thenThrow(notFound);

        assertThrows(EntityNotFoundException.class,
                () -> buildService.createItem(1L, itemRequest));

        verify(buildItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when product is not active")
    void createItem_ProductNotActive() {
        ProductClientResponse inactiveProduct = new ProductClientResponse(500L, "RTX 4090", false);

        when(buildRepository.findById(1L)).thenReturn(Optional.of(sampleBuild));
        when(productFeignClient.getProductById(500L)).thenReturn(inactiveProduct);

        assertThrows(IllegalArgumentException.class,
                () -> buildService.createItem(1L, itemRequest));

        verify(buildItemRepository, never()).save(any());
    }

    // ========================================================================
    // getItemsByBuildId
    // ========================================================================

    @Test
    @DisplayName("Should return items for a build")
    void getItemsByBuildId_ReturnsItems() {
        when(buildRepository.findById(1L)).thenReturn(Optional.of(sampleBuild));
        when(buildItemRepository.findByBuild_Id(1L)).thenReturn(List.of(sampleItem));

        List<BuildItemResponse> responses = buildService.getItemsByBuildId(1L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(100L, responses.getFirst().id());
        assertEquals(500L, responses.getFirst().productId());
        assertEquals(2, responses.getFirst().quantity().intValue());
    }

    @Test
    @DisplayName("Should return empty list when build has no items")
    void getItemsByBuildId_NoItems() {
        when(buildRepository.findById(1L)).thenReturn(Optional.of(sampleBuild));
        when(buildItemRepository.findByBuild_Id(1L)).thenReturn(List.of());

        List<BuildItemResponse> responses = buildService.getItemsByBuildId(1L);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when build not found for items retrieval")
    void getItemsByBuildId_BuildNotFound() {
        when(buildRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> buildService.getItemsByBuildId(99L));
    }

    // ========================================================================
    // getItemByIdAndBuildId
    // ========================================================================

    @Test
    @DisplayName("Should return item when found by ID and buildId")
    void getItemByIdAndBuildId_Found() {
        when(buildItemRepository.findByIdAndBuild_Id(100L, 1L)).thenReturn(Optional.of(sampleItem));

        BuildItemResponse response = buildService.getItemByIdAndBuildId(100L, 1L);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(1L, response.buildId());
        assertEquals(500L, response.productId());
        assertEquals(2, response.quantity());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when item not found")
    void getItemByIdAndBuildId_NotFound() {
        when(buildItemRepository.findByIdAndBuild_Id(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> buildService.getItemByIdAndBuildId(999L, 1L));
    }

    // ========================================================================
    // updateItem
    // ========================================================================

    @Test
    @DisplayName("Should update item successfully")
    void updateItem_Success() {
        BuildItemRequest updateItemRequest = new BuildItemRequest(600L, 3);

        when(buildRepository.findById(1L)).thenReturn(Optional.of(sampleBuild));
        when(buildItemRepository.findByIdAndBuild_Id(100L, 1L)).thenReturn(Optional.of(sampleItem));
        when(buildItemRepository.save(any(BuildItem.class))).thenAnswer(invocation -> {
            BuildItem item = invocation.getArgument(0);
            item.setId(100L);
            return item;
        });
        when(buildItemRepository.findByBuild_Id(1L)).thenReturn(List.of(sampleItem, new BuildItem()));

        BuildItemResponse response = buildService.updateItem(1L, 100L, updateItemRequest);

        assertNotNull(response);
        assertEquals(600L, response.productId());
        assertEquals(3, response.quantity());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating item for non-existent build")
    void updateItem_BuildNotFound() {
        when(buildRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> buildService.updateItem(99L, 100L, itemRequest));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating non-existent item")
    void updateItem_ItemNotFound() {
        when(buildRepository.findById(1L)).thenReturn(Optional.of(sampleBuild));
        when(buildItemRepository.findByIdAndBuild_Id(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> buildService.updateItem(1L, 999L, itemRequest));
    }

    // ========================================================================
    // deleteItem
    // ========================================================================

    @Test
    @DisplayName("Should delete item successfully")
    void deleteItem_Success() {
        when(buildItemRepository.findByIdAndBuild_Id(100L, 1L)).thenReturn(Optional.of(sampleItem));
        when(buildItemRepository.findByBuild_Id(1L)).thenReturn(List.of(sampleItem, new BuildItem()));

        buildService.deleteItem(100L, 1L);

        verify(buildItemRepository).delete(sampleItem);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when deleting non-existent item")
    void deleteItem_NotFound() {
        when(buildItemRepository.findByIdAndBuild_Id(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> buildService.deleteItem(999L, 1L));

        verify(buildItemRepository, never()).delete(any());
    }
}
