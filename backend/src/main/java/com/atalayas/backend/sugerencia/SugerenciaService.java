package com.atalayas.backend.sugerencia;

import com.atalayas.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SugerenciaService {

    private final SugerenciaRepository repo;

    public void enviar(SugerenciaRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Sugerencia s = Sugerencia.builder()
                .mensaje(request.mensaje().trim())
                .usuario(user)
                .destinatario(request.destinatario())
                .build();
        repo.save(s);
    }

    public List<SugerenciaResponse> listar() {
        return repo.findAllByOrderByCreadoEnDesc().stream()
                .map(s -> new SugerenciaResponse(
                        s.getId(),
                        s.getMensaje(),
                        s.getUsuario().getNombre() + " " + (s.getUsuario().getApellidos() != null ? s.getUsuario().getApellidos() : ""),
                        s.getUsuario().getEmail(),
                        s.getEstado(),
                        s.getDestinatario(),
                        s.getCreadoEn()
                ))
                .toList();
    }
}
