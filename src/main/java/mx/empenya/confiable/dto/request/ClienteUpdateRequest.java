package mx.empenya.confiable.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClienteUpdateRequest {

    @Size(max = 20)
    private String telefono;

    @Size(max = 500)
    private String domicilio;
}
