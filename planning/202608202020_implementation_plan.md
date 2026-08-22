# Plano de Melhoria de Colisões, Hitboxes (Aura) e Jogabilidade (Fases 2–5)

Este plano aborda diretamente a piora na jogabilidade relatada a partir da Fase 2, focando em:
1. **Desalinhamento entre Aura Visual e Hitboxes Reais ("Limiares")**: Redução e calibração das auras bioluminescentes para que a luz emita dentro dos limites físicos reais, eliminando colisões fantasmas e "auras enganosas".
2. **Tratamento de Colisões e Separação Física (MTV / Push-Out)**: Correção do problema em que velocidade alta fazia o herói atravessar ou ficar preso dentro de rochas, corais e obstáculos, tomando dano contínuo sem conseguir reagir.
3. **Sub-stepping de Física e Passagens da Fase 2**: Aumento do espaço vertical de passagem entre corais na Fase 2 (de 260px para 330px) e sub-divisão dos passos de física para evitar repetições desfair de dano.

---

## User Review Required

> [!IMPORTANT]
> **Sub-stepping de Física**: A movimentação e checagem de colisões serão sub-divididas em até 2 sub-passos por frame quando a velocidade do herói for alta ou quando houver correntes ativas. Isso garante detecção precisa mesmo em alta velocidade.
> **Separação de Posição (Push-out)**: Colisões com corais, rochas vulcânicas e obstáculos móveis passarão a **repelir fisicamente a posição do herói** para fora do contorno da rocha, impedindo que o jogador fique "preso dentro do obstáculo" tomando dano repetido.

---

## Proposed Changes

### Core Engine & Game Context

#### [MODIFY] [GameContext.java](file:///home/rafael/github/cache/grafica/Game/src/main/java/br/apsu/core/GameContext.java)
- **Aumento do Gap entre Corais (Fase 2)**: Alterar `gapH` de `260` para `330` em `startP2()`. Com a altura do herói em 192px, a margem passa de 34px para ~69px em cada lado, permitindo manobras precisas.
- **Refinamento dos Limiares de Hitbox (`heroHits`)**:
  - Ajustar o retângulo do herói para ser focado no tronco/cabeça (`hero.getX() + 16, hero.getY() + 32`, largura 40px, altura 128px), ignorando cauda/nadadeiras periféricas e auras externas nas colisões de dano.
- **Separação Física de Posição (MTV - Minimum Translation Vector)**:
  - Implementar resposta de colisão com afastamento de posição (`pushOut`) para `CORAL`, `VOLCANIC_ROCK` e `MOVING_OBSTACLE`. Se o herói encostar em uma rocha/coral, a posição `(x, y)` dele é imediatamente deslocada para a borda externa mais próxima, além de receber o impulso e o invulnerability frame.
  - Na `LAVA_POOL` (Fase 4), adicionar um impulso vertical suave para cima (`vy = -4.0`), ejetando o herói da superfície de lava em vez de deixá-lo afundando em dano contínuo.
- **Sub-stepping no Update**:
  - No método `update()`, integrar as posições e checar colisões em 2 sub-passos caso a velocidade total exceda um limite de segurança (por exemplo, 8px/frame) ou em zonas de corrente.

---

### Graphics & Visual Feedback

#### [MODIFY] [RenderEngine.java](file:///home/rafael/github/cache/grafica/Game/src/main/java/br/apsu/graphics/RenderEngine.java)
- **Calibração das Auras Bioluminescentes**:
  - Reduzir o raio do halo em obstáculos móveis, corais e rochas vulcânicas (de `width * 0.7` / `0.5` para `width * 0.35`), tornando a iluminação envolvente e bonita sem expandir visualmente para as zonas de passagem segura.
  - Ajustar a iluminação do herói (`drawBioluminescentHalo`) para concentrar o brilho no corpo central em vez de criar um imenso círculo que mascara a posição real.

---

### Hero Physics & Knockback

#### [MODIFY] [HeroEntity.java](file:///home/rafael/github/cache/grafica/Game/src/main/java/br/apsu/model/hero/HeroEntity.java)
- **Amortecimento de Knockback**:
  - Garantir que o impulso de dano (`applyKnockback`) restaure a mobilidade do jogador de forma previsível após o impacto, sem travar os controles nem permitir repetição de hits na mesma rocha.

---

## Verification Plan

### Automated Tests
- Executar os testes automatizados da aplicação via Maven:
  ```bash
  MVN_CMD="mvn test"
  ```
  Verificar se todos os testes unitários (`GameContextIntegrationTest`, `HeroEntityTest`, `EnemyEntityTest`, etc.) passam com sucesso sem regressões.

### Manual / Structural Verification
- Compilar o projeto e validar se a compilação JavaFX e a execução de testes ocorrem sem erros:
  - `make build`
  - Validar separação física em cenários de teste de colisão.
