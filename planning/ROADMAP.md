# 🗺️ ROADMAP — As Águas de Apsu: A Lenda dos Apkallu

> **Versão do documento:** 2.0.0
> **Última atualização:** 2026-08-20 (Sprints 7–10: qualidade, persistência, eventos, radar e combate do boss)
> **Autor:** Equipe de Desenvolvimento
> **Status geral:** 🟡 IN_PROGRESS — jogo com as 5 fases jogáveis, agora em passe de física/animação/direção de arte

---

## 🎯 Visão do Produto

> *"Um jogo 2D de nado livre com tema de Mitologia Mesopotâmica, onde o herói Adapa — um Apkallu (homem-peixe) — nada pelas profundezas do oceano primordial Apsu para recuperar as 5 Tabuletas da Sabedoria e derrotar o boss corrompido Kullullû."*

**Plataforma alvo:** Desktop Linux (Intel i7-8565U / 16GB RAM)
**Runtime:** Java 21 LTS + JavaFX 21
**Build:** Maven 3.9+ / Docker / GNU Make

---

## 🏁 Milestones

```
v0.1.0 ── Sprint 0 ── [✅ DONE]       Estrutura inicial + Game Plan
v0.2.0 ── Sprint 1 ── [✅ DONE]       Core Engine + Menu + Dialogue
v0.3.0 ── Sprint 2 ── [✅ DONE]       Fases 1, 2, 3 + HUD completo
v0.4.0 ── Sprint 3 ── [✅ DONE]       Build System (Maven+Docker+MPI)
v0.5.0 ── Sprint 4 ── [✅ DONE]       Arquitetura br.apsu.* + testes automatizados
v0.6.0 ── Sprint 5 ── [✅ DONE]       5 Fases + Correntes/Lava + Fixes B1/B2/B3
v0.7.0 ── Sprint 6 ── [✅ DONE]       Auditoria + Empuxo/Arrasto realistas + Rim Light + Ciclo de nado real
v0.8.0 ── Sprint 8 ── [✅ DONE]       Save/Load JSON + Event Bus desacoplado
v0.9.0 ── Sprint 10 ── [✅ DONE]      Radar e combate do boss em três estágios
v1.0.0 ── Sprint 11 ── [✅ DONE]      Redesign Fases 2-4 + Visual Procedural + Pipeline Blender 3D (37/37 testes)
v1.1.0 ── Sprint 12 ── [✅ DONE]      Overhaul UI sem Emojis + Rendering Fixes + Hitboxes & Física Responsiva
v1.0.0 ── Sprint 13 ── [✅ DONE]      Particionamento QuadTree 2D + 41 Testes Automatizados + Fat JAR Release 1.0.0
```

---

## 📅 Fases do Projeto

### Fase 1 — Fundação (Sprints 0–1) ✅ DONE

| Entregável | Status |
|---|---|
| Estrutura de pastas Maven | ✅ DONE |
| `GamePlan.md` definido | ✅ DONE |
| Sprites gerados por IA (hero, boss, npc, bg1-3) | ✅ DONE |
| Motor do jogo (`br.apsu.core.GameContext` + `GameLoop`) | ✅ DONE |
| Menu com seleção de dificuldade | ✅ DONE |
| Diálogo Enki funcional | ✅ DONE |
| `pom.xml` configurado | ✅ DONE |

---

### Fase 2 — Conteúdo (Sprint 2) ✅ DONE

| Entregável | Status |
|---|---|
| Fase 1 — As Águas Claras (scroll + inimigos + tabuleta) | ✅ DONE |
| Fase 2 — Cavernas de Coral (corais + easter egg + tabuleta) | ✅ DONE |
| Sistema de colisão (Bounding Box) | ✅ DONE |
| HUD completo (vida + tabuletas + fase) | ✅ DONE (fix de sobreposição na Sprint 6) |
| Telas de Vitória e Game Over | ✅ DONE |
| Sistema de partículas | ✅ DONE |

---

### Fase 3 — DevOps (Sprint 3) ✅ DONE

