# Sprint 14 — Mecânica de Ecolocalização, Tabuleta Oculta e Camadas Visuais

**Período:** Pós-Sprint 13 (Release v20261125)  
**Meta:** Implementar a mecânica de revelação da Tabuleta Sagrada via Ecolocalização baseada nas pistas dos Enkis, integrar a nova `tabuleta_2.blend`, elevar a UI/Diálogos para o topo da pilha de camadas (acima da SubScene 3D) e harmonizar os fundos e o balanceamento do modo Difícil.

---

## 1. Diagnóstico das Capturas Reais de Gameplay (.claude/capturas/inspecao/)

A inspeção visual automatizada revelou os pontos exatos que justificam as demandas:
1. **Oclusão 3D na Caixa de Diálogo:** A `SubScene 3D` fica posicionada acima do Canvas 2D. Modelos 3D de Enki, corais do chão e o boss Kullullû são desenhados por cima do texto e das bordas da caixa de diálogo.
2. **Tabuleta Entregue de Bandeja:** Enki flutua com a tabuleta já visível ao seu lado e a entrega imediatamente ao pressionar 'E', sem desafio ou uso da ecolocalização.
3. **Nomenclatura "RADAR":** O minimap superior direito ainda ostenta o título "RADAR" em vez de "ECOLOCALIZAÇÃO".
4. **Sobreposição de HUD e Alertas:** O número `0` sobrepõe a palavra `TABULETAS` no HUD superior esquerdo, e o alerta de fase continua renderizado por baixo da caixa de diálogo quando ela se abre.
5. **Cenário de Fundo das Fases 4 e 5:** A Fase 4 usa fundos genéricos com um relevo vulcânico desconexo flutuando na cena.

---

## 2. Histórias Prioritárias da Sprint 14

### APSU-R02 | Camada de UI e Diálogo no Topo Absoluto (Canvas Superior) | 5 pts | Marcos [x]
- **Contexto:** A SubScene 3D fica sobre o Canvas de desenho, fazendo com que atores 3D (Enki, corais, boss) sobreponham a caixa de diálogo e o HUD.
- **Entregue em:** commit em andamento (APSU-R02)
- **Critérios de aceite:**
  - [x] AC1: Criação de um segundo `Canvas` transparente (`foregroundCanvas`) posicionado acima da `SubScene 3D` no `StackPane` raiz de `ApsuGameMain`.
  - [x] AC2: Diálogos (`drawDialogueOverlay`), alertas (`drawAlert`) e HUD renderizados exclusivamente no Canvas superior, ficando 100% à frente de qualquer elemento 3D ou 2D de cenário.
  - [x] AC3: Alertas de fase ocultados enquanto uma caixa de diálogo estiver aberta.
  - [x] AC4: Ajuste do alinhamento do texto `TABULETAS` e contadores no HUD superior para eliminar sobreposição de caracteres.
  - [x] AC5: `mvn test` 100% verde (201/201 testes) e zero alocação no hotpath.
- **Tarefas:**
  - [x] Implementar pilha de nós `StackPane(backgroundCanvas, subScene3D, uiCanvas)` em `ApsuGameMain` (1,5 h)
  - [x] Roteamento de renderização de HUD/Diálogo/Alertas para o canvas superior em `RenderEngine` (2 h)
  - [x] Corrigir espaçamento de métricas no `UIRenderer.drawHUD` (0,5 h)
  - [x] Validar captura visual sem oclusão de texto (1 h)

### APSU-G01 | Tabuleta Sagrada Oculta e Revelação por Ecolocalização | 5 pts | Rafa / Marcos [x]
- **Contexto:** A tabuleta sagrada não deve ser entregue pelo Enki. O Enki deve passar a pista geográfica/mitológica no diálogo, a tabuleta fica invisível no mapa e só se revela quando a onda da tecla Q (Ecolocalização) a atinge.
- **Entregue em:** commit `35cb4cf`
- **Critérios de aceite:**
  - [x] AC1: Enki não entrega a tabuleta ao pressionar 'E'. Seu diálogo é enriquecido com a pista geográfica de onde o artefato repousa na respectiva fase.
  - [x] AC2: Criação de entidade `SacredTabletEntity` com estados `HIDDEN` (invisível, sem colisão), `REVEALED` (visível com glow pulsante e aura mística) e `COLLECTED`.
  - [x] AC3: A frente de onda de `EchoWaveRenderer` (ao acionar Q) detecta a posição da tabuleta oculta; ao atingi-la, dispara som de revelação mística, partículas e transiciona o estado para `REVEALED`.
  - [x] AC4: A coleta ocorre quando o herói nada até a tabuleta revelada, abrindo o portal de saída da fase.
  - [x] AC5: Testes de unidade em `SacredTabletTest` cobrindo o fluxo completo.
