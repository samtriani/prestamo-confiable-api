package mx.empenya.confiable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EmpenyaConfiableApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmpenyaConfiableApplication.class, args);
    }
}
