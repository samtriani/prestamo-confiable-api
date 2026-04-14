package mx.empenya.confiable.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter   jwtAuthFilter;
    private final CorsConfig      corsConfig;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .addFilterBefore(corsConfig.corsFilter(), UsernamePasswordAuthenticationFilter.class)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // login público
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                // actuator/docs públicos
                .requestMatchers("/actuator/**", "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // CLIENTE: solo su crédito
                .requestMatchers(HttpMethod.GET, "/auth/mi-credito").hasRole("CLIENTE")
                // cortes: solo ADMIN puede crear
                .requestMatchers(HttpMethod.POST, "/cortes").hasRole("ADMIN")
                // gestión usuarios: solo ADMIN
                .requestMatchers("/usuarios/**").hasRole("ADMIN")
                // CLIENTE no puede acceder a nada más
                .requestMatchers("/**").not().hasRole("CLIENTE")
                // todo lo demás requiere autenticación
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
