package com.noctua.backend.entity.Usuario;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "admins")
@Getter
@Setter
@NoArgsConstructor


public class AdminEntity {
    
    @Id
    private Long id;

    @OneToOne(optional=false, cascade = jakarta.persistence.CascadeType.ALL)
    @MapsId
    @JoinColumn(name = "id")
    private UsuarioEntity usuario;
}
