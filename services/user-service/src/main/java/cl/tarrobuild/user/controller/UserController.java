package cl.tarrobuild.user.controller;

import cl.tarrobuild.user.dto.UserRequest;
import cl.tarrobuild.user.dto.UserUpdateRequest;
import cl.tarrobuild.user.dto.UserResponse;
import cl.tarrobuild.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping()
    public ResponseEntity<List<UserResponse>> getUsers(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "last_name", required = false) String lastName) {
        return ResponseEntity.ok(userService.getUsers(name, lastName));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping("/phone/{phone}")
    public ResponseEntity<UserResponse> getUserByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(userService.getUserByPhone(phone));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest user) {
        UserResponse created = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest user) {
        return ResponseEntity.ok(userService.updateUser(id, user));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> patchUser(@PathVariable Long id, @RequestBody UserUpdateRequest user) {
        return ResponseEntity.ok(userService.patchUser(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}