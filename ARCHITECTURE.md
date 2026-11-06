# Documentação de Arquitetura — As Águas de Apsu (`apsu-game`)

Este documento detalha os princípios de design, componentes de software e subsistemas de tempo de execução do jogo **As Águas de Apsu: A Lenda dos Apkallu**.

---

## Arquitetura Geral do Sistema

```text
+---------------------------------------------------------------------------------+
|                        ARQUITETURA DE TEMPO DE EXECUÇÃO                         |
|                                                                                 |
|   +------------------+   +------------------+   +---------------------------+   |
|   |   InputManager   |   |   GameContext    |   |       RenderEngine        |   |
|   | (Teclas/Controle)|-->|(Mundo, Física,   |-->| (JavaFX Canvas Immediate) |   |
|   |                  |   | Entidades, Col)  |   | (Fresnel Rim Light + SSS) |   |
|   +------------------+   +------------------+   +---------------------------+   |
|                                    |                          |                 |
|                                    v                          v                 |
|                          +------------------+   +---------------------------+   |
|                          |  AnimationTimer  |   |   JavaFX 3D + SpriteManager |   |
|                          |  (60 FPS Loop)   |   | (personagens OBJ + cenário) |   |
|                          +------------------+   +---------------------------+   |
+---------------------------------------------------------------------------------+
```

---

## 1. Ciclo de Vida e Bootstrapping (`br.apsu.Launcher` / `ApsuGameMain`)

- `Launcher.java`: Wrapper de entrada do app JAR com dependências; na release, o launcher mantém o ZIP de recursos de jogo no classpath junto ao JAR executável.
- `ApsuGameMain.java`: Ponto de entrada JavaFX `Application`. Configura o Canvas responsivo (1366x768), inicializa o `EventBus`, conecta o `AudioEventSubscriber` e instancia o `GameContext`, o `RenderEngine` e o `GameLoop`.

---

## 2. Motor de Tempo Real (`br.apsu.core.GameLoop`)

- Utiliza o `AnimationTimer` do JavaFX para alcançar 60 FPS cravados.
- Em cada quadro, calcula o delta de tempo monotônico via `FrameMetrics`.
- Orquestra duas fases estritas:
  1. `context.update(delta)` — Atualização de entrada, físicas, colisões e IA.
  2. `renderer.render(gc, context)` — Desenho imperativo no Canvas.

---

## 3. Física de Mecânica dos Fluidos e Colisões (`HeroEntity` / `QuadTree`)

- **Arrasto Quadrático da Água:** 
  $$\vec{F}_{\text{drag}} = -\frac{1}{2} C_d \cdot \rho \cdot A \cdot |\vec{v}| \cdot \vec{v}$$
- **Empuxo de Arquimedes:** Força vertical oscilante amortecida.
- **Indexação Espacial QuadTree:** Divisão hierárquica do espaço de mundo em quadrantes para reduzir a complexidade de colisão AABB de $O(N^2)$ para $O(N \log N)$.

---

## 4. Pipeline gráfico 3D (`RenderEngine` / `Runtime3DLayer`)

- **Renderização Imperativa Canvas:** Renderiza via `GraphicsContext` JavaFX, eliminando o overhead de retenimento de nós do Scene Graph.
- **Transformação de Coordenadas:** A `Camera` converte Coordenadas de Mundo em Coordenadas de Tela (`screenX = worldX - camX`).
- **Malhas 3D em tempo real:** `Runtime3DLayer` carrega personagens e elementos de fase (herói, NPCs, inimigos, naufrágio, baú, recifes/cardumes, ruínas, correntes e obstáculos) como OBJ/MTL exportados do Blender, numa `SubScene` transparente sincronizada com a câmera, em todos os perfis.
- **Materiais OBJ/MTL:** cada `map_Kd` vira mapa diffuse do material JavaFX. `Ke` não pode reutilizar o diffuse como mapa emissivo: isso autoilumina a textura inteira e apaga o contraste de luz. O runtime mantém o diffuse sujeito à iluminação da cena; modelos que precisarem de brilho próprio devem exportar um mapa de emissão separado.
- **Eixos OBJ:** X e Z do Blender viram horizontal e vertical no jogo; Y vira profundidade e é centralizado junto com os outros eixos para evitar parallax involuntário.
- **Canvas:** fundos renderizados das cenas Blender, HUD, indicadores, partículas e sinais de gameplay; personagens e props não usam quadros rasterizados no caminho normal de runtime.
- **Cenários e transição:** panoramas são renders das cenas 3D de fase. A descida é marcada por ruínas/colunas atlantes, não por um portal fantástico.
- **Nado e ataque:** Adapa usa a malha vertical 3D e a pose thrust 3D com braço/tridente articulados. Não desenhar contornos retangulares intermitentes sobre a silhueta durante dano.
- **Shaders de Iluminação:** O `LightingEngine` aplica iluminação emissiva de contorno (Fresnel Rim Light) e dispersão sob a superfície (SSS).

---

## 5. Barramento de Eventos e Áudio (`EventBus` / `AudioManager`)

