package mx.empenya.confiable.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import mx.empenya.confiable.enums.RolUsuario;

@Data
public class UpdateUsuarioRequest {

    @Size(max = 150)
    private String nombre;

    private RolUsuario rol;

    /** Si viene vacío o null no se cambia la contraseña */
    @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
    private String password;

    // Solo requerido cuando rol = CLIENTE
    private java.util.UUID clienteId;
}
