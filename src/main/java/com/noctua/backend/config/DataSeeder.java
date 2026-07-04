package com.noctua.backend.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.noctua.backend.entity.Aluno.AlunoEntity;
import com.noctua.backend.entity.Avaliacao.AvaliacaoEntity;
import com.noctua.backend.entity.Frequencia.FrequenciaEntity;
import com.noctua.backend.entity.Nota.NotaEntity;
import com.noctua.backend.entity.Turma.TurmaEntity;
import com.noctua.backend.entity.Usuario.AdminEntity;
import com.noctua.backend.entity.Usuario.ProfessorEntity;
import com.noctua.backend.entity.Usuario.UsuarioEntity;
import com.noctua.backend.enums.TipoAvaliacao;
import com.noctua.backend.enums.Turno;
import com.noctua.backend.repository.turma.AlunoRepository;
import com.noctua.backend.repository.turma.AvaliacaoRepository;
import com.noctua.backend.repository.turma.FrequenciaRepository;
import com.noctua.backend.repository.turma.NotaRepository;
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
    private final AvaliacaoRepository avaliacaoRepository;
    private final NotaRepository notaRepository;
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
            seedAvaliacao(turma, alunos);
        }
    }

    private void seedAdmin() {
        if (usuarioRepository.existsByEmail("administrador@noctua.com"))
            return;

        UsuarioEntity usuario = UsuarioEntity.builder()
                .nome("Admin Noctua")
                .email("administrador@noctua.com")
                .senhaHash(passwordEncoder.encode("12345678"))
                .ativo(true)
                .build();

        AdminEntity admin = AdminEntity.builder()
                .usuario(usuario)
                .build();

        adminRepository.save(admin);
    }

    private ProfessorEntity seedProfessor() {
        if (usuarioRepository.existsByEmail("professor@noctua.com")) {
            return professorRepository.findByUsuarioEmail("professor@noctua.com").orElse(null);
        }

        UsuarioEntity usuario = UsuarioEntity.builder()
                .nome("Professor Noctua")
                .email("professor@noctua.com")
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
        turma.setNome("3º Ano - A");
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

    private void seedAvaliacao(TurmaEntity turma, List<AlunoEntity> alunos) {
        if (!avaliacaoRepository.findByTurmaId(turma.getId()).isEmpty())
            return;

        criarAvaliacaoComNotas(turma, alunos,
                "Álgebra e Funções",
                LocalDateTime.of(2026, 3, 18, 8, 0),
                TipoAvaliacao.PROVA, 1, 3, 1,
                new Double[]{ 8.5, 7.0, 5.5, 9.0, 6.5, null, 4.0, 8.0, 7.5, 6.0, 9.5, 5.0 });

        criarAvaliacaoComNotas(turma, alunos,
                "Geometria Plana",
                LocalDateTime.of(2026, 4, 2, 8, 0),
                TipoAvaliacao.TRABALHO, 1, 2, 2,
                new Double[]{ 9.0, 8.0, 6.0, 7.5, 7.0, 9.5, 5.5, 8.5, null, 7.0, 10.0, 6.5 });

        criarAvaliacaoComNotas(turma, alunos,
                "Resolução de Problemas",
                LocalDateTime.of(2026, 4, 16, 8, 0),
                TipoAvaliacao.ATIVIDADE, 1, 1, 3,
                new Double[]{ 7.0, 9.0, 7.5, 8.0, 5.0, 10.0, 6.0, 7.0, 9.0, 8.5, 8.0, 7.5 });

        criarAvaliacaoComNotas(turma, alunos,
                "Trigonometria",
                LocalDateTime.of(2026, 5, 20, 8, 0),
                TipoAvaliacao.PROVA, 2, 3, 4,
                new Double[]{ 7.5, 6.0, 4.5, 9.5, 8.0, 9.0, 3.5, 7.0, 6.5, 5.5, 10.0, 4.0 });

        criarAvaliacaoComNotas(turma, alunos,
                "Estatística Descritiva",
                LocalDateTime.of(2026, 6, 3, 8, 0),
                TipoAvaliacao.TRABALHO, 2, 2, 5,
                new Double[]{ 8.0, 7.5, 6.5, 9.0, 7.0, 10.0, 5.0, 8.5, 7.0, 6.5, 9.5, 5.5 });

        criarAvaliacaoComNotas(turma, alunos,
                "Análise de Gráficos",
                LocalDateTime.of(2026, 6, 17, 8, 0),
                TipoAvaliacao.ATIVIDADE, 2, 1, 6,
                new Double[]{ 9.0, 8.5, 7.0, 8.0, 6.0, 10.0, 6.5, 9.0, 8.0, 7.5, 9.0, 6.0 });
    }

    private void criarAvaliacaoComNotas(TurmaEntity turma, List<AlunoEntity> alunos,
            String tema, LocalDateTime data, TipoAvaliacao tipo,
            int periodo, int peso, int numeroChamada, Double[] valores) {

        AvaliacaoEntity avaliacao = new AvaliacaoEntity();
        avaliacao.setTema(tema);
        avaliacao.setData(data);
        avaliacao.setPeso(peso);
        avaliacao.setTipo(tipo);
        avaliacao.setPeriodo(periodo);
        avaliacao.setNumeroChamada(numeroChamada);
        avaliacao.setConcluida(true);
        avaliacao.setTurma(turma);

        AvaliacaoEntity salva = avaliacaoRepository.save(avaliacao);

        for (int i = 0; i < alunos.size(); i++) {
            salvarNota(salva, alunos.get(i), valores[i] != null ? BigDecimal.valueOf(valores[i]) : null);
        }
    }

    private void salvarNota(AvaliacaoEntity avaliacao, AlunoEntity aluno, BigDecimal valor) {
        NotaEntity nota = new NotaEntity();
        nota.setAvaliacao(avaliacao);
        nota.setAluno(aluno);
        nota.setValor(valor);
        nota.setNaoRealizada(valor == null);
        notaRepository.save(nota);
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