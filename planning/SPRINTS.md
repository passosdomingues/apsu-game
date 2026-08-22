# 🏃 SPRINTS — As Águas de Apsu

> **Metodologia:** Sprints de 1 semana (adaptada para projeto solo/pequena equipe)
> **Duração padrão de sprint:** 5 dias úteis
> **Cerimônias:** Planning na 2ª, Review na 6ª, Retro rápida ao final

---

## 🗒️ Como ler este documento

Cada task tem o formato:

```
- [STATUS] APSU-XXX — Título da Task
  📋 Descrição clara do que deve ser feito
  ✅ Critério de aceite: como saber que está pronto
  ⚠️  Observação sênior: dica importante para não errar
  🔗 Dependências: outras tasks que devem estar prontas antes
  👤 Assignee: quem está fazendo (ou "unassigned")
  🕐 Estimativa: horas ou pontos de complexidade
```

---

## ✅ Sprint 0 — Kickoff & Game Plan
**Período:** 2026-07-28 → 2026-08-06 | **Status:** 🟢 DONE

> **Objetivo:** Definir o escopo, tecnologias e assets visuais iniciais do projeto.

### Tasks

- [x] APSU-001 — Escrever GamePlan.md com requisitos detalhados
  📋 Documento com tema, mecânicas, fases, HUD e requisitos de código
  ✅ Aceite: Arquivo existente com todas as seções preenchidas
  👤 Tech Lead | 🕐 2h

- [x] APSU-002 — Definir stack tecnológica
  📋 JavaFX 21 + Maven + Docker + MPI + Makefile
  ✅ Aceite: Stack documentada no ROADMAP.md
  👤 Tech Lead | 🕐 1h

- [x] APSU-003 — Gerar sprite do herói Adapa (Apkallu)
  📋 Imagem PNG base do personagem principal (Apkallu.png)
  ✅ Aceite: PNG em `/Game/Apkallu.png`, resolução adequada para jogo 2D
  👤 Asset Designer | 🕐 3h

- [x] APSU-004 — Gerar sprites IA: boss, npc, bg1, bg2, bg3
  📋 Kullullû (boss), Enki (NPC), 3 backgrounds das fases
  ✅ Aceite: 5 imagens geradas e aprovadas visualmente
  ⚠️  Sênior: Manter consistência de estilo (mesmo art style do Apkallu)
  👤 Asset Designer | 🕐 2h

- [x] APSU-005 — Criar estrutura de pastas Maven
  📋 `src/main/java/`, `src/main/resources/`, `planning/`
  ✅ Aceite: `mvn -version` funciona e estrutura existe
  👤 Dev DevOps | 🕐 0.5h

---

## ✅ Sprint 1 — Core Engine & Menu
**Período:** 2026-08-07 → 2026-08-13 | **Status:** 🟢 DONE

> **Objetivo:** Ter o jogo compilando e rodando com menu funcional, seleção de dificuldade e diálogo do Enki.

### 🎯 Goal da Sprint
Ao final desta sprint, executar `make run` e ver o menu animado com bolhas, selecionar dificuldade com ↑↓, pressionar ENTER, ver o diálogo do Enki e entrar na Fase 1 (mesmo que vazia).

---

### Tasks

- [x] APSU-010 — Criar `pom.xml` com JavaFX 21 e plugins Maven
  📋 Configurar: `javafx-maven-plugin`, `maven-compiler-plugin` (Java 21), `maven-assembly-plugin` para fat JAR
  ✅ Aceite: `mvn javafx:run` compila e abre janela em fullscreen sem erros
  ⚠️  Sênior: Use `<sourceDirectory>src/main/java</sourceDirectory>` explícito. Adicione profile `<classifier>linux</classifier>` no JavaFX para evitar download de módulos desnecessários. Inclua a propriedade `<mainClass>ApsuGame</mainClass>` no javafx-maven-plugin.
  🔗 Nenhuma
  👤 Dev DevOps | 🕐 1.5h

- [x] APSU-011 — Criar `ApsuGame.java` — esqueleto base
  📋 Classe principal com: imports, constantes W/H, enum State/Diff, campos principais, `main()`, `start()`, AnimationTimer básico com `update()` e `render()` vazios
  ✅ Aceite: Janela abre fullscreen preta sem exceções no console
  ⚠️  Sênior: **NUNCA** use `Thread.sleep()` dentro do AnimationTimer — ele já é chamado a cada frame pela JVM. Use `nano` (parâmetro do `handle()`) para timing. Sempre verifique se `GraphicsContext` não é compartilhado entre threads.
  🔗 APSU-010
  👤 Dev JavaFX | 🕐 2h

