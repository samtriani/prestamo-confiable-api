package mx.empenya.confiable.dto.response;

import lombok.Builder;
import lombok.Data;
import mx.empenya.confiable.enums.RolUsuario;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UsuarioResponse {
    private UUID          id;
    private String        username;
    private String        nombre;
    private RolUsuario    rol;
    private Boolean       activo;
    private java.util.UUID clienteId;
    private LocalDateTime createdAt;
}
