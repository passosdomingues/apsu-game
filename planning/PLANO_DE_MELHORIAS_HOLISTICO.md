# 🎯 Plano Holístico de Melhorias — As Águas de Apsu

> **Documento Estratégico de Evolução Técnica e Artística**
> **Data:** 2026-08-20 | **Versão:** 1.0.0
> **Projeto:** As Águas de Apsu (Jogo 2.5D Mesopotâmico — Java 21 / Blender 4.5 / MPI)
> **Autor:** Equipe de Arquitetura & Computação Gráfica

---

## 📌 1. Sumário Executivo & Diagnóstico Geral

Após uma auditoria completa de código, assets 3D, infraestrutura de build e fundamentos de Computação Gráfica aplicada, este documento estabelece um **Plano Holístico de Melhorias**. O objetivo é elevar o projeto do seu estado atual de maturidade (5 fases funcionais, 100% de cobertura de entidades e 19 testes automatizados) para uma **plataforma de nível comercial e referência acadêmica**.

A análise holística abrange **5 dimensões estratégicas**:

```
                               ┌────────────────────────────────────────┐
                               │     PLANO HOLÍSTICO DE MELHORIAS      │
                               └───────────────────┬────────────────────┘
                                                   │
     ┌───────────────────┬───────────────────┼───────────────────┬───────────────────┐
     ▼                   ▼                   ▼                   ▼                   ▼
┌───────────┐       ┌───────────┐       ┌───────────┐       ┌───────────┐       ┌───────────┐
│ DIMENSÃO 1│       │ DIMENSÃO 2│       │ DIMENSÃO 3│       │ DIMENSÃO 4│       │ DIMENSÃO 5│
│Arquitetura│       │Computação │       │Game Design│       │ Qualidade │       │  DevOps & │
│ & Software│       │  Gráfica  │       │  & UX/UI  │       │ & Testes  │       │Distribuição│
└───────────┘       └───────────┘       └───────────┘       └───────────┘       └───────────┘
```

---

## 🏛️ Dimensão 1 — Arquitetura de Software & Engine Java

### 1.1 Estado Atual
A refatoração para os pacotes `br.apsu.*` estabeleceu uma boa separação inicial entre `core`, `graphics`, `model` e `audio`. Contudo, subsistemas cruciais como áudio e tratamento de eventos ainda apresentam **acoplamento direto** com a regra de negócio do `GameContext.java`.

### 1.2 Oportunidades & Ações de Melhoria

#### 🚀 Ação 1.1: Barramento de Eventos Desacoplado (Event Bus Pattern)
* **Problema:** O `GameContext.java` chama diretamente métodos estáticos como `AudioManager.getInstance().playSound("hurt")` em mais de 35 pontos de código.
* **Solução:** Implementar um **Event Bus** genérico e assíncrono.
* **Arquitetura Proposta:**

```java
// Contrato do Evento
public record GameEvent(EventType type, Object data, double worldX, double worldY) {
    public enum EventType {
        HERO_DAMAGED, HERO_SHOOT, ENEMY_KILLED, TABLET_COLLECTED, GEYSER_TRIGGERED, BOSS_PHASE_CHANGE
    }
}

// Inscrição Desacoplada:
EventBus.subscribe(EventType.HERO_DAMAGED, event -> {
    AudioManager.getInstance().playSound("hurt");
    particleSystem.addBurst(event.worldX(), event.worldY(), Color.RED);
});
```

> [!TIP]
> **Benefício:** Elimina a dependência circular entre lógica do jogo e subsistemas de saída (áudio e partículas), permitindo testar `GameContext` em ambientes sem headless audio.

---

#### 🚀 Ação 1.2: Otimização de Colisão com Particionamento Espacial (QuadTree)
* **Problema:** A checagem de colisões (`rectsHit`) atualmente executa uma varredura $O(N \times M)$ entre todos os projéteis, inimigos e elementos do cenário a cada frame.
* **Solução:** Introduzir uma estrutura de dados de **QuadTree 2D** no `GameContext.java`.

$$\text{Complexidade de Colisão: } O(N \cdot M) \longrightarrow O(N \log M)$$

