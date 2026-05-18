package com.atalayas.backend.sugerencia;

import com.atalayas.backend.usuario.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sugerencia")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sugerencia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @Column(nullable = false)
    private OffsetDateTime creadoEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoSugerencia estado = EstadoSugerencia.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DestinatarioSugerencia destinatario = DestinatarioSugerencia.EMPRESA;

    @PrePersist
    void prePersist() { creadoEn = OffsetDateTime.now(); }
}
