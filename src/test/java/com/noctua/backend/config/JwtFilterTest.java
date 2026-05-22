package com.noctua.backend.config;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private JwtFilter jwtFilter;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    // Teste 1: quando o header Bearer possui token válido, autentica o e-mail no SecurityContext.
    @Test
    void doFilterDeveAutenticarQuandoTokenForValido() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        request.addHeader("Authorization", "Bearer token-valido");

        when(jwtUtil.isValid("token-valido")).thenReturn(true);
        when(jwtUtil.extractEmail("token-valido")).thenReturn("teste@email.com");

        jwtFilter.doFilter(request, response, filterChain);

        assertEquals("teste@email.com", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(jwtUtil).isValid("token-valido");
        verify(jwtUtil).extractEmail("token-valido");
    }

    // Teste 2: quando nao há header Authorization, apenas segue a cadeia de filtros.
    @Test
    void doFilterNaoDeveAutenticarQuandoHeaderNaoExistir() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtil, never()).isValid(org.mockito.Mockito.anyString());
        verify(jwtUtil, never()).extractEmail(org.mockito.Mockito.anyString());
    }

    // Teste 3: quando o token e inválido, não autentica usuário.
    @Test
    void doFilterNaoDeveAutenticarQuandoTokenForInvalido() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        request.addHeader("Authorization", "Bearer token-invalido");

        when(jwtUtil.isValid("token-invalido")).thenReturn(false);

        jwtFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtil).isValid("token-invalido");
        verify(jwtUtil, never()).extractEmail(org.mockito.Mockito.anyString());
    }
}