- **Tarefas:**
  - [x] Criar `SacredTabletEntity` com estados e coordenadas por fase (P1 a P4) (2 h)
  - [x] Atualizar falas de Enki com as pistas geográficas autênticas (1 h)
  - [x] Integrar colisão da onda do sonar com a tabuleta em `EchoWaveRenderer` (1,5 h)
  - [x] Implementar coleta física pelo herói e abertura do portal (1,5 h)
  - [x] Escrever testes automatizados de unidade e integração (1 h)

### APSU-A03 | Pipeline e Integração da Nova Tabuleta 2 (3D) | 3 pts | Wagner / Marcos [x]
- **Contexto:** Novo modelo 3D `tools/personagens/blends/12_elementos/tabuleta_2.blend` disponível para ser a malha oficial da tabuleta oculta revelada.
- **Entregue em:** commit APSU-A03
- **Critérios de aceite:**
  - [x] AC1: Modelo `tabuleta_2.blend` exportado para OBJ/MTL em `src/main/resources/models/scenery/tabuleta_sagrada_2_3d/`.
  - [x] AC2: Carregamento no `ObjModelLoader` com material e texturas adequados.
  - [x] AC3: Renderização da malha 3D flutuando no ponto de revelação da fase via `Runtime3DLayer`.
- **Tarefas:**
  - [x] Exportar `tabuleta_2.blend` via script Blender headless para OBJ/MTL (1 h)
  - [x] Ajustar escalas, pivôs e MTL para o padrão do jogo (1 h)
  - [x] Conectar o nó 3D na `Runtime3DLayer` vinculado à entidade da tabuleta (1 h)

### APSU-U03 | Renomeação para Ecolocalização e Tutorial Orgânico | 3 pts | Rafa [x]
- **Contexto:** O minimapa utiliza o termo anacrônico "RADAR", e o tutorial textual precisa de dicas orgânicas sobre como e quando usar a Ecolocalização.
- **Entregue em:** commit APSU-U03
- **Critérios de aceite:**
  - [x] AC1: Título "RADAR" alterado para "ECOLOCALIZAÇÃO" no minimap do `UIRenderer`.
  - [x] AC2: `TutorialStep` e dicas contextuais atualizados para referenciar ecolocalização e tecla `[Q]`.
  - [x] AC3: Dica visual elegante e pulsante da tecla Q sugerida quando o jogador estiver próximo à área indicada pela pista do Enki.
- **Tarefas:**
  - [x] Atualizar renderização do minimap em `UIRenderer.java` (0,5 h)
  - [x] Revisar referências em `TutorialStep`, `EchoController` e testes (1 h)
  - [x] Implementar sinalização sutil de recarga/prontidão do sonar (1 h)

### APSU-B02 | Depuração do Fundo Vulcânico e Calibração de Dano Difícil | 3 pts | Marcos [x]
- **Contexto:** O relevo de fundo da Fase 4 apresenta colunas/vulcão desconexo, e o combate no modo Difícil precisa de maior tensão tática reduzindo o dano excessivo das bolhas.
- **Entregue em:** commit APSU-B02
- **Critérios de aceite:**
  - [x] AC1: Background da Fase 4 harmonizado, com tons profundos de magma e abismo, removendo a rocha flutuante esquisita.
  - [x] AC2: Dano das bolhas básicas do herói no modo Difícil calibrado para 0.8 (redução de 20%), prolongando o clímax da luta com Kullullû.
  - [x] AC3: `DifficultyTest` e `BossEntityTest` atualizados e verdes.
- **Tarefas:**
  - [x] Revisar assets e gradientes de fundo da Fase 4 em `RenderEngine` (1 h)
  - [x] Calibrar dano de bolha em `Difficulty` e `Projectile` (1 h)
  - [x] Validar testes de combate (0,5 h)

### APSU-P02 | Sprint Gate e Verificação Visual Automatizada | 2 pts | Marcos [x]
- **Contexto:** Garantir 100% de testes verdes, re-executar a ferramenta de captura visual para certificar a eliminação de todas as oclusões e rodar `perf_gate.py`.
- **Entregue em:** commit APSU-P02
- **Critérios de aceite:**
  - [x] AC1: `mvn test` 100% aprovado — **208/208 testes** verdes.
  - [x] AC2: Suíte de testes cobre fluxo completo da tabuleta revelada (SacredTabletTest, ObjModelLoaderTest, UIRendererTest, GuardianEntityTest).
  - [x] AC3: `perf_gate.py` **APROVADO** em todas as 5 fases — 60 FPS estável, p95 ≤ 17 ms, >33ms/min = 0.
- **Tarefas:**
  - [x] Executar suíte de testes completa (0,5 h)
  - [x] Validar comportamento de tabuleta, diálogo e minimap nos testes (0,5 h)
  - [x] Rodar `perf_gate.py` contra todos os 5 baselines e consolidar métricas (1 h)
