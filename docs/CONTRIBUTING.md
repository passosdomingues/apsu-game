# Guia de Contribuicao - As Aguas de Apsu (apsu-game)

Agradecemos o interesse em contribuir para o **As Aguas de Apsu: A Lenda dos Apkallu**! Este guia define as diretrizes de desenvolvimento, convencoes de codigo e responsabilidades da equipe.

---

## Matriz de Responsabilidades da Equipe

Para evitar conflitos e organizar a colaboracao, as areas do projeto estao distribuidas entre os seguintes papeis:

| Membro | Responsabilidade Principal | Modulos e Pastas Relacionadas |
|---|---|---|
| **Erik Alexandre** | Trilha sonora e composicao musical | `docs/audio/`, `src/main/resources/audio/`, audio adaptativo |
| **Wagner** | Modelos 3D dos personagens | `personagens/blends/`, malhas OBJ/MTL de herois e inimigos |
| **Marcos** | Modelos 3D e arte dos cenarios | Cenarios 3D, panoramas de fase, props de cena, fundos paralaxe |
| **Rafael** | Mecanica, fisica e engine em geral | `src/main/java/br/apsu/`, loop 60 FPS, zero-alloc, colisao, plataforma |

---

## Workflow Git e Nomenclatura de Branches

Toda contribuicao deve ser feita via Pull Request a partir de uma branch dedicada seguindo o padrao:

- `feature/<descricao-curta>` - Novas mecanicas, entidades ou funcionalidades.
- `fix/<descricao-curta>` - Correcao de bugs ou falhas de fisica/renderizacao.
- `perf/<descricao-curta>` - Otimizacoes de tempo por quadro (frame-time) ou VRAM.
- `refactor/<descricao-curta>` - Melhorias estruturais sem alteracao de comportamento.
- `docs/<descricao-curta>` - Atualizacoes de documentacao e diagramas.
- `test/<descricao-curta>` - Adicao ou ajuste de testes automatizados.

---

## Convencao de Commits (Conventional Commits)

Os commits devem seguir o padrao: `<tipo>: <mensagem curta no imperativo>`

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
3. **Sem Artefatos de Build:** Certifique-se de que nenhum arquivo `target/`, `.jar` ou log foi incluido no commit.
4. **Revisao por Codeowners:** O PR deve ser aprovado pelo responsavel pelo modulo afetado (ver `CODEOWNERS`).
