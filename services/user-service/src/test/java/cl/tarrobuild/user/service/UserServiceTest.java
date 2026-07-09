package cl.tarrobuild.user.service;

import cl.tarrobuild.user.dto.UserRequest;
import cl.tarrobuild.user.dto.UserResponse;
import cl.tarrobuild.user.dto.UserUpdateRequest;
import cl.tarrobuild.user.model.User;
import cl.tarrobuild.user.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserRequest userRequest;
    private UserUpdateRequest userUpdateRequest;
    private final LocalDateTime now = LocalDateTime.of(2026, 7, 8, 12, 0);

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Juan");
        user.setLastName("Pérez");
        user.setEmail("juan.perez@example.com");
        user.setPhone("12345678");
        user.setCreatedAt(now);

        userRequest = new UserRequest("Juan", "Pérez", "juan.perez@example.com", "12345678");
        userUpdateRequest = new UserUpdateRequest("Carlos", "García", "87654321");
    }

    // -------------------------------------------------------------------------
    // createUser
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería crear un usuario exitosamente")
    void createUser_Success() {
        when(userRepository.existsByEmail(userRequest.email())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.createUser(userRequest);

        assertNotNull(response);
        assertEquals(user.getId(), response.id());
        assertEquals(user.getName(), response.name());
        assertEquals(user.getLastName(), response.lastName());
        assertEquals(user.getEmail(), response.email());
        assertEquals(user.getPhone(), response.phone());
        assertEquals(user.getCreatedAt(), response.createdAt());

        verify(userRepository).existsByEmail(userRequest.email());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Debería lanzar excepción al crear usuario con email duplicado")
    void createUser_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByEmail(userRequest.email())).thenReturn(true);

        EntityExistsException exception = assertThrows(EntityExistsException.class,
                () -> userService.createUser(userRequest));

        assertTrue(exception.getMessage().contains(userRequest.email()));
        verify(userRepository).existsByEmail(userRequest.email());
        verify(userRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getUserById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería retornar usuario por ID cuando existe")
    void getUserById_Found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Juan", response.name());
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Debería lanzar excepción al buscar usuario por ID que no existe")
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.getUserById(99L));

        assertTrue(exception.getMessage().contains("99"));
        verify(userRepository).findById(99L);
    }

    // -------------------------------------------------------------------------
    // getUserByEmail
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería retornar usuario por email cuando existe")
    void getUserByEmail_Found() {
        when(userRepository.findByEmail("juan.perez@example.com")).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserByEmail("juan.perez@example.com");

        assertNotNull(response);
        assertEquals("juan.perez@example.com", response.email());
        verify(userRepository).findByEmail("juan.perez@example.com");
    }

    @Test
    @DisplayName("Debería lanzar excepción al buscar usuario por email que no existe")
    void getUserByEmail_NotFound_ThrowsException() {
        when(userRepository.findByEmail("no@existe.com")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.getUserByEmail("no@existe.com"));

        assertTrue(exception.getMessage().contains("no@existe.com"));
        verify(userRepository).findByEmail("no@existe.com");
    }

    // -------------------------------------------------------------------------
    // getUserByPhone
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería retornar usuario por teléfono cuando existe")
    void getUserByPhone_Found() {
        when(userRepository.findByPhone("12345678")).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserByPhone("12345678");

        assertNotNull(response);
        assertEquals("12345678", response.phone());
        verify(userRepository).findByPhone("12345678");
    }

    @Test
    @DisplayName("Debería lanzar excepción al buscar usuario por teléfono que no existe")
    void getUserByPhone_NotFound_ThrowsException() {
        when(userRepository.findByPhone("00000000")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.getUserByPhone("00000000"));

        assertTrue(exception.getMessage().contains("00000000"));
        verify(userRepository).findByPhone("00000000");
    }

    // -------------------------------------------------------------------------
    // getUsers
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería filtrar usuarios por nombre")
    void getUsers_FilterByName() {
        when(userRepository.findByName("Juan")).thenReturn(List.of(user));

        List<UserResponse> responses = userService.getUsers("Juan", null);

        assertEquals(1, responses.size());
        assertEquals("Juan", responses.getFirst().name());
        verify(userRepository).findByName("Juan");
        verify(userRepository, never()).findAll();
        verify(userRepository, never()).findByLastName(any());
        verify(userRepository, never()).findByNameAndLastName(any(), any());
    }

    @Test
    @DisplayName("Debería filtrar usuarios por apellido")
    void getUsers_FilterByLastName() {
        when(userRepository.findByLastName("Pérez")).thenReturn(List.of(user));

        List<UserResponse> responses = userService.getUsers(null, "Pérez");

        assertEquals(1, responses.size());
        assertEquals("Pérez", responses.getFirst().lastName());
        verify(userRepository).findByLastName("Pérez");
    }

    @Test
    @DisplayName("Debería filtrar usuarios por nombre y apellido simultáneamente")
    void getUsers_FilterByNameAndLastName() {
        when(userRepository.findByNameAndLastName("Juan", "Pérez")).thenReturn(List.of(user));

        List<UserResponse> responses = userService.getUsers("Juan", "Pérez");

        assertEquals(1, responses.size());
        assertEquals("Juan", responses.getFirst().name());
        assertEquals("Pérez", responses.getFirst().lastName());
        verify(userRepository).findByNameAndLastName("Juan", "Pérez");
    }

    @Test
    @DisplayName("Debería retornar todos los usuarios cuando no hay filtros")
    void getUsers_NoFilters_ReturnsAll() {
        User user2 = new User();
        user2.setId(2L);
        user2.setName("María");
        user2.setLastName("López");
        user2.setEmail("maria@example.com");
        user2.setPhone("87654321");
        user2.setCreatedAt(now);

        when(userRepository.findAll()).thenReturn(List.of(user, user2));

        List<UserResponse> responses = userService.getUsers(null, null);

        assertEquals(2, responses.size());
        verify(userRepository).findAll();
    }

    // -------------------------------------------------------------------------
    // updateUser
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería actualizar un usuario exitosamente")
    void updateUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.updateUser(1L, userUpdateRequest);

        assertNotNull(response);
        assertEquals("Carlos", response.name());
        assertEquals("García", response.lastName());
        assertEquals("87654321", response.phone());

        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Debería lanzar excepción al actualizar usuario que no existe")
    void updateUser_NotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.updateUser(99L, userUpdateRequest));

        assertTrue(exception.getMessage().contains("99"));
        verify(userRepository).findById(99L);
        verify(userRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // patchUser
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería aplicar patch parcial a un usuario exitosamente")
    void patchUser_PartialFields_Success() {
        UserUpdateRequest partialUpdate = new UserUpdateRequest(null, "González", null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.patchUser(1L, partialUpdate);

        assertNotNull(response);
        assertEquals("Juan", response.name());
        assertEquals("González", response.lastName());
        assertEquals("12345678", response.phone());

        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Debería lanzar excepción al aplicar patch a usuario que no existe")
    void patchUser_NotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.patchUser(99L, userUpdateRequest));

        assertTrue(exception.getMessage().contains("99"));
        verify(userRepository).findById(99L);
        verify(userRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // deleteUser
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Debería eliminar un usuario exitosamente")
    void deleteUser_Success() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Debería lanzar excepción al eliminar usuario que no existe")
    void deleteUser_NotFound_ThrowsException() {
        when(userRepository.existsById(99L)).thenReturn(false);

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.deleteUser(99L));

        assertTrue(exception.getMessage().contains("99"));
        verify(userRepository).existsById(99L);
        verify(userRepository, never()).deleteById(any());
    }
}
