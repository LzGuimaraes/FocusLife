package dev.LzGuimaraes.FocusLifeHub.User;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.LzGuimaraes.FocusLifeHub.User.dto.request.UserUpdateDTO;
import dev.LzGuimaraes.FocusLifeHub.User.dto.response.UserResponseDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/users") 
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMe() {
        UserResponseDTO user = userService.getMe();
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> updateMe(@Valid @RequestBody UserUpdateDTO dto) {
        UserResponseDTO updatedUser = userService.updateMe(dto);
        return ResponseEntity.ok(updatedUser);
    }
}