| Entregável | Status |
|---|---|
| `Makefile` com targets: build, run, docker-build, docker-run, mpi-demo, render-sprites | ✅ DONE |
| `Dockerfile` (JDK 21 Temurin) | ✅ DONE |
| `.gitignore` completo | ✅ DONE |
| Suporte a JavaFX local | ✅ DONE |
| Demo MPI com 8 cores (i7-8565U) | ✅ DONE |

---

### Fase 4 — Arquitetura & Expansão (Sprint 4) ✅ DONE

| Entregável | Status |
|---|---|
| Refatoração pra pacotes `br.apsu.{core,graphics,model}` | ✅ DONE |
| Suíte de testes automatizados (37/37 em 2026-08-21) | ✅ DONE |
| Animações procedurais no motor Java (tail wave, pitch, ataque em 4 estágios) | ✅ DONE |

---

### Fase 5 — 5 Fases Completas & Física Ambiental (Sprint 5) ✅ DONE — 2026-08-15

| Entregável | Status |
|---|---|
| B1/B2/B3 — fixes de escala/coordenadas | ✅ DONE |
| Fase 3 — Correntes Abissais (correntes de água, zonas de pressão) | ✅ DONE |
| Fase 4 — Abismo Vulcânico (arraião, leviatã, lagoas de lava) | ✅ DONE |
| Fase 5 — Templo de Apsu (boss fight Kullullû, Enki em pessoa) | ✅ DONE |
| `bg4.png`, `bg5.png` | ✅ DONE |

---

### Fase 6 — Auditoria, Física Real & Direção de Arte (Sprint 6) ✅ DONE — 2026-08-15

| Entregável | Status |
|---|---|
| Bug: partícula da Fase 5 nascendo fora da tela (offset de câmera residual) | ✅ DONE |
| Bug: HUD sobrepondo texto na Fase 5 (nome do herói vs. nome da fase) | ✅ DONE |
| Física de empuxo realista (oscilação amortecida por velocidade, não constante) | ✅ DONE |
| Arrasto quadrático (regime turbulento de natação, não decaimento linear) | ✅ DONE |
| Squash & stretch no herói (incl. pop de impacto no ataque) | ✅ DONE |
| Paletas 60/30/10 medidas de verdade nos 5 backgrounds | ✅ DONE |
| Rim light (Fresnel) — herói, boss, Enki, inimigos, guardião | ✅ DONE |
| SSS real na escama do herói (era 0.12, quase zero) | ✅ DONE |
| Ciclo de nado 3D real (`swim_cycle_keyframes`) | ✅ DONE |
| Variações de pose de ataque 3D (thrust/slash/spin/charge) | ✅ DONE |
| Suíte de testes executada via Maven | ✅ DONE (37/37 testes) |
| Pipeline Blender 3D (`gerador_mestre_apsu.py`) executado | ✅ DONE |

---

### Fase 11 — Redesign de Cenários & Visual Procedural ✅ DONE — 2026-08-21

| Entregável | Status |
|---|---|
| Redesign de layout da Fase 2 (6 corais em S, inimigos nas salas abertas) | ✅ DONE |
| Redesign de layout da Fase 3 (4 inimigos, 2 obstáculos, correntes sequenciais) | ✅ DONE |
| Redesign de layout da Fase 4 (5 inimigos, Leviatã isolado como sub-boss com 600px de arena limpa) | ✅ DONE |
| Corais procedurais orgânicos no RenderEngine (`drawOrganicCoral`) | ✅ DONE |
| Filamentos fluídos em movimento nas zonas de corrente (`drawCurrentZone`) | ✅ DONE |
| Coluna sólida translúcida + cápsula nos gêiseres (`drawGeyser`) | ✅ DONE |
| Faixa de lava, vapor e faíscas incandescentes (*embers*) emergindo das rochas na Fase 4 | ✅ DONE |
| Recifes e vida marinha de fundo na Fase 2 | ✅ DONE |
| Validação de build Maven e suíte de 37 testes automatizados | ✅ DONE |

