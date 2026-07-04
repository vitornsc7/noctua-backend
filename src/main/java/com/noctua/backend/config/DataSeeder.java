package com.noctua.backend.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.noctua.backend.entity.Aluno.AlunoEntity;
import com.noctua.backend.entity.Frequencia.FrequenciaEntity;
import com.noctua.backend.entity.Turma.TurmaEntity;
import com.noctua.backend.entity.Usuario.AdminEntity;
import com.noctua.backend.entity.Usuario.ProfessorEntity;
import com.noctua.backend.entity.Usuario.UsuarioEntity;
import com.noctua.backend.enums.Turno;
import com.noctua.backend.repository.turma.AlunoRepository;
import com.noctua.backend.repository.turma.FrequenciaRepository;
import com.noctua.backend.repository.turma.TurmaRepository;
import com.noctua.backend.repository.usuario.AdminRepository;
import com.noctua.backend.repository.usuario.ProfessorRepository;
import com.noctua.backend.repository.usuario.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final AdminRepository adminRepository;
    private final ProfessorRepository professorRepository;
    private final TurmaRepository turmaRepository;
    private final AlunoRepository alunoRepository;
    private final FrequenciaRepository frequenciaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedAdmin();
        ProfessorEntity professor = seedProfessor();
        if (professor != null) {
            TurmaEntity turma = seedTurma(professor);
            List<AlunoEntity> alunos = seedAlunos(turma);
            seedFrequencias(alunos);
        }
    }

    private void seedAdmin() {
        if (usuarioRepository.existsByEmail("admin@noctua.com"))
            return;

        UsuarioEntity usuario = UsuarioEntity.builder()
                .nome("Admin Noctua")
                .email("admin@noctua.com")
                .senhaHash(passwordEncoder.encode("12345678"))
                .ativo(true)
                .build();

        AdminEntity admin = AdminEntity.builder()
                .usuario(usuario)
                .build();

        adminRepository.save(admin);
    }

    private ProfessorEntity seedProfessor() {
        if (usuarioRepository.existsByEmail("prof@noctua.com")) {
            return professorRepository.findByUsuarioEmail("prof@noctua.com").orElse(null);
        }

        UsuarioEntity usuario = UsuarioEntity.builder()
                .nome("Professor Padrão")
                .email("prof@noctua.com")
                .senhaHash(passwordEncoder.encode("12345678"))
                .ativo(true)
                .build();

        ProfessorEntity professor = ProfessorEntity.builder()
                .usuario(usuario)
                .build();

        return professorRepository.save(professor);
    }

    private TurmaEntity seedTurma(ProfessorEntity professor) {
        if (turmaRepository.countByProfessorIdAndAtivoTrue(professor.getId()) > 0) {
            return turmaRepository
                    .findAll()
                    .stream()
                    .filter(t -> t.getProfessor().getId().equals(professor.getId()) && t.getAtivo())
                    .findFirst()
                    .orElse(null);
        }

        TurmaEntity turma = new TurmaEntity();
        turma.setNome("3º Ano A");
        turma.setAnoLetivo(LocalDate.of(2026, 1, 1));
        turma.setQtdePeriodos(4);
        turma.setQtdeAulasPrevistasPeriodo(20);
        turma.setTurno(Turno.MATUTINO);
        turma.setDisciplina("Matemática");
        turma.setInstituicao("Escola Noctua");
        turma.setMediaMinima(6.0);
        turma.setAtivo(true);
        turma.setProfessor(professor);

        return turmaRepository.save(turma);
    }

    private List<AlunoEntity> seedAlunos(TurmaEntity turma) {
        List<AlunoEntity> existentes = alunoRepository.findByTurmaId(turma.getId());
        if (!existentes.isEmpty())
            return existentes;

        List<String> nomes = List.of(
                "Ana Beatriz Silva",
                "Bruno Costa Santos",
                "Carlos Eduardo Lima",
                "Daniela Ferreira Souza",
                "Eduardo Henrique Alves",
                "Fernanda Gomes Rocha",
                "Gabriel Martins Oliveira",
                "Heloísa Ribeiro Costa",
                "Igor Nascimento Pinto",
                "Juliana Carvalho Mendes",
                "Lucas Pereira Dias",
                "Mariana Oliveira Ramos"
        );

        List<AlunoEntity> alunos = nomes.stream().map(nome -> {
            AlunoEntity aluno = new AlunoEntity();
            aluno.setNome(nome);
            aluno.setAtivo(true);
            aluno.setTurma(turma);
            return aluno;
        }).toList();

        return alunoRepository.saveAll(alunos);
    }

    private void seedFrequencias(List<AlunoEntity> alunos) {
        if (!frequenciaRepository.findByAlunoIdAndAtivoTrue(alunos.get(0).getId()).isEmpty())
            return;

        salvarFalta(alunos.get(0),  LocalDateTime.of(2026,  3,  5, 8, 0), 1, 2);
        salvarFalta(alunos.get(0),  LocalDateTime.of(2026,  3, 19, 8, 0), 1, 1);
        salvarFalta(alunos.get(2),  LocalDateTime.of(2026,  3, 12, 8, 0), 1, 2);
        salvarFalta(alunos.get(4),  LocalDateTime.of(2026,  2, 26, 8, 0), 1, 1);
        salvarFalta(alunos.get(6),  LocalDateTime.of(2026,  3, 19, 8, 0), 1, 3);
        salvarFalta(alunos.get(8),  LocalDateTime.of(2026,  2, 19, 8, 0), 1, 1);
        salvarFalta(alunos.get(9),  LocalDateTime.of(2026,  3, 26, 8, 0), 1, 2);
        salvarFalta(alunos.get(10), LocalDateTime.of(2026,  3,  5, 8, 0), 1, 1);

        salvarFalta(alunos.get(1),  LocalDateTime.of(2026,  5,  7, 8, 0), 2, 1);
        salvarFalta(alunos.get(1),  LocalDateTime.of(2026,  6,  4, 8, 0), 2, 2);
        salvarFalta(alunos.get(3),  LocalDateTime.of(2026,  5, 21, 8, 0), 2, 1);
        salvarFalta(alunos.get(5),  LocalDateTime.of(2026,  5, 14, 8, 0), 2, 2);
        salvarFalta(alunos.get(7),  LocalDateTime.of(2026,  6, 11, 8, 0), 2, 1);
        salvarFalta(alunos.get(8),  LocalDateTime.of(2026,  5, 28, 8, 0), 2, 3);
        salvarFalta(alunos.get(9),  LocalDateTime.of(2026,  6, 18, 8, 0), 2, 1);
        salvarFalta(alunos.get(11), LocalDateTime.of(2026,  6, 25, 8, 0), 2, 2);
    }

    private void salvarFalta(AlunoEntity aluno, LocalDateTime dataFalta, int periodo, int periodosFaltados) {
        FrequenciaEntity falta = new FrequenciaEntity();
        falta.setAluno(aluno);
        falta.setDataFalta(dataFalta);
        falta.setPeriodo(periodo);
        falta.setPeriodosFaltados(periodosFaltados);
        falta.setAtivo(true);
        frequenciaRepository.save(falta);
    }
}