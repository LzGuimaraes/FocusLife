package dev.LzGuimaraes.FocusLifeHub.User;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder; 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; 

import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.User.dto.request.UserUpdateDTO;
import dev.LzGuimaraes.FocusLifeHub.User.dto.response.UserResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.config.JWTUserData;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder; 

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder; 
    }

    public UserResponseDTO getMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JWTUserData jwtData = (JWTUserData) authentication.getPrincipal();

        UserModel user = userRepository.findById(jwtData.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com ID " + jwtData.userId() + " não encontrado"));

        return userMapper.toResponse(user);
    }

    @Transactional 
    public UserResponseDTO updateMe(UserUpdateDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JWTUserData jwtData = (JWTUserData) authentication.getPrincipal();
        UserModel user = userRepository.findById(jwtData.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário " + jwtData.userId() + " não encontrado para atualizar"));

        if (dto.nome() != null && !dto.nome().isBlank() && !dto.nome().equals(user.getNome())) {
            user.setNome(dto.nome());
        }

        if (dto.email() != null && !dto.email().isBlank() && !dto.email().equals(user.getEmail())) {
            
            if (userRepository.existsByEmail(dto.email())) {
                throw new RuntimeException("Erro: Este e-mail já está em uso."); 
            }
            user.setEmail(dto.email());
        }

        if (dto.password() != null && !dto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.password()));
        }

        UserModel updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }
}