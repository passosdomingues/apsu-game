# Sprint 17 — Viewport Culling e Desempenho em Tempo Real (APSU-P17)

**Período:** Pós-Sprint 16 (Release v20261125-213629)  
**Meta:** Eliminar lentidão progressiva ao avançar de fase e garantir renderização estritamente em tempo real do que está na tela/viewport do jogador.

---

## Diagnóstico do Problema ("fica cada vez mais lento à medida que passa as fases")

1. **Acúmulo de nós 3D no Scene Graph:** `Runtime3DLayer` instanciava malhas 3D para cenários, inimigos, projéteis e guardiões de cada fase e as adicionava aos grupos `scenery` e `actors`. Ao mudar de fase, esses nós ficavam apenas com `visible=false`, sem nunca serem removidos do Scene Graph do JavaFX. Pela Fase 4 e 5, dezenas de MeshViews acumulados consumiam ciclos de sincronização e dirty tree traversal a cada frame.
2. **Tabuleta Sagrada 3D sem viewport culling:** `Runtime3DLayer` atualizava e posicionava a malha 3D da tabuleta mesmo quando ela estava a milhares de pixels de distância no mundo (ex: x=2750 com câmera em x=0).
3. **Física e disparo de IA sem culling:** `GameContext.updateEnemies()` calculava IA de mira e disparo para todos os inimigos da fase, mesmo os que estavam a mais de 2500px fora da tela.
4. **Partículas sem culling:** `ParticleSystem.render()` desenhava todas as partículas ativas do pool mesmo quando já haviam saido do enquadramento da tela.
5. **Projéteis 2D sem culling:** `RenderEngine.drawProjectiles()` desenhava todos os projéteis sem verificar limites de tela.
6. **Onda de Eco e Destaques sem culling:** `EchoWaveRenderer` executava chamadas de traçado para todos os guardiões, inimigos e cenários atingidos pelo eco mesmo quando estavam fora da viewport.
7. **Detecção de colisão de cenário sem culling espacial:** `checkSceneryCollisions()` iterava todos os obstáculos do mundo todo frame.

---

## Histórias Implementadas

### APSU-P17A | Purga de Scene Graph Entre Fases (`Runtime3DLayer.onPhaseTransition`)
- Nós 3D transitórios (cenários, inimigos, projéteis e guardiões) são removidos dos grupos `scenery` e `actors` na troca de fase.
- Apenas os nós persistentes do herói são preservados.
- Testado em `Runtime3DLayerTest.phaseTransitionPurgesTransientActors`.

### APSU-P17B | Viewport Culling em Tempo Real (Render & Simulação)
- **Partículas:** `ParticleSystem.render()` agora descarta partículas fora da tela (`x < -16 || x > 1382 || y < -16 || y > 784`).
- **Projéteis:** `RenderEngine.drawProjectiles()` descarta projéteis fora de tela.
- **Tabuleta Sagrada (2D e 3D):** Ambos os renderizadores agora usam guardas de viewport antes de processar.
- **Portal de descida:** `drawPhaseExit` ignora chamadas com `worldX` fora da tela.
- **Guardião 2D:** `drawGuardian` com guard de visibilidade.
- **Onda de Eco:** `EchoWaveRenderer.renderHighlights()` e `renderWaveRing()` ignoram elementos fora da tela.
- **Inimigos:** `GameContext.updateEnemies()` pula cálculos de IA de tiro para inimigos além de 900px da tela.
- **Cenário:** `checkSceneryCollisions()` filtra por vizinhança imediata do herói (±200px).
- **Limpeza de fase:** `particleSystem.clear()` e `echoWaveRenderer.reset()` adicionados ao início de cada fase (`startP1` .. `startP5`).

---

### APSU-P17C | Correção do Herói do Menu Principal e Zero-Alloc no Minimap
- **Remoção de `menu-hero` fora do menu:** Adicionado descarte e ocultação explícita do nó 3D do herói do menu (`menu-hero`) assim que o jogo entra em qualquer fase (`state != MENU`), eliminando o artefato de modelo duplicado suspenso na direita da tela.
- **Isolamento de Herói em `onPhaseTransition`:** Purga preserva estritamente `hero-swim` e `hero-attack`, garantindo que nós do menu sejam limpos.
- **Zero-Alloc no Minimap (`UIRenderer`):** Substituído `new double[]` por buffers reutilizáveis (`minimapHeroX`, `minimapHeroY`), eliminando a última alocação por frame do HUD.
- **Desativação de Projéteis 3D no Modo Híbrido:** `Runtime3DLayer` agora respeita `secondaryElementsDisabled` também para projéteis 3D (`bolha_3d` e `bola_de_lava_3d`).
- **Otimizações de Cáusticas e Bolhas (`LightingEngine`, `RenderEngine`):** Cáusticas fora da viewport são descartadas antes de chamar `fillPolygon`; bolhas da Fase 3 reduzidas pela metade no modo econômico (`reducedEffects`).

---

## Testes e Validação

- Testes: **212/212 testes verdes** (incluindo `ViewportCullingTest` e `Runtime3DLayerTest`).
- Build: `BUILD SUCCESS` (0 failures, 0 errors).
- Screenshots atualizados em `.claude/capturas/inspecao/`.
