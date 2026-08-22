# AGENTS.md — Diretrizes para Desenvolvedores Humanos e Agentes de IA

Este documento contém as regras fundamentais de conduta e desenvolvimento aplicáveis a qualquer desenvolvedor humano ou agente de IA que contribua para o repositório **As Águas de Apsu** (`apsu-game`).

## Regras Críticas de Desenvolvimento (13 Regras Invioláveis)

1. **Escopo Restrito:** Nunca modificar arquivos não relacionados à tarefa em andamento.
2. **Auditoria Prévia:** Inspecionar a estrutura e o código existente antes de realizar qualquer edição.
3. **Preservação Arquitetural:** Preservar a arquitetura existente a menos que um refactoring seja explicitamente solicitado pelo usuário.
4. **Validação Obrigatória:** Rodar a suíte de testes relevante (`mvn test` / `make test`) antes de declarar conclusão de qualquer tarefa.
5. **Formatação Limpa:** Evitar mudanças de formatação ou re-estilizações em linhas não relacionadas às alterações da tarefa.
6. **Commits Atômicos:** Manter commits atômicos, coesos e devidamente documentados conforme o padrão Conventional Commits.
7. **Zero Artefatos de Build:** Nunca commitar artefatos gerados por compilação ou execução (`target/`, `build/`, `out/`, `*.class`, `*.jar`, logs, etc.).
8. **Preservação de Assets:** Nunca excluir assets (modelos 3D, sprites, áudios, mapas) sem justificativa técnica explícita.
9. **Sincronização de Documentação:** Atualizar a documentação (`README.md`, `ARCHITECTURE.md`, `DEVELOPMENT.md`) sempre que interfaces, utilitários ou workflows forem alterados.
10. **Modularidade sobre Monolito:** Preferir mudanças modulares e desacopladas em vez de realizar edições diretas em "God objects" centrais (`GameContext.java`, `RenderEngine.java`).
11. **Performance em Runtime:** Avaliar o impacto de alocação de memória (GC pressure) e tempo por quadro (frame-time) em qualquer código executado no loop principal de 60 FPS (`GameLoop.java`).
12. **Execução de MPI Desacoplada:** Nunca adicionar chamadas MPI síncronas ao loop de frame em tempo real, a menos que a tarefa peça explicitamente execução distribuída e os custos de sincronização estejam justificados.
13. **Resolução de Conflitos de Instrução:** Se qualquer instrução deste documento entrar em conflito com uma instrução direta do usuário na tarefa em andamento, parar e solicitar esclarecimento em vez de tomar decisões unilaterais.
