package dev.LzGuimaraes.FocusLifeHub.Tarefas;

import org.springframework.stereotype.Component;

import dev.LzGuimaraes.FocusLifeHub.Tarefas.dto.TarefasRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.dto.TarefasResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;

@Component
public class TarefasMapper {

    public TarefasModel toModel(TarefasRequestDTO dto, UserModel user) {
        if (dto == null) {
            return null;
        }

        TarefasModel tarefa = new TarefasModel();
        tarefa.setTitulo(dto.titulo());
        tarefa.setStatus(dto.status());
        tarefa.setPrioridade(dto.prioridade()); 
        tarefa.setPrazo(dto.prazo());
        tarefa.setUser(user);

        return tarefa;
    }

    public TarefasResponseDTO toResponse(TarefasModel tarefa) {
        if (tarefa == null) {
            return null;
        }

        Long userId = (tarefa.getUser() != null) ? tarefa.getUser().getId() : null;

        return new TarefasResponseDTO(
            tarefa.getId(),
            tarefa.getTitulo(),
            tarefa.getStatus(),
            tarefa.getPrioridade(),
            tarefa.getPrazo(),
            userId
        );
    }
}