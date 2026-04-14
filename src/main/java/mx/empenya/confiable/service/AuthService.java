package mx.empenya.confiable.service;

import lombok.RequiredArgsConstructor;
import mx.empenya.confiable.config.JwtService;
import mx.empenya.confiable.dto.request.LoginRequest;
import mx.empenya.confiable.dto.response.LoginResponse;
import mx.empenya.confiable.entity.Usuario;
import mx.empenya.confiable.enums.RolUsuario;
import mx.empenya.confiable.exception.BusinessException;
import mx.empenya.confiable.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder   passwordEncoder;
    private final JwtService        jwtService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new BusinessException(
                "Credenciales incorrectas", org.springframework.http.HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"
            ));

        if (!usuario.isEnabled()) {
            throw new BusinessException(
                "Usuario inactivo", org.springframework.http.HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"
            );
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new BusinessException(
                "Credenciales incorrectas", org.springframework.http.HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"
            );
        }

        return LoginResponse.builder()
            .token(jwtService.generateToken(usuario))
            .username(usuario.getUsername())
            .nombre(usuario.getNombre())
            .rol(usuario.getRol())
            .clienteId(usuario.getClienteId())
            .build();
    }

    /** Crea el usuario admin inicial si no existe ningún usuario. */
    @Transactional
    public void inicializarAdminSiEsNecesario(PasswordEncoder encoder) {
        if (usuarioRepository.count() == 0) {
            usuarioRepository.save(Usuario.builder()
                .username("samuel.partida")
                .password(encoder.encode("samuel.partida"))
                .nombre("Samuel Partida")
                .rol(RolUsuario.ADMIN)
                .activo(true)
                .build());
        }
    }
}
