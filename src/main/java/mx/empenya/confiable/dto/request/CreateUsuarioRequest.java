package mx.empenya.confiable.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import mx.empenya.confiable.enums.RolUsuario;

@Data
public class CreateUsuarioRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank
    @Size(max = 150)
    private String nombre;

    @NotNull
    private RolUsuario rol;

    // Solo requerido cuando rol = CLIENTE
    private java.util.UUID clienteId;
}