---

### Fase 7 — Release (Sprint 7) ⚪ TODO

| Entregável | Status |
|---|---|
| Testes de regressão manual (as 5 fases, de ponta a ponta) | ⚪ TODO |
| Fat JAR executável (`mvn package`) | ⚪ TODO |
| README completo com instruções de instalação | ⚪ TODO |
| Tag v1.0.0 no Git (projeto tem só 3 commits — muito trabalho das Sprints 4-6 está sem commit, ver auditoria) | ⚪ TODO |

### Fase 8 — Arquitetura & Progresso ✅ DONE — 2026-08-20

| Entregável | Status |
|---|---|
| Save/load JSON versionado e tolerante a corrupção | ✅ DONE |
| Preferências e progresso restaurados pelo `GameContext` | ✅ DONE |
| Event Bus e adaptador de áudio desacoplado | ✅ DONE |

### Fase 10 — Navegação & Boss ✅ DONE — 2026-08-20

| Entregável | Status |
|---|---|
| Radar/minimapa no HUD | ✅ DONE |
| Kullullû em três estágios com telegraph de 0,5 s | ✅ DONE |

---

## 🔮 Futuro / Backlog (pós v1.0.0)

> Ideias aprovadas para versões futuras. **Não implementar antes da v1.0.0.**

- [ ] **v1.1.0** — Sistema de save/load com serialização JSON
- [ ] **v1.2.0** — Fase bônus: O Abismo de Nammu (boss alternativo)
- [ ] **v1.3.0** — Multiplayer local (2 Apkallus)
- [ ] **v1.4.0** — Editor de fases com JavaFX Scene Builder
- [ ] **v2.0.0** — Port para LibGDX para mobile (Android)
- [ ] Estender squash & stretch e rim light pros inimigos/guardiões no lado Java (hoje só o herói recebe no `RenderEngine`)
- [ ] Continuidade de silhueta nos modelos 3D (hoje são primitivas empilhadas — testar Skin modifier / remesh voxel numa cauda pra ver se fica mais "peixe" e menos "boneco de neve")
- [ ] Corrigir `blender_render_sprites.py` (pipeline MPI paralelo, `tools/render_blender_mpi_worker.sh`) com o mesmo passe de keyframes — não foi tocado na Sprint 6, escopo separado do `render_all_2d5_sprites.py`

---

## ⚠️ Riscos e Dependências

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| JavaFX incompatível com Docker headless | Média | Alto | Usar Xvfb ou VNC no container |
| Performance < 60 FPS em cenas densas | Baixa | Médio | Profiling com AnimationTimer + Canvas clipping |
| MPI não instalado localmente | Baixa | Baixo | Incluir instalação no Makefile (apt-get) |
| Hunyuan3D não roda na GPU AMD | Alta | Baixo | CPU-only mode ou Cloud API |
| **Novo:** edições Java da Sprint 6 não foram compiladas/testadas de verdade (ambiente de auditoria sem Maven/JavaFX) | Certa | Médio | Rodar `make test && make build` antes de aceitar a sprint |
| **Novo:** scripts Blender da Sprint 6 não foram executados (ambiente de auditoria sem Blender/GPU) | Certa | Médio | Rodar no Blender do Rafa + checklist visual em `00_LEIA_ME.md` |
| **Novo:** trabalho das Sprints 4-6 sem commit (git só tem 3 commits, muita coisa "untracked") | Alta | Médio | Commitar em blocos lógicos antes de seguir acumulando |

---

## 👥 Time (Roles)

| Role | Responsabilidade |
|---|---|
| **Tech Lead / Sênior** | Arquitetura, revisão de código, decisões técnicas |
| **Dev JavaFX** | Motor do jogo, física, rendering |
| **Dev DevOps** | Makefile, Dockerfile, MPI, CI |
| **Game Designer** | Balanceamento de dificuldade, mecânicas |
| **Asset Designer** | Sprites, backgrounds, animações, pipeline Blender |