- [x] APSU-012 — Implementar sistema de Input (teclado)
  📋 `Set<KeyCode> keys` para teclas pressionadas continuamente. `onKey()` para eventos de pressionar uma vez. Suporte a: Setas+WASD (movimento), ESPAÇO (ação), ENTER (confirmar), ESC (sair para menu)
  ✅ Aceite: `System.out.println` mostra teclas corretamente no console
  ⚠️  Sênior: Use `Set<KeyCode>` (não Map) para detectar múltiplas teclas simultâneas. O `onKeyPressed` é para eventos pontuais (toggle, disparo). O `Set` é para movimento contínuo verificado a cada frame no `update()`. Nunca faça lógica de jogo dentro do handler de evento — só adiciona/remove do Set.
  🔗 APSU-011
  👤 Dev JavaFX | 🕐 1h

- [x] APSU-013 — Implementar tela de Menu
  📋 Background com gradiente oceânico animado, bolhas flutuantes, título estilizado em dourado, 4 opções navegáveis (Sábio, Ira de Enki, Iniciar, Créditos)
  ✅ Aceite: Menu renderiza a 60fps, navegação ↑↓ funciona, opção selecionada tem highlight dourado, créditos aparecem e somem com ESPAÇO
  ⚠️  Sênior: Para animações baseadas em tempo (bolhas, ondas), use sempre `nano / 1_000_000_000.0` convertido para segundos. Isso garante que a animação seja independente do FPS. Nunca use `System.currentTimeMillis()` para animação — use o parâmetro `now` do AnimationTimer.
  🔗 APSU-012
  👤 Dev JavaFX | 🕐 3h

- [x] APSU-014 — Implementar tela de Diálogo Enki
  📋 Avatar do Enki (PNG ou fallback geométrico), caixa de diálogo com 7 linhas de texto, navegação por ESPAÇO/ENTER, indicador de progresso (pontinhos)
  ✅ Aceite: Todas as 7 linhas de diálogo aparecem em sequência, ao final do diálogo transiciona para PHASE1
  ⚠️  Sênior: Implemente `wrapText()` para quebra automática de linha — JavaFX Canvas não quebra texto automaticamente. Estime a largura por `text.length() * fatorDeCaractere` (não é perfeito mas suficiente para o jogo). Fontes serif medievais combinam com o tema.
  🔗 APSU-013
  👤 Dev JavaFX | 🕐 2h

- [x] APSU-015 — Implementar SpriteManager (fallback geométrico)
  📋 Método `loadImg(String name)` que tenta: 1) classpath Maven, 2) filesystem `resources/`, 3) retorna `null`. Método `drawSprite()` chama `gc.drawImage()` se imagem != null, ou desenha formas geométricas
  ✅ Aceite: Com e sem os PNGs, o jogo roda sem NullPointerException. Console mostra `[INFO] nome.png não encontrado → fallback geométrico` para cada sprite faltante
  ⚠️  Sênior: Encapsule o `try/catch` no `loadImg()`. Nunca deixe propagação de exceção por arquivo ausente interromper o game loop. O fallback geométrico **não é gambiarra** — é uma feature de resiliência documentada no GamePlan.
  🔗 APSU-011
  👤 Dev JavaFX | 🕐 1.5h

- [x] APSU-016 — Copiar sprites para `src/main/resources/`
  📋 Copiar: `Apkallu.png → hero.png`, `boss_kullullu.png → boss.png`, `npc_enki.png → npc.png`, `bg1_aguas_claras.png → bg1.png`, `bg2_cavernas_coral.png → bg2.png`, `bg3_templo_submerso.png → bg3.png`
  ✅ Aceite: `mvn javafx:run` e sprites aparecem no jogo (sem usar fallback geométrico)
  ⚠️  Sênior: Sprites devem estar em `src/main/resources/` (sem subpastas) para que o `getClass().getResourceAsStream("/hero.png")` funcione corretamente. Se colocar em subpasta, ajustar o path no `loadImg()`. Valide que Maven copia para `target/classes/` com `mvn compile && ls target/classes/`.
  🔗 APSU-015
  👤 Dev DevOps | 🕐 0.5h

