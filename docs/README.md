# Indice da Documentacao Interna - As Aguas de Apsu

Este diretorio centraliza a documentacao tecnica, guias de engenharia, especificacoes de arquitetura, artefatos de pesquisa academica e registros de evolucao do projeto **As Aguas de Apsu**.

---

## Estrutura da Documentacao

| Documento / Diretorio | Descricao |
|---|---|
| [ARQUITETURA_E_DESENVOLVIMENTO.md](ARQUITETURA_E_DESENVOLVIMENTO.md) | Documento central de engenharia e producao: estados, runtime, controles e checklist completo. |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Especificacao formal dos subsistemas de runtime, fisica de fluidos, loop de render e zero-alocacao. |
| [PIPELINE_3D.md](PIPELINE_3D.md) | Guia do pipeline de exportacao 3D do Blender para o runtime JavaFX (malhas OBJ, materiais MTL, texturas). |
| [DEVELOPMENT.md](DEVELOPMENT.md) | Guia de preparacao de ambiente local, comandos do Makefile, build com Maven e execucao em Linux e Windows. |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Diretrizes de colaboracao, convencoes de commits, fluxo de PRs e matriz de papeis da equipe. |
| [perf-sprint-report.md](perf-sprint-report.md) | Relatorio detalhado de auditoria de performance, frame-timing e otimizacoes de Garbage Collector. |
| [[Grafica] Game - Aguas de Apsu.zip]("[Gráfica] Game - Águas de Apsu.zip") | Pacote completo do relatorio tecnico academico em LaTeX (UNIFAL), figuras, fontes e dados de sprints. |

---

## Subdiretorios Especializados

- **`diagrams/`**: Diagramas de arquitetura nos formatos DOT, PNG e SVG, alem do relatorio academico compilado em PDF (`[Gráfica] Game - Águas de Apsu.pdf`).
- **`product/`**: Gestao de produto, definicao de pronto (DoD), backlog e roadmaps de sprints (`BACKLOG.md`, `DEFINITION_OF_DONE.md`, `SPRINTS.md`).
- **`development/`**: Planilhas e acompanhamento detalhado de esforco (`apsu_sprints_backlog.xlsx`).
- **`qa/`**: Relatorios de controle de qualidade, investigacoes de renderizacao de hardware (Mesa/OpenGL) e testes de stress.
- **`evolucao/`**: Registros historicos de capturas de tela e evolucao visual da jogabilidade desde as fases iniciais ate as versoes mais recentes (`docs/evolucao/README.md` e `gameplay/`).
- **`audio/`**: Registro de fontes, licencas CC0 e creditos de composicao sonora (`SOURCES.md`).
- **`legacy/`**: Arquivos e artefatos de prototipos anteriores preservados para referencia historica.

---

## Responsabilidades da Equipe

- **Erik Alexandre**: Trilha Sonora e Composicao Musical
- **Wagner**: Modelos 3D dos Personagens
- **Marcos**: Modelos 3D e Arte dos Cenarios
- **Rafael**: Mecanica, Fisica e Engine em Geral