* **Critério de Aceite:** Manter framerate estável em 60 FPS com mais de 200 entidades simultâneas na tela sem picos de *GC Pause*.

---

#### 🚀 Ação 1.3: Sistema de Persistência de Dados (`SaveManager` JSON)
* **Problema:** O jogo não preserva progresso entre sessões (HP do herói, tabuletas coletadas, fases desbloqueadas).
* **Solução:** Criar a classe `br.apsu.core.SaveManager` utilizando `Jackson` ou `Gson` para salvar em `~/.apsu/savegame.json`.

```json
{
  "version": "1.0",
  "difficulty": "MEDIO",
  "selectedHero": "HERO_ADAPA",
  "unlockedPhase": 4,
  "tabletsCollected": [true, true, true, true, false],
  "highScore": 14500
}
```

---

#### 🚀 Ação 1.4: Roteiro de Transição Incremental FXGL (ADR-009 — Fase 2)
* **Problema:** A classe `ApsuFXGLGame.java` é um protótipo estático.
* **Solução:** Implementar uma ponte de componentes onde entidades simples (como decorações de cenário e partículas) migram para o ecossistema ECS (Entity-Component-System) do FXGL 21.1 sem interromper o loop Canvas de produção.

---

## 🎨 Dimensão 2 — Computação Gráfica, Shaders & Pipeline 3D

### 2.1 Estado Atual
Os scripts Python (`bpy`) em `personagens/scripts/` introduziram recursos como **Fresnel Rim Light**, **Subsurface Scattering (SSS)** e **curvas anguiliformes procedurais**. No entanto, esses recursos visuais dependem de bake 2D estático e não utilizam iluminação dinâmica no runtime JavaFX.

### 2.2 Oportunidades & Ações de Melhoria

#### 🚀 Ação 2.1: Mapeamento de Normais 2.5D (Normal Mapping no Canvas)
* **Problema:** A iluminação do jogo no JavaFX é plana sobre os sprites 2D baked.
* **Solução:** Modificar a pipeline do Blender (`tools/render_all_2d5_sprites.py`) para gerar dois mapas por frame: a textura de cor (Albedo) e o mapa de normais de espaço de tela (Normal Map).
* **Implementação no `LightingEngine.java`:**

```
  [ Sprite Albedo PNG ] + [ Normal Map PNG ] 
             │                    │
             └──────────┬─────────┘
                        ▼
    [ Pixel Shader JavaFX / Canvas Normal Lighting ]
    I_diffuse = N • L  (Produto Escalar da Normal com Luz)
```

> [!NOTE]
> Essa técnica traz profundidade 3D real às escamas do Adapa e ao corpo de obsidiana do Kullullû quando passam perto de luzes bioluminescentes ou gêiseres de lava.

---

#### 🚀 Ação 2.2: Integração Total das 4 Poses de Ataque Procedurais
* **Problema:** O script `01_adapa_ataque_variacoes.py` gerou 4 poses de ataque 3D incríveis (`thrust`, `slash`, `spin`, `charge`), mas a mecânica de ataque no Java ainda aciona apenas o disparo de bolhas.
* **Solução:** Vincular o estilo de ataque à mecânica do tridente:
  * **Thrust (Estocada):** Ataque corpo a corpo de curto alcance com alto knockdown.
  * **Slash (Corte Arcado):** Destroi projéteis inimigos em arco de 180°.
  * **Spin (Giro 360°):** Repete ataque em área contra peixes sombrios e medusas.
  * **Charge (Carga):** Disparo concentrado de bolha grande com física de penetração.

---

#### 🚀 Ação 2.3: Re-topologia e Continuidade de Malha no Blender (Voxel Remesh)
* **Problema:** Alguns modelos 3D (como a cauda dos Apkallus e o corpo do Leviatã) usam primitivas empilhadas em hierarquia, gerando "costuras" visíveis no render.
* **Solução:** Aplicar o modificador **Voxel Remesh + Smooth** nos scripts Python do Blender para fundir os segmentos em uma malha única e contínua com rig de ossos (*Armature Weighting*).

