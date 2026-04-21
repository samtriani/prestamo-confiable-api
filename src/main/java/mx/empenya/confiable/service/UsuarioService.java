package mx.empenya.confiable.service;

import lombok.RequiredArgsConstructor;
import mx.empenya.confiable.dto.request.CreateUsuarioRequest;
import mx.empenya.confiable.dto.request.UpdateUsuarioRequest;
import mx.empenya.confiable.dto.response.UsuarioResponse;
import mx.empenya.confiable.entity.Usuario;
import mx.empenya.confiable.exception.BusinessException;
import mx.empenya.confiable.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder   passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> findAll() {
        return usuarioRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public UsuarioResponse create(CreateUsuarioRequest request) {
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw BusinessException.conflict("El usuario '" + request.getUsername() + "' ya existe");
        }
        if (request.getRol() == mx.empenya.confiable.enums.RolUsuario.CLIENTE
                && request.getClienteId() == null) {
            throw BusinessException.badRequest("El rol CLIENTE requiere un clienteId");
        }
        Usuario usuario = usuarioRepository.save(Usuario.builder()
            .username(request.getUsername())
            .password(passwordEncoder.encode(request.getPassword()))
            .nombre(request.getNombre())
            .rol(request.getRol())
            .clienteId(request.getClienteId())
            .activo(true)
            .build());
        return toResponse(usuario);
    }

    @Transactional
    public UsuarioResponse update(UUID id, UpdateUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("Usuario", id.toString()));

        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            usuario.setNombre(request.getNombre());
        }
        if (request.getRol() != null) {
            if (request.getRol() == mx.empenya.confiable.enums.RolUsuario.CLIENTE
                    && request.getClienteId() == null && usuario.getClienteId() == null) {
                throw BusinessException.badRequest("El rol CLIENTE requiere un clienteId");
            }
            usuario.setRol(request.getRol());
        }
        if (request.getClienteId() != null) {
            usuario.setClienteId(request.getClienteId());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse toggleActivo(UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("Usuario", id.toString()));
        usuario.setActivo(!usuario.getActivo());
        return toResponse(usuarioRepository.save(usuario));
    }

    private UsuarioResponse toResponse(Usuario u) {
        return UsuarioResponse.builder()
            .id(u.getId())
            .username(u.getUsername())
            .nombre(u.getNombre())
            .rol(u.getRol())
            .activo(u.getActivo())
            .clienteId(u.getClienteId())
            .createdAt(u.getCreatedAt())
            .build();
    }
}
