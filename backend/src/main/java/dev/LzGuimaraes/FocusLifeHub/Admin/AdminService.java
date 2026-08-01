package dev.LzGuimaraes.FocusLifeHub.Admin;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.User.Role;
import dev.LzGuimaraes.FocusLifeHub.User.UserMapper;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;
import dev.LzGuimaraes.FocusLifeHub.User.dto.response.UserResponseDTO;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public AdminService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public List<UserResponseDTO> listUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional
    public UserResponseDTO updateRole(Long userId, Role role) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com ID " + userId + " não encontrado"));
        user.setRole(role);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Usuário com ID " + userId + " não encontrado");
        }
        try {
            userRepository.deleteById(userId);
        } catch (DataIntegrityViolationException ex) {
            throw new RuntimeException("Não é possível excluir este usuário: ele possui dados associados (finanças, contas, etc.).");
        }
    }
}
