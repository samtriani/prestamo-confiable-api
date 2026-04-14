package mx.empenya.confiable.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import mx.empenya.confiable.repository.UsuarioRepository;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService        jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest  request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain         chain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token    = authHeader.substring(7);
        String username = jwtService.extractUsername(token);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            usuarioRepository.findByUsername(username).ifPresent(usuario -> {
                if (jwtService.isTokenValid(token, username)) {
                    var auth = new UsernamePasswordAuthenticationToken(
                        usuario, null, usuario.getAuthorities()
                    );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            });
        }
        chain.doFilter(request, response);
    }
}