---

#### 🚀 Ação 2.4: Shaders de Distorção de Água e Causticas Dinâmicas
* **Problema:** O efeito de caustica no `LightingEngine.java` é desenhado como ovais simples.
* **Solução:** Implementar um shader de ruído Perlin/Simplex em JavaFX para gerar refração de luz aquática em tempo real sobre a tela.

---

## 🎮 Dimensão 3 — Game Design, Level Design & Experiência (UX/UI)

### 3.1 Estado Atual
O jogo conta com 5 fases estruturadas e com atmosferas distintas. No entanto, o balanceamento de dificuldade entre as fases é rígido e a luta contra o Boss Kullullû precisa de mais fases telegrafadas.

### 3.2 Oportunidades & Ações de Melhoria

#### 🚀 Ação 3.1: Reestruturação do Boss Fight Kullullû em 3 Fases Telegrafadas
* **Problema:** O Kullullû ataca disparando projéteis com padrão estático.
* **Solução:** Transformar a batalha da Fase 5 em um combate em 3 estágios com *Telegraphing* visual nítido (brilho de aviso em 0.5s):

```
 ┌────────────────────────────────────────────────────────────────────────┐
 │                      ESTÁGIOS DO BOSS KULLULLÛ                         │
 ├────────────────────────────────────────────────────────────────────────┤
 │ ESTÁGIO 1 (HP 100%-66%): Obsidiana Fria — Disparos direcionados normais│
 │ ESTÁGIO 2 (HP 65%-33%) : Fusão Vulcânica — Rugido + Ondas de Choque  │
 │ ESTÁGIO 3 (HP 32%-0%)  : Fúria de Apsu — Meteoros de Lava + Cargas   │
 └────────────────────────────────────────────────────────────────────────┘
```

---

#### 🚀 Ação 3.2: Ajuste Dinâmico de Dificuldade (DDA Engine)
* **Problema:** Jogadores novatos podem travar na Fase 3 (Correntes), enquanto veteranos acham a Fase 1 muito fácil.
* **Solução:** Aprimorar o método `getThreatMultiplier()` em `GameContext.java` para avaliar continuamente a taxa de acerto/erro do jogador:

$$\text{ThreatMult} = \text{BaseDiff} + \left( \frac{\text{Dano Tomado}}{\text{Tempo de Fase}} \right) \cdot \alpha - (\text{Inimigos Mortos}) \cdot \beta$$

---

#### 🚀 Ação 3.3: Minimapa e Radar Retrátil no HUD
* **Problema:** Em fases longas com scroll (4000px), o jogador pode perder a localização dos Guardiões ou Baús.
* **Solução:** Adicionar um radar minimapa estilizado no canto superior direito do HUD via `UIRenderer.java`.

---

#### 🚀 Ação 3.4: Polimento Tátil & Efeitos de Câmera (*Screen Shake* & Juice)
* **Problema:** Impactos fortes (como tomar dano de lava ou acertar o boss) carecem de feedback físico na tela.
* **Solução:** Implementar impulso de trepidação na câmera (`camera.applyShake(intensity, duration)`) e efeito de *Slow Motion* (time dilation) de 0.2s ao derrotar o Boss.

---

## 🧪 Dimensão 4 — Qualidade de Código, Testabilidade & Cobertura

### 4.1 Estado Atual
A suíte de testes JUnit 5 possui 19 testes integrados e unitários focados nas entidades lógicas e na matemática de câmera/projéteis.

### 4.2 Oportunidades & Ações de Melhoria

#### 🚀 Ação 4.1: Expansão da Cobertura de Testes (Target > 85%)
* **Problema:** Subsistemas de rendering, gerenciamento de sprite e estado de overlay não possuem testes unitários.
* **Solução:** Criar novas classes de teste:
  * `SpriteManagerTest.java`: Valida o carregamento de sequências e tratamentos de fallback para PNGs inexistentes.
  * `LightingEngineTest.java`: Testa os cálculos de halos bioluminescentes e interpolação de cores de Fresnel.
  * `SaveManagerTest.java`: Garante integridade do salvamento/carregamento JSON.