- [x] APSU-017 — Criar `.gitignore`
  📋 Ignorar: `target/`, `*.class`, `.idea/`, `.vscode/`, `*.iml`, arquivos de OS
  ✅ Aceite: `git status` não mostra arquivos de build ou IDE
  👤 Dev DevOps | 🕐 0.25h

---

## ✅ Sprint 2 — Game Phases & HUD
**Período:** 2026-08-07 → 2026-08-07 | **Status:** 🟢 DONE (concluída junto com Sprint 1)

> **Objetivo:** Implementar as 3 fases completas e jogáveis com HUD funcional.

### Tasks

- [x] APSU-020 — Sistema de câmera horizontal (camera scroll)
  📋 Variável `camX` seguindo o herói suavemente. Lógica de "deadzone" — herói não ativa scroll quando está nos 35% esquerdos da tela
  ✅ Aceite: Herói se move pelo mundo, câmera acompanha suavemente. Obstáculos renderizam em `worldX - camX`
  ⚠️  Sênior: A câmera simples usa `camX = max(0, heroWorldX - DEADZONE)`. Todos os objetos do mundo devem ser armazenados em **coordenadas de mundo** (worldX) e convertidos para tela na renderização (`screenX = worldX - camX`). Misturar coords de mundo e tela é o erro #1 de devs júnior em jogos 2D.
  🔗 APSU-012
  👤 Dev JavaFX | 🕐 2h

- [x] APSU-021 — Movimentação do herói (nado livre 4 direções)
  📋 WASD + Setas, sem gravidade, velocidade configurável por dificuldade. Flipar sprite horizontalmente quando muda de direção
  ✅ Aceite: Herói nada em todas as direções. Teclas diagonais funcionam (U+D ao mesmo tempo). Não ultrapassa os limites da tela
  ⚠️  Sênior: Para flipar horizontalmente com Canvas: `gc.save(); gc.translate(x + w, y); gc.scale(-1, 1); gc.drawImage(..., 0, 0, w, h); gc.restore()`. Nunca modifique a imagem original — faça a transformação no GraphicsContext e restaure com `save()/restore()`.
  🔗 APSU-020
  👤 Dev JavaFX | 🕐 1.5h

- [x] APSU-022 — Sistema de colisão (Bounding Box)
  📋 Método `rectsHit(x1,y1,w1,h1, x2,y2,w2,h2)` retornando boolean. Aplicar para: herói↔inimigos, herói↔corais, projéteis↔inimigos, projéteis↔boss, bolhas↔herói
  ✅ Aceite: Colisões detectadas visualmente corretas. Sem falsos positivos gritantes. Tolerância de ±4px é aceitável para o estilo do jogo
  ⚠️  Sênior: AABB (Axis-Aligned Bounding Box) é `x1 < x2+w2 && x1+w1 > x2 && y1 < y2+h2 && y1+h1 > y2`. Para sprites com transparência, reduza o bounding box em 20% (ex: use 80% da largura/altura real) para colisão mais justa. Isso é chamado de "hitbox menor que sprite".
  🔗 APSU-021
  👤 Dev JavaFX | 🕐 1.5h

- [x] APSU-023 — Fase 1: As Águas Claras
  📋 Scroll horizontal, 3 inimigos com movimento senoidal vertical, tabuleta no final, portal de transição
  ✅ Aceite: Herói atravessa a fase, desviando/atirando nos inimigos, coleta a 1ª tabuleta, portal aparece no final e transiciona para Fase 2
  ⚠️  Sênior: Movimento senoidal: `y = baseY + Math.sin(time * speed + phase) * amplitude`. Cada inimigo deve ter `phase` diferente (use o worldX * 0.01 como phase) para não moverem sincronizados. Isso parece mais natural e menos artificial.
  🔗 APSU-022
  👤 Dev JavaFX | 🕐 3h

- [x] APSU-024 — Fase 2: Cavernas de Coral
  📋 Corais no teto e chão (obstáculos físicos), Easter Egg do baú (ESPAÇO próximo ao baú), tabuleta no final
  ✅ Aceite: Herói bate nos corais e perde HP. Easter Egg popup aparece ao pressionar ESPAÇO perto do baú. Tabuleta coletável
  ⚠️  Sênior: Os corais são obstáculos sólidos — ao colidir, empurre o herói para fora (`if (coralIsTop) heroY = coral.y + coral.h + 2`). Aplique invencibilidade temporária (1.5s) após levar dano para evitar múltiplos hits em sequência (técnica chamada de "iframe" ou invincibility frame — presente em todo jogo de plataforma profissional).
  🔗 APSU-023
  👤 Dev JavaFX | 🕐 3h