- O `EventBus` implementa o padrão Publish/Subscribe desacoplado.
- `GameContext` publica pedidos com identificadores semânticos (`AudioCue`) em transições de mecânica: disparo, dano, coleta, diálogo e combate.
- `AudioEventSubscriber` conecta `GameEvent` a `AudioPort`; `AudioManager` aplica política de profundidade e estado do boss. Nenhum dos dois precisa conhecer arquivo, caminho ou formato de áudio.
- `JavaFxAudioOutput` usa um `AudioClip` PCM compartilhado por cue e ajusta o ganho por profundidade; suprime sobreposição do mesmo cue para evitar criar players concorrentes. As faixas de fase/boss continuam `MediaPlayer` em loop e usam WAV PCM 16-bit/44,1 kHz. A música inicia após o player ficar pronto, usa volume-base de 55% ajustado pela profundidade e tenta a faixa original se a versão gerada faltar/falhar. Substituir composição/arquivo exige atualizar o adaptador ou os recursos, sem alterar produtores de gameplay.
- Os eventos de profundidade, modo de música do boss e fase do boss são consumidos pelo áudio. Um tipo declarado em `GameEvent.Type` sem publicação e consumidor não representa uma integração concluída.
- O pipeline compõe um pad ambiente CC0 e rumble submarino CC0 sobre a trilha existente; a proporção pad/rumble muda em cada perfil. Ao entrar na arena final, um evento troca para a composição CC0 de suspense do boss, também moldada pela profundidade. Efeitos curtos usam os WAVs-fonte compartilhados; não são duplicados por perfil na reprodução.
- O jogo não executa filtros nem chama FFmpeg no loop. `tools/audio/render_audio_assets.py` cria renders offline, normalizados e substituídos atomicamente após sucesso.
- FFmpeg aplica filtro passa-baixa crescente e reflexões mais tardias conforme a profundidade. A trilha troca para o render da nova fase; execução normal continua usando os assets empacotados e não requer FFmpeg.

---

## 6. Otimizações de Performance e Zero-Alocação (Sprints R01-R05)

- **Tipografia e Cores Imutáveis (`UiFonts` / `Palette`):**
  - Centralização de 24 instâncias estáticas imutáveis de `Font` em `UiFonts.java`.
  - Pré-alocação dos gradientes de fundo e strokes em `Palette.java`.
  - Substituição de `ConcurrentHashMap` por `HashMap` leve dedicado ao thread JavaFX, eliminando ~1.800 alocações/s no caminho quente de `RenderEngine` e `UIRenderer`.
- **Sistema de Partículas com Pool Estático (`ParticleSystem`):**
  - Array estático de tamanho fixo `Particle[320]` com compactação in-place (swap-and-pop) sem alocação de coleções nem chamadas a `remove()`.
  - Paletas alfa estáticas pré-aquecidas, garantindo zero alocação de memória no heap durante rajadas e combate intenso.
- **Cache de Atores e Materiais 3D (`Runtime3DLayer`):**
  - Atualização in-place de `PhongMaterial.setDiffuseColor` via `ObjModelLoader.multiply` quando apenas o tint da fase é alterado, evitando destruição e reinserção de nós do Scene Graph (`Group`, `MeshView`) e eliminando o congelamento de transição de fase.
  - Guards anti-spam para `requestModel()` na Fase 2 e no Menu Principal.
- **Particionamento Espacial Reciclável (`QuadTree`):**
  - Retenção estrutural de subárvores pré-alocadas (`nodes[0..3]`) através de ciclos sucessivos de `clear()` e `split()` via flag `hasSplit`.
  - Eliminação de loops lineares duplicados de detecção de cenário no `GameContext`.
- **Orquestração e Flags JVM:**
  - Configuração padrão de G1 Garbage Collector com limite de pausa de 5 ms (`-XX:+UseG1GC -XX:MaxGCPauseMillis=5`).
  - Pipeline gráfico OpenGL ES2 com VSync habilitado (`-Dprism.order=es2,sw -Dprism.vsync=true`).

---

## 7. Eliminação de Tremor e Loop Fluido de Render (Sprint 7: F01, F02, F04)

- **Anti-Jitter de Entrada no X11 (`InputManager`):**
  - Janela de tolerância de 25 ms (`RELEASE_GRACE_NANOS = 25_000_000L`) que absorve eventos espúrios de release/press do auto-repeat de teclado do Linux/X11, mantendo o nado contínuo e estável ao segurar as teclas.
  - Normalização de eixos ortogonais (`axisX()`, `axisY()`) onde comandos de direção opostos simultâneos cancelam-se deterministicamente para neutro (0.0).
- **Câmera com Amortecimento Crítico (`Camera`):**
  - Rastreamento amortecido com decaimento exponencial (`blend = 1.0 - Math.exp(-10.0 * dt)`), eliminando o snap instantâneo rígido que propagava micro-vibrações do herói para o cenário inteiro.
  - Suporte a `snap(targetWorldX)` e `snapPrevious()` para transições de fase e respawns sem interpolação espúria.
- **Interpolação de Render Subpixel (`GameLoop`, `RenderEngine`, `Runtime3DLayer`, `HeroEntity`, `Projectile`):**
  - Desacoplamento do passo fixo da simulação (60 Hz) da taxa de atualização do monitor através do fator `alpha = accumulatedNanos / STEP_NANOS`.
  - Coordenadas interpoladas `lerp(prev, current, alpha)` para herói, projéteis e câmera no Canvas 2D e no Scene Graph 3D (`Runtime3DLayer`), eliminando saltos discretos em monitores de alta frequência (75Hz, 120Hz, 144Hz) e variações de frame pacing.

---