---

#### 🚀 Ação 4.2: Testes de Regressão Visual Automáticos (Headless Canvas Snapshots)
* **Problema:** Alterações no `RenderEngine.java` podem introduzir bugs visuais imperceptíveis nos testes unitários tradicionais.
* **Solução:** Implementar testes de captura de tela automatizados (*Snapshot Testing*):

```java
@Test
void verifyRenderP1Snapshot() {
    Image snapshot = renderEngine.renderToImage(mockContextP1);
    double diff = ImageComparator.pixelDiff(snapshot, referenceP1Image);
    assertTrue(diff < 0.01, "Divergência visual detectada na Fase 1!");
}
```

---

#### 🚀 Ação 4.3: Microbenchmarks de Performance (JMH Harness)
* **Problema:** Não há dados empíricos sobre o tempo exato gasto na renderização do Canvas vs. atualização de física.
* **Solução:** Adicionar o módulo **JMH (Java Microbenchmark Harness)** para medir em nanosegundos o tempo de execução de `GameContext.update()` e `RenderEngine.render()`.

---

## 📦 Dimensão 5 — DevOps, Infraestrutura & Distribuição

### 5.1 Estado Atual
O projeto possui um excelente `Makefile`, suporte a Docker com repasse X11 e demonstração paralela em OpenMPI.

### 5.2 Oportunidades & Ações de Melhoria

#### 🚀 Ação 5.1: Pipeline de CI/CD (GitHub Actions / GitLab CI)
* **Problema:** A validação do build e dos testes exige execução manual do desenvolvedor via `make test`.
* **Solução:** Criar o arquivo `.github/workflows/ci.yml`:

```yaml
name: Apsu Game CI/CD Pipeline
on: [push, pull_request]
jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run Maven Tests
        run: mvn test
      - name: Validate MPI Build
        run: mpicc -O2 -o mpi/map_gen mpi/MapGenerator.c
```

---

#### 🚀 Ação 5.2: Empacotamento Nativo Cross-Platform (`jpackage` & GraalVM)
* **Problema:** O usuário final precisa ter o Java 21 e o JavaFX instalados para rodar o Fat JAR.
* **Solução:** Configurar o plugin `jpackage` no `pom.xml` para gerar instaladores nativos autocontidos (`.deb` para Ubuntu/Debian, `.AppImage` e executáveis Windows).

---

#### 🚀 Ação 5.3: Container Docker Multi-Stage Otimizado
* **Problema:** A imagem Docker atual reinstala dependências a cada build.
* **Solução:** Refatorar o `Dockerfile` com estelas multi-stage (Stage 1: Build Maven; Stage 2: Runtime enxuto Liberica JDK 21 Full).

---

## 🗺️ Matriz de Roteiro e Execução Sequencial

Para garantir a execução sem regressões, as melhorias foram organizadas em **5 Sprints sequenciais**:

| Sprint | Foco Principal | Ações Incluídas | Entregável Principal |
|---|---|---|---|
| **Sprint 7** | **Release v1.0.0 & Polimento** | Ação 2.2, 3.4, 4.1 | Tag Git v1.0.0, Fat JAR consolidado e testes 100% integrados |
| **Sprint 8** | **Arquitetura & Eventos** | Ação 1.1, 1.3, 4.2 | Event Bus desacoplado e sistema de Save/Load JSON |
| **Sprint 9** | **Visual 2.5D & Normais** | Ação 2.1, 2.3, 2.4 | Normal Mapping 2.5D no Canvas e re-topologia Voxel Remesh |
| **Sprint 10** | **Game Design & Boss** | Ação 1.2, 3.1, 3.2, 3.3 | Boss Fight 3 Fases, QuadTree $O(N \log M)$ e Minimapa |
| **Sprint 11** | **DevOps & Distribuição** | Ação 5.1, 5.2, 5.3, 4.3 | GitHub Actions CI/CD e pacotes nativos `.deb`/`.AppImage` |

---

<div align="center">

**Aprovado para Execução Estratégica pelo Time de Desenvolvimento**

</div>
