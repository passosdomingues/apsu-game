# Sprint 18 — Teclado Virtual Translúcido Glassmorphic, Tutorial Animado de Controles e Projéteis de Lava do Boss Kullullû (APSU-S18)

**Período:** Pós-Sprint 17 (Culling de Viewport & Correção de Herói do Menu)  
**Meta:** Prover feedback visual de entrada de alto padrão (UX/UI imersiva), tutorial interativo animado dos Apkallu no Menu e na jogabilidade, e equipar o Boss Kullullû (Fase 5) com as malhas 3D e renderização 2D de bolas de lava incandescentes.

---

## 1. Contexto e Motivação

1. **UX/UI e Curva de Aprendizado:** Jogadores novatos e avaliadores acadêmicos precisam de clareza imediata sobre todos os controles disponíveis (WASD, Setas, Q para sonar, E para interação, ESPAÇO para ataque, M para alternância tática de disparo único/triplo, R para reiniciar e ESC para pausa/menu).
2. **Teclado Translúcido Interativo em Tempo Real:** Uma representação glassmorphic na tela com afundamento físico de teclas e feedback de iluminação instantâneo ao pressionar qualquer tecla.
3. **Tutorial Animado no Menu Principal:** Card de demonstração visual onde Adapa nada, emite anéis de ecolocalização, interage com relíquias/guardiões, dispara bolhas purificadoras e demonstra a alternância tática entre disparo único frontal e leque triplo via [M], com as teclas acendendo sincronizadas.
4. **Projéteis Vulcânicos do Boss Kullullû (Fase 5):** Os projéteis do chefe (`BOSS_BULLET`) utilizavam círculos vermelhos planos em 2D e não requisitavam a malha 3D `scenery/bola_de_lava_3d` na Fase 5. Agora utilizam a mesma malha 3D rotativa e incandescente com proporção colossal (escala 54) e núcleo ardente em 2D.

---

## 2. Entregas e Histórias de Usuário

### APSU-B18A | Projéteis Vulcânicos 3D e 2D de Kullullû (`BOSS_BULLET`)
- **`Runtime3DLayer.java`**:
  - Requisita o modelo `scenery/bola_de_lava_3d` na Fase 5 (`context.getCurrentPhase() == 4 || context.getCurrentPhase() == 5`).
  - Mapeia `BOSS_BULLET` para a malha 3D `bola_de_lava_3d` com tamanho 54, rotação contínua nos eixos e tint da paleta `accent()`.
  - Culling de viewport mantido para evitar processamento fora da arena.
- **`RenderEngine.java`**:
  - Renderização 2D de `BOSS_BULLET` aprimorada com aura de magma (`LAVA_AURA`), manto vulcânico (`LAVA_OUTER`), núcleo incandescente (`LAVA_CORE`), ponto quente central (`LAVA_HOTSPOT`) e rastro de cinzas/brasas (`LAVA_TRAIL`).

### APSU-U18B | Teclado Virtual Translúcido Glassmorphic (`VirtualKeyboardOverlay.java`)
- **Componente dedicado:** `src/main/java/br/apsu/graphics/VirtualKeyboardOverlay.java`.
- **Layout de teclas completas:**
  - Movimento: W, A, S, D e Setas direcionais (↑, ←, ↓, →).
  - Ações especiais: Q (Sonar / Ecolocalização), E (Interagir / Guardião / Baú), ESPAÇO (Atacar / Purificar), M (Alternar Tiro Único / Triplo), R (Reiniciar) e ESC (Pausa / Menu).
- **Feedback Físico e Luminoso:** Afundamento do keycap de 2px, iluminação ciano brilhante (`Palette.rgb(0, 215, 255)`) e halo radiante ao pressionar qualquer tecla detectada via `InputManager`.
- **Zero-Alloc:** Instâncias e coordenadas calculadas uma única vez no construtor; nenhum objeto instanciado durante o loop de renderização a 60 FPS.

### APSU-U18C | Tutorial Animado dos Apkallu no Menu Principal
- **Showcase Interativo no Menu:** Painel glassmorphic elegante no lado esquerdo da tela (345x490 px) harmonizando com o card de herói na direita.
- **Ciclo contínuo de 5 passos com Adapa animado:**
  1. *Navegação & Mergulho:* Adapa ondulando e superando correntes com [W, A, S, D / Setas] acendendo ritmicamente.
  2. *Ecolocalização Mística:* Emissão de anéis dourados de sonar revelando relíquias com [Q] piscando.
  3. *Interação Sagrada:* Conexão dourada com Guardiões Enki e Baú com [E] em destaque.
  4. *Combate de Purificação:* Disparo de bolhas luminosas com [ESPAÇO] afundando.
  5. *Modo Tático de Disparo:* Alternância entre disparo único concentrado e rajada tripla em leque com [M] acendendo.
- **Teclado Virtual Interativo no Menu:** Permite ao jogador pressionar as teclas físicas no menu e ver a resposta imediata na tela antes de iniciar a partida.

### APSU-U18D | Destaque Guiado de Tutorial e Alertas Táticos na Jogabilidade
- **Overlay de Gameplay HUD:** Docked com sutileza no canto inferior direito (`width - 282, height - 98`), sempre legível e sem obstruir o combate.
- **Destaque Pulsante Contextual (P1 e P2):**
  - Fase 1 (Passo 1): Teclas [S] e [↓] pulsam com brilho dourado.
  - Fase 1 (Passo 2): Teclas [A], [D], [←], [→] pulsam para guiar desvio de inimigos.
  - Fase 1 (Passo 3): Tecla [Q] pulsa orientando emissão do sonar.
  - Fase 1 (Passo 4): Teclas [E] e [ESPAÇO] pulsam próximo ao Guardião Enki.
  - Fase 2 (Baú Descoberto): Tecla [E] e [ESPAÇO] pulsam próximo ao baú.
  - Fase 2 (Poder Desperto): Teclas [ESPAÇO] e [M] destacadas, com alert indicativo da alternância de modo.
- **Alerta de Alternância via [M]:** Exibe `"Modo de Ataque: Rajada Tripla [M]"` ou `"Modo de Ataque: Bolha Única [M]"` instantaneamente ao pressionar [M].

---

## 3. Validação e Qualidade Técnica

1. **Testes Unitários:**
   - Criado `VirtualKeyboardOverlayTest` (instanciação, renderização sem exceções, validação de mapeamento de teclas).
   - Atualizado `Runtime3DLayerTest` (mapeamento de `boss-bullet-0` para a paleta `accent()`).
   - Todos os **215/215 testes unitários e de integração verdes** (`BUILD SUCCESS`).
2. **Inspeção Visual:**
   - 11 capturas de tela regeneradas em `.claude/capturas/inspecao/` cobrindo o novo menu principal com tutorial animado e o overlay de teclado em todas as fases.
   - Análise de pixels confirma posicionamento e ausência de regressões gráficas.
3. **Zero-Alloc Loop:**
   - Sem alocações no loop quente de 60 FPS; todas as fontes e cores reutilizam `Palette` e `UiFonts`.
