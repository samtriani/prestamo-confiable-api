package mx.empenya.confiable.dto.response;

import lombok.Builder;
import lombok.Data;
import mx.empenya.confiable.enums.RolUsuario;

@Data
@Builder
public class LoginResponse {
    private String     token;
    private String     username;
    private String     nombre;
    private RolUsuario rol;
    private java.util.UUID clienteId;
}
