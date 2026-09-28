package com.tesis.repository;

import org.springframework.data.jpa.repository.JpaRepository;


import com.tesis.entity.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    
    Optional<Usuario> findByEmail(String email);
    
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
    
    boolean existsByEmail(String email);
    
    boolean existsByNombreUsuario(String nombreUsuario);
}