- [x] APSU-025 — Fase 3: Boss Fight — Kullullû
  📋 Arena fixa, boss no canto direito com bobbing animation, dispara bolhas em padrão de leque, herói atira feixes de luz dirigidos ao boss
  ✅ Aceite: Boss toma 5 hits para morrer. Bolhas aparecem em intervalos (2.5s fácil / 1.2s difícil). Herói pode acertar boss com ESPAÇO. Ao matar boss, transiciona para VICTORY
  ⚠️  Sênior: Bolhas em leque: use `Math.cos(angle)*speed` e `Math.sin(angle)*speed` para cada bolha. Distribua os ângulos com `angle = baseAngle + i * (arcSpread / numBubbles)`. Projetil do herói deve mirar no centro do boss: normalize o vetor `(bossCenter - heroCenter)` e multiplique pela velocidade. Normalizar = dividir por `Math.sqrt(dx*dx + dy*dy)`.
  🔗 APSU-024
  👤 Dev JavaFX | 🕐 4h

- [x] APSU-026 — HUD completo (Vida, Tabuletas, Fase, Dificuldade)
  📋 Corações vermelhos/vazios, contador "0/3 → 3/3" de tabuletas, nome da fase atual, badge de dificuldade
  ✅ Aceite: HUD visível em todas as fases, atualiza em tempo real, semi-transparente para não bloquear o jogo
  ⚠️  Sênior: Renderize o HUD **sempre por último** (após todos os outros elementos) para que apareça sobre tudo. Use `gc.setFill(Color.rgb(0,0,0,0.7))` para fundo semi-transparente sem bibliotecas extras.
  🔗 APSU-021
  👤 Dev JavaFX | 🕐 2h

- [x] APSU-027 — Sistema de partículas
  📋 Lista de partículas `{x,y,vx,vy,life,r,g,b}`. Spawn ao: coletar tabuleta (dourado), hit inimigo (vermelho), hit boss (ciano), derrotar boss (dourado)
  ✅ Aceite: Partículas aparecem nas situações certas, duram ~1 segundo, somem suavemente
  ⚠️  Sênior: Use `list.removeIf(p -> p[4] <= 0)` para limpar partículas mortas de forma eficiente. Spawne no máximo 14 partículas por evento para não saturar. `ArrayList` com `removeIf` é adequado — não precisa de pool de objetos nesta escala.
  🔗 APSU-022
  👤 Dev JavaFX | 🕐 2h

- [x] APSU-028 — Telas de Vitória e Game Over
  📋 VICTORY: animação dourada, mensagem de encerramento, opção de menu. GAME_OVER: tela vermelha escura, contagem de tabuletas coletadas, opção reiniciar
  ✅ Aceite: Ambas as telas renderizam sem erros. ESPAÇO/ENTER/R voltam ao menu
  👤 Dev JavaFX | 🕐 1.5h

---

## ✅ Sprint 3 — Build System & DevOps
**Período:** 2026-08-07 → 2026-08-07 | **Status:** 🟢 DONE

> **Objetivo:** Qualquer pessoa consegue rodar o jogo com `make run` ou `make docker-run` sem configuração manual.

### Tasks

- [x] APSU-030 — Makefile completo
  📋 Targets: `build`, `run`, `run-local` (usa JAVAFX_PATH), `docker-build`, `docker-run`, `mpi-demo`, `clean`, `help`
  ✅ Aceite: `make help` mostra todos os targets com descrição. `make run` roda o jogo. `make docker-run` abre janela via X11 forwarding
  ⚠️  Sênior: Use `JAVAFX_PATH ?= /opt/javafx-21/lib` com `?=` para permitir override. Adicione `xhost +local:docker` antes do `docker run` para X11. Sempre valide se o Docker daemon está rodando antes do `docker-run` com `docker info 2>/dev/null || (echo "Docker não está rodando"; exit 1)`.
  👤 Dev DevOps | 🕐 2h

