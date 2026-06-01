package cl.tarrobuild.user.service;

import cl.tarrobuild.user.dto.UserRequest;
import cl.tarrobuild.user.dto.UserUpdateRequest;
import cl.tarrobuild.user.dto.UserResponse;
import cl.tarrobuild.user.model.User;
import cl.tarrobuild.user.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private UserResponse toResponse(User user){
        return new UserResponse(
                user.getId(), user.getName(), user.getLastName(),
                user.getEmail(), user.getPhone(), user.getCreatedAt());
    }

    public UserResponse getUserById(Long id) {
        log.info("Getting user by id: {}", id);
        return userRepository
                .findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("User with ID " + id + " not found"));
    }

    public List<UserResponse> getUsers(String name, String lastName){
        log.info("Getting users with name: {} and lastName: {}", name, lastName);
        List<User> users;
        if (name != null && lastName != null){
            users = userRepository.findByNameAndLastName(name, lastName);
        } else if (name != null) {
            users = userRepository.findByName(name);
        } else if (lastName != null) {
             users = userRepository.findByLastName(lastName);
        } else {
            users = userRepository.findAll();
        }

        return users.stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getUserByEmail(String email) {
        log.info("Getting user by email: {}", email);
        return userRepository
                .findByEmail(email)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("User with email " + email + " not found"));
    }

    public UserResponse getUserByPhone(String phone) {
        log.info("Getting user by phone: {}", phone);
        return userRepository
                .findByPhone(phone)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("User with phone " + phone + " not found"));
    }

    public UserResponse createUser(UserRequest request) {
        log.info("Creating user with email: {}", request.email());
        if (userRepository.existsByEmail(request.email())) {
            throw new EntityExistsException("Email: \"" + request.email() + "\" already exists");
        }
        User user = new User();
        user.setName(request.name());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setCreatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        return this.toResponse(saved);
    }

    public UserResponse updateUser(Long id, UserUpdateRequest userData) {
        log.info("Updating user id: {}", id);
        return userRepository.findById(id)
                .map(user ->{
                    user.setName(userData.name());
                    user.setLastName(userData.lastName());
                    user.setPhone(userData.phone());
                    User saved = userRepository.save(user);
                    return toResponse(saved);
                })
                .orElseThrow(() -> new EntityNotFoundException("User with ID " + id + " not found"));
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            log.info("User with id: {} not found for deletion", id);
            throw new EntityNotFoundException("User with ID " + id + " not found");
        }
        userRepository.deleteById(id);
        log.info("User with id: {} deleted successfully", id);
    }
}
