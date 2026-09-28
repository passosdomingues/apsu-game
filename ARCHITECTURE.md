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

- `Launcher.java`: Wrapper que permite executar o Fat JAR via `java -jar` sem passar argumentos `--module-path` JavaFX.
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

## 4. Pipeline Gráfico 2.5D e Renderização (`RenderEngine` / `Runtime3DLayer`)

- **Renderização Imperativa Canvas:** Renderiza via `GraphicsContext` JavaFX, eliminando o overhead de retenimento de nós do Scene Graph.
- **Transformação de Coordenadas:** A `Camera` converte Coordenadas de Mundo em Coordenadas de Tela (`screenX = worldX - camX`).
- **Cenário 2.5D:** Planos de fundo, paralaxe, partículas e HUD continuam no Canvas JavaFX.
- **Malhas 3D em tempo real:** `Runtime3DLayer` carrega personagens e elementos de fase (naufrágio, baú, recifes/cardumes, ruínas, correntes, portal e obstáculos) como OBJ/MTL exportados do Blender, numa `SubScene` transparente sincronizada com a câmera.
- **Nado leve:** A pose base 3D recebe balanço, squash e inclinação limitada a 20°; as poses de ataque seguem os quadros OBJ exportados.
- **Shaders de Iluminação:** O `LightingEngine` aplica iluminação emissiva de contorno (Fresnel Rim Light) e dispersão sob a superfície (SSS).

---

## 5. Barramento de Eventos e Áudio (`EventBus` / `AudioManager`)

- O `EventBus` implementa o padrão Publish/Subscribe desacoplado.
- Entidades disparam `GameEvent` quando ocorrem ações (disparo, dano, coleta, morte, transição de fase).
- O `AudioEventSubscriber` escuta o barramento e solicita a reprodução de efeitos sonoros WAV ao `AudioManager` em threads de áudio dedicadas.

---

## 6. Geração Distribuída de Mapas (`mpi/src/MapGenerator.c`)

- Módulo independente compilado em C e OpenMPI.
- Divide a malha do mapa entre as threads de CPU disponíveis utilizando `MPI_Scatter` e consolida a matriz via `MPI_Gather`.
- Exporta os arquivos JSON das 5 fases (`mpi/generated/phase-1.json` a `phase-5.json`), que podem ser lidos pelo `MPIMapLoader.java`.
