# Guia de Contribuição — As Águas de Apsu (`apsu-game`)

Agradecemos o interesse em contribuir para o **As Águas de Apsu: A Lenda dos Apkallu**! Este guia define as diretrizes de desenvolvimento, convenções de código e responsabilidades da equipe.

---

## Matriz de Responsabilidades da Equipe (5 Desenvolvedores)

Para evitar conflitos de merge e organizar a colaboração, as áreas do projeto estão divididas entre os seguintes papéis:

| Papel | Responsabilidade Principal | Módulos e Pastas Relacionadas |
|---|---|---|
| **DEV 1 — Core Engine** | `GameContext`, `GameLoop`, gestão de estado central, ciclo de vida de entidades | `src/main/java/br/apsu/core/` |
| **DEV 2 — Gráficos** | `RenderEngine`, `Camera`, `SpriteManager`, `LightingEngine`, `UIRenderer`, performance 60 FPS | `src/main/java/br/apsu/graphics/` |
| **DEV 3 — Gameplay** | `HeroEntity`, `EnemyEntity`, `BossEntity`, `GuardianEntity`, físicas, colisões, IA | `src/main/java/br/apsu/model/` |
| **DEV 4 — Pipeline de Assets** | Blender/bpy, modelos 3D procedurais, animações, baking 2.5D, spritesheets PNG | `personagens/`, `tools/assets/` |
| **DEV 5 — Plataforma & Sistemas** | OpenMPI, gerador de mapas em C, Makefile, `pom.xml`, CI/CD, scripts de setup | `mpi/`, `tools/setup/`, `.github/`, `Makefile` |

---

## Workflow Git e Nomenclatura de Branches

Toda contribuição deve ser feita via Pull Request a partir de uma branch dedicada seguindo o padrão:

- `feature/<descricao-curta>` — Novas mecânicas, entidades ou funcionalidades.
- `fix/<descricao-curta>` — Correção de bugs ou falhas de física/renderização.
- `perf/<descricao-curta>` — Otimizações de tempo por quadro (frame-time) ou VRAM.
- `refactor/<descricao-curta>` — Melhorias estruturais sem alteração de comportamento.
- `docs/<descricao-curta>` — Atualizações de documentação e diagramas.
- `test/<descricao-curta>` — Adição ou ajuste de testes automatizados.

---

## Convenção de Commits (Conventional Commits)

Os commits devem seguir o padrão: `<tipo>: <mensagem curta no imperativo>`

Exemplos:
- `feat: adiciona ataque especial da variante Abissal`
- `fix: corrige offset da camera ao redimensionar viewport`
- `perf: otimiza spatial index QuadTree para 100+ projeteis`
- `docs: atualiza diagrama DOT com a nova classe de evento`
- `build: adiciona perfil de SO para Windows no pom.xml`

---

## Processo de Pull Request

1. **Garanta que os testes passem:** Execute `make test` ou `mvn test` localmente antes de abrir o PR.
2. **Preencha o Template de PR:** Responda todos os itens do template em `.github/pull_request_template.md`.
3. **Sem Artefatos de Build:** Certifique-se de que nenhum arquivo `target/`, `.jar` ou log foi incluído no commit.
4. **Revisão por Codeowners:** O PR deve ser aprovado por pelo menos um revisor responsável pelo módulo afetado (ver `CODEOWNERS`).
