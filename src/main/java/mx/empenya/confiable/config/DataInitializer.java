package mx.empenya.confiable.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.empenya.confiable.service.AuthService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final AuthService    authService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        authService.inicializarAdminSiEsNecesario(passwordEncoder);
        log.info("DataInitializer: verificación de usuario admin completada");
    }
}