- [x] APSU-031 — Dockerfile (Liberica JDK 21 Full)
  📋 Multi-stage: build com Maven, runtime com Liberica JDK 21 Full (inclui JavaFX nativamente). Expor display via `ENV DISPLAY=:0`
  ✅ Aceite: `docker build -t apsu-game .` conclui sem erros. Container executa o jogo com janela visível via X11
  ⚠️  Sênior: Use `bellsoft/liberica-openjdk-debian:21-full` — é a única imagem oficial que inclui JavaFX no JDK sem configuração extra. Evite `openjdk:21` puro (não tem JavaFX). No runtime, instale `libx11-6 libxext6 libxrender1 libxtst6 libxi6` para suporte X11. O fat JAR (`-jar-with-dependencies`) é necessário porque JavaFX não é um módulo simples no classpath.
  🔗 APSU-030
  👤 Dev DevOps | 🕐 2h

- [x] APSU-032 — Suporte a run-local com JAVAFX_PATH
  📋 Target `run-local` usa `javac --module-path $JAVAFX_PATH` e `java --module-path $JAVAFX_PATH --add-modules javafx.controls,javafx.graphics`
  ✅ Aceite: `JAVAFX_PATH=/opt/javafx-21/lib make run-local` compila e executa sem Maven
  ⚠️  Sênior: Para compilação direta: `javac --module-path $JAVAFX_PATH --add-modules javafx.controls,javafx.graphics -d target/classes src/main/java/ApsuGame.java`. Para execução: `java --module-path $JAVAFX_PATH --add-modules javafx.controls,javafx.graphics -cp target/classes ApsuGame`. Copie os resources antes da execução.
  🔗 APSU-030
  👤 Dev DevOps | 🕐 1.5h

- [x] APSU-033 — Demo MPI: Gerador de Mapa Paralelo
  📋 Arquivo `mpi/MapGenerator.c` que usa MPI para gerar tiles de mapa em paralelo, aproveitando os 8 cores do i7-8565U
  ✅ Aceite: `make mpi-demo` roda com `mpirun -np 8 ./map_gen` e imprime tiles gerados por cada processo. Instala `openmpi-bin` se necessário
  ⚠️  Sênior: MPI_Init/MPI_Finalize em todo programa MPI. Use `MPI_Comm_rank()` para ID do processo e `MPI_Comm_size()` para total. O padrão de trabalho paralelo aqui é: processo 0 divide os tiles, distribui com `MPI_Scatter`, cada processo gera sua fatia, coleta com `MPI_Gather`. Print com `MPI_Comm_rank` para identificar qual core gerou o quê.
  👤 Dev DevOps | 🕐 3h

---

## 🟢 Sprint 4 — Polish & Assets
**Período:** 2026-08-08 → 2026-08-14 | **Status:** 🟢 DONE

> **Objetivo:** Elevar a qualidade visual e sonora. O jogo deve impressionar à primeira vista.

### Tasks

- [x] APSU-040 — Validar e ajustar sprites no jogo
  📋 Verificar proporções, transparência, recorte. Ajustar tamanho de renderização se necessário
  ✅ Aceite: Sprites aparecem sem artefatos, tamanho proporcional ao canvas 1366x768
  👤 Asset Designer | 🕐 3h

- [x] APSU-041 — Parallax de fundo (2 camadas)
  📋 Background scrollando a 30% da velocidade do herói (camada distante), midground a 60%
  ✅ Aceite: Sensação de profundidade ao nadar, backgrounds não cortam abruptamente
  🔗 APSU-020
  👤 Dev JavaFX | 🕐 2h

- [x] APSU-042 — Efeitos sonoros (opcional)
  📋 Sons para: nadar (ambient), tiro (whoosh), hit inimigo, coletar tabuleta, morte, vitória
  ✅ Aceite: Sons tocam sem delay perceptível. Não travam o game loop
  👤 Dev JavaFX | 🕐 3h

- [x] APSU-043 — Benchmark de performance
  📋 Medir FPS com `AnimationTimer`. Log se FPS < 55. Identificar gargalos com profiler
  ✅ Implementado: `FrameMetrics` mede FPS em janelas de 1 s e `GameLoop` registra alerta abaixo de 55 FPS, sem spam no console. 60 FPS com aceleração GPU Prism (`es2`).
  👤 Dev JavaFX | 🕐 2h

---

## 🟢 Sprint 11 — Redesign de Cenários & Visual Procedural
**Período:** 2026-08-21 | **Status:** 🟢 DONE

> **Objetivo:** Redesenhar o layout das Fases 2–4 para eliminar a frustração de colisões/obstáculos e enriquecer o visual com renderização procedural em Canvas e pipeline Blender 3D.

### Tasks

