package com.noctua.backend.controller.turma;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.noctua.backend.dto.Turma.TurmaFiltrosDTO;
import com.noctua.backend.dto.Turma.TurmaRequestDTO;
import com.noctua.backend.dto.Turma.TurmaResponseDTO;
import com.noctua.backend.enums.Turno;
import com.noctua.backend.service.turma.TurmaService;

@ExtendWith(MockitoExtension.class)
class TurmaControllerTest {

    @Mock
    private TurmaService turmaService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TurmaController turmaController;

    // Teste 1: endpoint POST /turmas cria turma para o professor autenticado.
    @Test
    void criarDeveRetornarCreatedComTurmaCriada() {
        TurmaRequestDTO request = criarRequest();
        TurmaResponseDTO serviceResponse = criarResponse();

        when(authentication.getName()).thenReturn("prof@email.com");
        when(turmaService.criar("prof@email.com", request)).thenReturn(serviceResponse);

        ResponseEntity<TurmaResponseDTO> response = turmaController.criar(authentication, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(turmaService).criar("prof@email.com", request);
    }

    // Teste 2: endpoint GET /turmas/filtros retorna filtros disponíveis do professor autenticado.
    @Test
    void buscarFiltrosDeveRetornarFiltrosDoProfessorAutenticado() {
        TurmaFiltrosDTO serviceResponse = new TurmaFiltrosDTO(
                List.of(2026, 2025),
                List.of("IFSP"),
                List.of("Matematica"));

        when(authentication.getName()).thenReturn("prof@email.com");
        when(turmaService.buscarFiltros("prof@email.com")).thenReturn(serviceResponse);

        ResponseEntity<TurmaFiltrosDTO> response = turmaController.buscarFiltros(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(turmaService).buscarFiltros("prof@email.com");
    }

    // Teste 3: endpoint GET /turmas lista turmas paginadas com filtros.
    @Test
    void listarDeveRetornarPaginaDeTurmasComFiltros() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TurmaResponseDTO> serviceResponse = new PageImpl<>(List.of(criarResponse()), pageable, 1);

        when(authentication.getName()).thenReturn("prof@email.com");
        when(turmaService.listar(
                "prof@email.com",
                pageable,
                "MATUTINO",
                "2026",
                "IFSP",
                "Matematica"))
                .thenReturn(serviceResponse);

        ResponseEntity<Page<TurmaResponseDTO>> response = turmaController.listar(
                authentication,
                pageable,
                "MATUTINO",
                "2026",
                "IFSP",
                "Matematica");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
    }

    // Teste 4: endpoint GET /turmas/{id} busca turma por id.
    @Test
    void buscarPorIdDeveRetornarTurmaDoProfessorAutenticado() {
        TurmaResponseDTO serviceResponse = criarResponse();

        when(authentication.getName()).thenReturn("prof@email.com");
        when(turmaService.buscarPorId("prof@email.com", 10L)).thenReturn(serviceResponse);

        ResponseEntity<TurmaResponseDTO> response = turmaController.buscarPorId(authentication, 10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(turmaService).buscarPorId("prof@email.com", 10L);
    }

    // Teste 5: endpoint PUT /turmas/{id} atualiza turma.
    @Test
    void atualizarDeveRetornarTurmaAtualizada() {
        TurmaRequestDTO request = criarRequest();
        TurmaResponseDTO serviceResponse = criarResponse();
        serviceResponse.setNome("Turma Atualizada");

        when(authentication.getName()).thenReturn("prof@email.com");
        when(turmaService.atualizar("prof@email.com", 10L, request)).thenReturn(serviceResponse);

        ResponseEntity<TurmaResponseDTO> response = turmaController.atualizar(authentication, 10L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Turma Atualizada", response.getBody().getNome());
        verify(turmaService).atualizar("prof@email.com", 10L, request);
    }

    // Teste 6: endpoint DELETE /turmas/{id} remove turma e retorna 204.
    @Test
    void deletarDeveRetornarNoContent() {
        when(authentication.getName()).thenReturn("prof@email.com");

        ResponseEntity<Void> response = turmaController.deletar(authentication, 10L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(turmaService).deletar("prof@email.com", 10L);
    }

    private TurmaRequestDTO criarRequest() {
        return new TurmaRequestDTO(
                "Turma A",
                LocalDate.of(2026, 1, 1),
                4,
                20,
                Turno.MATUTINO,
                "Matematica",
                6.0,
                "IFSP");
    }

    private TurmaResponseDTO criarResponse() {
        return new TurmaResponseDTO(
                10L,
                "Turma A",
                LocalDate.of(2026, 1, 1),
                4,
                20,
                Turno.MATUTINO,
                "Matematica",
                6.0,
                List.of(),
                0,
                "IFSP");
    }
}
