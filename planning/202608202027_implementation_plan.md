# Plano de Otimização de Performance (FPS/Flicker) e Ajuste Fino de Hitboxes (Fases 2–5)

Este plano resolve diretamente os travamentos/flicker e o excesso de hitbox relatados a partir da Fase 2:

1. **Causa Raiz do Lag e Flicker (Cache de Imagens Nulas no `SpriteManager`)**:
   - Atualmente, imagens ausentes (como quadros de sequências de animação ou assets 3D opcionais) **não eram salvas no cache** quando retornavam `null`.
   - Isso fazia o Java realizar chamadas repetidas de I/O de disco (`File.exists()`) e busca no ClassLoader para dezenas de arquivos ausentes **a cada frame (60 vezes por segundo)** a partir da Fase 2.
   - **Solução**: Garantir que caminhos nulos sejam registrados no `imageCache`, eliminando 100% das chamadas de disco no loop de renderização e estabilizando a taxa de quadros em 60 FPS cravados.

2. **Refinamento Fino das Hitboxes (Mais Justas e Compactas)**:
   - **Herói**: Reduzir a caixa de dano central para o tronco/cabeça (`32px` de largura × `112px` de altura), garantindo extrema agilidade nas esquivas.
   - **Inimigos**: Ajustar a área de colisão para 50% das dimensões visuais centrais (ignorando nadadeiras, caudas e tentáculos).
   - **Corais (Fase 2)**: Aumentar o canal de passagem vertical para **360px** (dando ~248px de margem livre) e suavizar o contorno neon.

3. **Substituição do Flicker Visual por Efeito Fantasma Translúcido**:
   - Substituir o piscar intermitente (que sumia com o herói durante a invulnerabilidade) por um efeito de transparência suave (`globalAlpha = 0.45`), evitando a sensação de travamento visual/flicker.

4. **Rebalanceamento de Forças das Correntes (Fases 3 e 4)**:
   - Suavizar a aceleração das correntes de água e calor para evitar empurrões violentos contra as paredes de obstáculos.

---

## Proposed Changes

### Graphics & Resource Caching

#### [MODIFY] [SpriteManager.java](file:///home/rafael/github/cache/grafica/Game/src/main/java/br/apsu/graphics/SpriteManager.java)
- Atualizar `getImage(resourcePath)` para sempre executar `imageCache.put(resourcePath, img)` (mesmo quando `img == null`), prevenindo buscas repetidas no sistema de arquivos a cada frame.

#### [MODIFY] [RenderEngine.java](file:///home/rafael/github/cache/grafica/Game/src/main/java/br/apsu/graphics/RenderEngine.java)
- Substituir a ocultação intermitente do herói em `drawHero` por `gc.setGlobalAlpha(0.45)` durante a invulnerabilidade.
- Suavizar a borda dos corais na Fase 2 para alinhar a estética marinha aos novos limites.

---

### Core Mechanics & Collision Boundaries

#### [MODIFY] [GameContext.java](file:///home/rafael/github/cache/grafica/Game/src/main/java/br/apsu/core/GameContext.java)
- **Hitbox do Herói (`heroHits` e `pushHeroOutOfObstacle`)**:
  - Ajustar offset do núcleo: `x + 20, y + 40`, largura `32px`, altura `112px`.
- **Hitbox dos Inimigos (`heroHitsEnemy`)**:
  - Reduzir escala de colisão para `width * 0.50` e `height * 0.50` no centro.
- **Passagens da Fase 2 (`startP2`)**:
  - Definir `gapH = 360.0`.
- **Correntes da Fase 3 e 4**:
  - Reduzir vetores de empuxo das correntes para manter controle total do nado.

---

## Verification Plan

### Automated Tests
- Executar a suíte de testes unitários e de integração:
  ```bash
  JAVA_HOME=/opt/java-temurin-21 mvn test
  ```

### Performance & Visual Verification
- Validar se o carregamento de quadros zerou I/O desnecessário e se os testes de integração do `GameContext` e `SpriteManager` continuam passando sem regressões.
