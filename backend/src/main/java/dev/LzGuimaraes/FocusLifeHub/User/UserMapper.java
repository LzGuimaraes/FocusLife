package dev.LzGuimaraes.FocusLifeHub.User;


import org.springframework.stereotype.Component;

import dev.LzGuimaraes.FocusLifeHub.User.dto.response.UserResponseDTO;

@Component
public class UserMapper {

    public UserResponseDTO toResponse(UserModel user) {
        if (user == null) {
            return null;
        }
        return new UserResponseDTO(
            user.getId(),
            user.getNome(),
            user.getEmail()
        );
    }
}