- [x] APSU-060 — Redesign do layout da Fase 2 (Cavernas de Coral)
  📋 6 colunas em S legível, inimigos nas salas abertas, baú visível
  ✅ Aceite: Padrão previsível e espaçoso para navegação do jogador

- [x] APSU-061 — Redesign do layout da Fase 3 (Correntes Abissais)
  📋 4 inimigos bem separados, 3 zonas de corrente sequenciais
  ✅ Aceite: Progressão sem acúmulo caótico de perigos

- [x] APSU-062 — Redesign do layout da Fase 4 (Abismo Vulcânico)
  📋 5 inimigos com Leviatã isolado como sub-boss (600px livres)
  ✅ Aceite: Leviatã se destaca como momento especial de combate

- [x] APSU-063 — Visual Procedural no RenderEngine
  📋 Corais orgânicos (`drawOrganicCoral`), filamentos de correntes (`drawCurrentZone`), gêiser sólido (`drawGeyser`), lava + faíscas + vapor
  ✅ Aceite: Fases ricas visualmente a 60 FPS estáveis

- [x] APSU-064 — Pipeline 3D Blender Automatizado
  📋 Integração de `/opt/blender-4.5.5-lts/blender` e execução do `gerador_mestre_apsu.py`
  ✅ Aceite: Modelos .blend gerados e sincronizados sem erros

---

## ⚪ Sprint 5 — QA & Release v1.0.0
**Período:** 2026-09-04 → 2026-09-10 | **Status:** ⚪ TODO

> **Objetivo:** Produto estável, documentado e empacotado para distribuição.

### Tasks

- [ ] APSU-050 — Testes manuais de regressão
  📋 Checklist: menu, dificuldades, todas as fases, vitória, game over, restart, ESC, easter egg, limites de tela
  ✅ Aceite: Nenhum crash ou NullPointerException em 3 runs completos do jogo
  👤 QA / Toda equipe | 🕐 2h

- [ ] APSU-051 — Gerar fat JAR executável
  📋 `mvn package` gera `apsu-game-1.0.jar` com todas as dependências. Testar execução standalone
  ✅ Aceite: `java -jar apsu-game-1.0.jar` roda o jogo sem Maven instalado
  👤 Dev DevOps | 🕐 1.5h

- [ ] APSU-052 — Avaliar Hunyuan3D localmente
  📋 Testar CPU-only mode do Hunyuan3D-2.0 via Docker. Documentar resultado (tempo de inferência, qualidade)
  ✅ Aceite: Relatório documentado em `planning/EXPERIMENTS.md` com tempo de geração e viabilidade real
  👤 Tech Lead | 🕐 4h

- [ ] APSU-053 — README.md final do projeto
  📋 Como instalar, rodar localmente, via Docker, com JavaFX local. Screenshots do jogo. Controles
  ✅ Aceite: Dev novo consegue rodar o jogo seguindo só o README, sem perguntar nada
  👤 Tech Lead | 🕐 2h

- [ ] APSU-054 — Tag v1.0.0 no Git
  📋 `git tag -a v1.0.0 -m "Release v1.0.0 — As Águas de Apsu"` + push
  ✅ Aceite: Tag visível no repositório remoto
  🔗 APSU-050, APSU-051
  👤 Tech Lead | 🕐 0.25h

---

## 📊 Velocity & Métricas

| Sprint | Tasks Total | Tasks Done | Horas Estimadas | Horas Reais | Status |
|---|---|---|---|---|---|
| Sprint 0 | 5 | 5 | 8.5h | ~8h | 🟢 DONE |
| Sprint 1 | 8 | 8 | 11.75h | ~2h | 🟢 DONE |
| Sprint 2 | 9 | 9 | 20h | ~3h* | 🟢 DONE |
| Sprint 3 | 4 | 4 | 8.5h | ~1h | 🟢 DONE |
| Sprint 4 | 4 | 4 | 10h | ~4h | 🟢 DONE |
| Sprint 11 | 5 | 5 | 12h | ~5h | 🟢 DONE |
| Sprint 5 | 5 | 0 | 9.75h | — | ⚪ TODO |
| **TOTAL** | **40** | **35** | **80.5h** | **~23h** | **87.5% done** |


> \* Sprints 1 e 2 foram implementadas em uma única sessão intensiva (monolito ApsuGame.java com tudo junto). Isso foi possível pelo design monolítico (ADR-001) que elimina overhead de integração entre módulos.
