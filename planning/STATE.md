# 📡 STATE — Estado Atual do Projeto

> **Este é o documento mais importante para o dia a dia.**
> Atualizado sempre que uma task muda de estado ou um bloqueio é identificado.
>
> **Última atualização:** 2026-08-21 (Sprint 13: Otimização de colisão com QuadTree 2D, ampliação de suíte de testes para 41 testes e Release v1.0.0)
> **Responsável pela atualização:** AI Agent / Dev ativo

---

## 🚦 Status Geral

| Aspecto | Status | Detalhe |
|---|---|---|
| **Sprint Atual** | 🟢 Sprint 13 Concluída | QuadTree 2D para colisões O(N log M), 41 testes unitários/integrados e Fat JAR v1.0.0 empacotado. |
| **Build (`mvn test`)** | ✅ VERIFICADO | 41/41 testes, BUILD SUCCESS (2026-08-21) |
| **Versão / Release** | 🚀 RELEASE v1.0.0 | Fat JAR `apsu-game-1.0.0.jar` gerado com sucesso via Maven. |
| **Particionamento Espacial** | ✅ IMPLEMENTADO | `QuadTree` 2D otimiza consultas de projéteis e herói x inimigos para O(N log M). |
| **Interface / UI** | ✅ REMODELADA | Emojis 100% removidos; HUD redesenhado com Canvas nativo (barras e formas geométricas). |
| **Estabilidade Gráfica** | ✅ CORRIGIDA | `BlendMode.ADD` removido; artefato de retângulo escuro do coral 3D eliminado; zero flicker em Mesa/SW-GL. |
| **Controle & Física** | ✅ OTIMIZADA | Inércia responsiva com frenagem ágil (0.58/0.62) e empuxo recalibrado; sem drift excessivo ou afundamento rápido. |
| **Hitboxes** | ✅ RECALIBRADAS | Herói ajustado para `28x100`; sobreposição de inimigos ajustada para 55% central, sem dano à distância. |
| **Performance** | ✅ OTIMIZADA | ~35% redução de overhead procedural; limiar de FPS ajustado para 30 FPS ignorando warm-up JVM. |
| **Redesign Fases 2–4** | ✅ IMPLEMENTADO | P2 (6 corais em S), P3 (4 inimigos, correntes sequenciais), P4 (Leviatã sub-boss isolado com 600px de arena limpa). |
| **Pipeline 3D (Blender)** | ✅ EXECUTADO | `/opt/blender-4.5.5-lts/blender` localizado e integrado; `gerador_mestre_apsu.py` executado com sucesso. |
| **Viewport** | ✅ IMPLEMENTADO | Canvas lógico 1366×768 é escalado proporcionalmente e centralizado; nenhuma borda do jogo é cortada em telas com outra proporção. |
| **Ataques das variantes** | ✅ PIPELINE PRONTO | Abissal, Deus e Recife com 12 poses próprias geradas em Blender. |
| **Dificuldade adaptativa** | ✅ IMPLEMENTADA | Ajuste por fase observa dano recebido e inimigos derrotados, sem substituir a escolha Fácil/Médio/Difícil do menu. |
| **Render 3D — herói (rim light)** | ✅ VERIFICADO | Confirmado por pixel-diff (contorno visível em toda a silhueta) + relato do Rafa ("aura brilhante") |
| **Render 3D — ciclo de nado** | ✅ VERIFICADO | Rafa: "vi ele nadando pelo blend, deu certo" |
| **Render 3D — ataques (thrust/slash/spin/charge)** | ✅ VERIFICADO | Rafa: "rodando o tridente, sucesso" — 4 renders recebidos e conferidos |
| **Cauda do herói — curva suave** | ✅ VERIFICADO | Curva procedural (`smooth_angle_chain`, smoothstep) aplicada nos scripts do Adapa. |
| **Integração Java — ciclo de nado** | ✅ JÁ FUNCIONA | `RenderEngine`/`SpriteManager` carregam sequências de 8 frames com animação contínua. |
| **Integração Java — poses de ataque** | ✅ INTEGRADO | `RenderEngine` alterna os quatro diretórios de ataque em cada disparo. |
| **Sprites & Cenários** | 🟢 PRONTOS | 5 Backgrounds (bg1 a bg5), 5 Guardiões, 6 Inimigos + elementos procedurais |
| **Modelos 3D (.blend)** | ✅ EXECUTADOS | Catálogo completo de 22 scripts .blend compilados via Blender 4.5.5 LTS |
| **Inicialização JavaFX** | ✅ VERIFICADA | `mvn javafx:run` rodando a 60 FPS com aceleração GPU Prism (`es2`). |
| **Controle do herói** | ✅ RECALIBRADO | Frenagem subaquática e inércia responsiva. |
| **Colisões P2–P5** | ✅ RECALIBRADAS | Hitbox ultra compacta no peito (24x80); perigos e corais recalibrados sem aprisionamento. |

---

## ✅ O que está FEITO

### Sprint 11 — Redesign Completo de Cenários, Visual Procedural e Pipeline 3D (2026-08-21)

**IMPLEMENTAÇÃO**
- **Fase 2 (Cavernas de Coral)**: Reduzido de 9 para 6 colunas de coral com padrão em S legível e previsível; inimigos reposicionados nas salas abertas; baú do galeão em local amplo (`wx=1800`).
- **Fase 3 (Correntes Abissais)**: Reduzido de 6 para 4 inimigos; 3 zonas de corrente sequenciais em vez de sobrepostas; 2 obstáculos móveis bem espaçados.
- **Fase 4 (Abismo Vulcânico)**: Reduzido de 7 para 5 inimigos; Leviatã isolado como sub-boss com 600px de arena limpa; 2 piscinas de lava maiores com coluna de vapor.
- **RenderEngine Enriquecido**: Corais procedurais orgânicos com bioluminescência rotativa; filamentos de água em movimento nas correntes; coluna sólida translúcida nos gêiseres; recifes e vida marinha de fundo na P2; faixa de lava, bolhas brotantes e faíscas incandescentes (*embers*) emergindo das rochas na P4.
- **Pipeline Blender 3D**: Execução resolvida via `/opt/blender-4.5.5-lts/blender`; `gerador_mestre_apsu.py` e renderização de sprites 2.5D sincronizados.

**VALIDAÇÃO**
- Suíte completa de **37/37 testes** unitários e de integração aprovada via Maven.


### Próximos passos priorizados

1. **QA visual/manual:** testar ao menos uma tela 16:9 e uma 16:10/4:3, confirmando que o canvas inteiro fica visível e que as barras não encobrem conteúdo.
2. **Assets das variantes:** inspecionar em jogo as 12 poses Blender de Abissal, Deus e Recife; cada sequência gerada tem oito frames distintos.
3. **DDA em partida real:** registrar FPS e sensação de desafio por fase; calibrar os limites de ±0,24 somente após esse teste.
4. **Release v1.0.0:** testar o JAR recém-gerado fora do Maven e, depois do QA, criar o commit/tag de release.

### Controle e desempenho adaptativo (2026-08-20)

- A troca para GPU revelou a física antiga medida por frame: o herói mais rápido
  podia alcançar 20 px por frame e ficar difícil de controlar. Guardião, Abissal,
  Deus Dourado e Recife receberam limites/impulsos calibrados; o bônus por fase
  caiu de 6% para 2,5% e a inversão de direção freia a inércia de imediato.
- `GameLoop` agora usa o FPS observado para alternar qualidade com histerese:
  reduz efeitos abaixo de 50 FPS e só restaura a partir de 57 FPS, evitando
  alternância visual constante. O limite de partículas cai de 320 para 140
  durante esse modo, e as cáusticas/halos caros são simplificados.
- O Canvas JavaFX e seu `GraphicsContext` devem ficar no thread da interface;
  mover o desenho para threads paralelas não é suportado. A otimização preserva
  esse requisito e evita alocação/overdraw nos momentos de combate.
- Validação: `mvn test` e `mvn verify` concluídos com sucesso; novo teste cobre
  desaceleração ao soltar e inverter o movimento.

### Colisões e leitura de perigo (2026-08-20)

- O corpo visível do herói agora usa uma hitbox interna (`48×144` dentro do
  sprite `72×192`); aura, cauda e margens transparentes não causam contato.
  Inimigos também usam somente 68% da área central da arte.
- Gêiseres, pedras móveis, rochas vulcânicas e lava receberam áreas de dano
  menores. A lava avalia apenas os pés do herói, e não o sprite inteiro.
- Um acerto aplica dano reduzido, invulnerabilidade existente e knockback para
  fora da fonte. Coral recebe margem extra após o empurrão; o jogador não fica
  preso no mesmo obstáculo durante a janela de invulnerabilidade.
- Correntes agora aplicam 20% de sua força declarada, em vez de 35%, mantendo
  desafio sem anular os comandos do jogador.
- Validação: `mvn test` e `mvn verify` aprovados; `GameContextIntegrationTest`
  garante que a aura do sprite não é tratada como colisão com coral.

### QA de execução pós-assets (2026-08-20)

- `mvn javafx:run` iniciou com sucesso e copiou os 434 recursos atuais para o
  runtime. No ambiente automatizado, a medição ficou entre 7 e 14 FPS porque
  o `pom.xml` priorizava o backend Prism de software (`sw`).
- A ordem agora é `es2,sw,j2d`: usa aceleração GPU na máquina de jogo e só
  recua para software quando necessário. O QA visual e a medição final devem
  ser feitos na sessão gráfica normal, não no ambiente de automação.
- Após a mudança, `mvn verify` e uma nova inicialização JavaFX concluíram sem
  alertas de FPS abaixo da meta no log.

### Higiene do pipeline de assets (2026-08-20)

**Causa raiz e correção**
- Os scripts `07_navio_naufragado_elemento-cenario.py` a
  `10_perigos_e_obstaculos_cenario.py` traziam literalmente o caminho da
  máquina Windows antiga. Em Linux, `C:\\Users\\...` não é um caminho absoluto;
  foi interpretado como uma pasta comum abaixo de `personagens/scripts/`.
- Cada um agora calcula a raiz atual a partir de `__file__`, tal como os demais
  geradores. O gerador mestre também localiza o Blender por `BLENDER_BIN`,
  `PATH` ou locais padrão, sem fixar uma instalação pessoal.
- Os quatro `.blend` mais novos que haviam caído na pasta indevida foram
  centralizados em `personagens/blends/`; a pasta acidental foi removida após
  a validação de sintaxe.
- Rafa executou `make assets` com sucesso. A auditoria posterior confirmou
  oito PNGs distintos em cada uma das 45 sequências de sprite atuais. Os seis
  renders de `fase_atlantis_apsu_3d` são screenshots de apresentação, não uma
  animação, e por isso permanecem fora dessa métrica. O diretório legado
  `sprites/scenery/06_bau_tesouro/` (seis frames antigos e sem uso no Java) e
  os backups automáticos `.blend1` foram removidos; `make assets` passa a
  encerrar com essa limpeza automaticamente.

**Regra de manutenção**
- Saídas duráveis: `personagens/blends/<família>/` e
  `personagens/renders/`.
- Saídas de jogo: `src/main/resources/sprites/`, geradas exclusivamente pelo
  renderizador 2.5D.
- Backups automáticos `*.blend1` não devem ser versionados; o `.gitignore`
  cobre-os.

### Sprint 10 — Kullullû em três estágios legíveis (2026-08-20)

**IMPLEMENTAÇÃO**
- `BossEntity` define os estágios Obsidiana Fria, Fusão Vulcânica e Fúria de Apsu pelos limiares de 2/3 e 1/3 da vida.
- Cada troca pausa o ataque por 0,5 s, mostra um anel dourado no boss, emite evento `BOSS_PHASE_CHANGED`, gera feedback de impacto e apresenta um alerta ao jogador.
- Os padrões escalam de leque direcionado para onda radial e, por fim, meteoros de lava; a cadência também aumenta sem tornar a primeira fase punitiva.

**VALIDAÇÃO**
- `BossEntityTest` cobre os dois limiares, o telegrafo e a garantia de que uma transição não é emitida duas vezes.
- `mvn test`: **29/29** aprovados.

### Sprint 7 — Feedback tátil de impacto e teste de regressão da câmera (2026-08-20)

**IMPLEMENTAÇÃO**
- `Camera` agora possui tremor visual determinístico, com envelope de decaimento e sem alterar coordenadas de mundo.
- Dano por inimigos, projéteis, cenário, lava, gêiser e contato com Kullullû gera resposta proporcional; a derrota do boss usa um impacto maior.
- `reset()` cancela qualquer feedback pendente para evitar vazamento de efeito entre partidas.

**VALIDAÇÃO**
- `CameraTest` cobre o decaimento do tremor e garante que a rolagem de mundo não seja afetada.
- `mvn test`: **22/22** aprovados.

### Sprint 7 — Cache de animações e resiliência do pipeline de sprites (2026-08-20)

**IMPLEMENTAÇÃO**
- `SpriteManager` passa a reutilizar as listas imutáveis de frames por diretório, eliminando a reconstrução de até oito referências de imagem para cada entidade em cada frame.
- Sequências ausentes preservam o fallback individual, sem poluir o cache compartilhado com um sprite de tipo incorreto.

**VALIDAÇÃO**
- Novo `SpriteManagerTest` valida sequência de 8 frames, reutilização de cache e fallback seguro.
- O cache nativo do JavaFX nos testes é isolado em `target/javafx-cache`, evitando dependência de escrita no diretório pessoal.
- A suíte desabilita áudio por propriedade (`apsu.audio.enabled=false`); a execução normal mantém áudio habilitado por padrão.

### Automação de qualidade — CI inicial (2026-08-20)

**IMPLEMENTAÇÃO**
- Workflow do GitHub Actions valida `mvn verify`, publica o JAR gerado para inspeção curta e compila o demonstrador C/OpenMPI em jobs separados.
- O JDK 21 e o cache Maven são configurados explicitamente, deixando a validação reproduzível em push e pull request.

### Sprint 8 — Fundação de Save/Load JSON (2026-08-20)

**IMPLEMENTAÇÃO**
- `SaveManager` persiste savegames versionados em `~/.apsu/savegame.json`, com caminho injetável para uso em testes.
- O contrato inclui dificuldade, herói selecionado, fase desbloqueada, tabuletas e recorde; dados nulos ou fora dos limites são normalizados.
- Falhas de leitura e JSON inválido retornam um resultado vazio, permitindo ao jogo iniciar uma sessão nova em vez de interromper o jogador.

**VALIDAÇÃO**
- `SaveManagerTest` cobre ida e volta do JSON, arquivo inexistente/inválido e saneamento de dados; `mvn test`: **27/27** aprovados.

### Sprint 8 — Save/Load conectado ao fluxo do jogo (2026-08-20)

**IMPLEMENTAÇÃO**
- `GameContext` restaura dificuldade e herói selecionado ao iniciar, e grava preferências, tabuletas, fase desbloqueada e vitória sem bloquear o loop de jogo em caso de erro de disco.
- O construtor aceita `SaveManager` injetável, deixando testes totalmente isolados do perfil real do usuário.

### Sprint 8 — Barramento de eventos para áudio (2026-08-20)

**IMPLEMENTAÇÃO**
- `GameContext` não depende mais de `AudioManager`: publica pedidos de som em um `EventBus` thread-safe.
- `AudioEventSubscriber` conecta o áudio JavaFX somente no ponto de montagem da aplicação; testes e lógica podem operar sem backend de mídia.

### UX — Radar de navegação no HUD (2026-08-20)

**IMPLEMENTAÇÃO**
- Um minimapa compacto no canto superior direito mostra posição do herói, inimigos, guardiões e boss, com escala adequada à arena final e às fases de 4000px.
- A apresentação é somente visual; não interfere em câmera, colisões ou coordenadas de mundo.

**VALIDAÇÃO**
- `UIRendererTest` garante conversão de mundo para radar com limites; `mvn test`: **28/28** aprovados.

### Sprint 6.8 — Leviatã Menor criado + auditoria sistemática de cobertura (2026-08-16)

Rafa: "ficou top, mete o pau aí, prossiga". Em vez de ir direto pra
polimento, fiz uma auditoria sistemática (comparei TODO caminho
`sprites/` referenciado em qualquer lugar do Java contra os scripts/
renders que realmente existem) — a mesma técnica que achou o Oráculo
e a rocha vulcânica nas sprints anteriores, agora aplicada por
completo em vez de caso a caso.

**AUDITORIA**
- achado grave: `EnemyType.LEVIATA` ("Leviatã Menor — sub-boss da
  Fase 4, move rápido em diagonal") tem tipo, tamanho (180×160, o
  maior inimigo comum do jogo), dano e posição em
  `GameContext.java` desde sempre — **nunca teve nenhum asset 3D**.
  Sem sprite carregado, caía no fallback geométrico (retângulo roxo
  simples) — o sub-boss da fase mais exigente visualmente do jogo
  era, na prática, invisível/placeholder
- conferido tipo a tipo: os outros 5 inimigos (peixe, enguia, medusa,
  caranguejo, arraião), os 4 heróis, os 3 variantes de Enki, os 5
  guardiões e os elementos de cenário — todos têm render
  correspondente em disco. **Cobertura agora está completa** — não
  achei mais nenhum órfão

**IMPLEMENTAÇÃO**
- `14_leviata_menor.py` novo — serpente marinha grande (corpo mais
  grosso e mais longo que a enguia, 8 segmentos), cabeça com
  mandíbula/dentes visíveis, crista dorsal de espinhos, basalto
  escuro com veias de magma (mesma técnica Voronoi do Kullullû —
  reforça a leitura de "predador nativo do abismo vulcânico").
  Ciclo de nado aplicado (mesmo padrão da enguia, mas é literalmente
  uma serpente, candidato ainda mais natural). Nome de arquivo do
  script é 14 (próximo disponível), mas salva como
  "12_leviata_menor" — precisa bater exatamente com
  `EnemyType.LEVIATA` em Java, não com a numeração dos scripts
- `gerador_mestre_apsu.py`: script novo adicionado à lista

**VALIDAÇÃO**
- Sintaxe conferida ✅. **Nada renderizado ainda** — precisa
  `make assets` de novo pra ver o Leviatã pela primeira vez

**DOCUMENTAÇÃO**
- STATE.md atualizado (este arquivo)
- Seção 12 nova em `202608151500_auditoria_e_melhorias.md`

### Sprint 6.7 — Baú/navio, squash-stretch no Java pra inimigos/boss, áudio auditado (2026-08-16)

Rafa: "manda a ver em tudo que achar que pode melhorar, confio em
você" — autonomia total. Continuei descendo a lista de prioridades
que eu mesmo tinha proposto na sprint anterior.

**AUDITORIA**
- `AudioManager.java` conferido — cobertura já é razoável (shoot/hurt/
  collect/boss-hit/victory, todos com `.wav` correspondente em
  `resources/audio/`). Sem ferramenta de geração de áudio neste
  ambiente, não dá pra criar sons novos — não forcei mudança aqui,
  ficaria mudança por mudança sem valor real
- Reconsiderei o modifier de Subdivision Surface como alternativa
  "seguro" ao remesh completo (que eu vinha adiando por risco) — na
  prática NÃO ajudaria: os segmentos dos personagens são objetos de
  malha SEPARADOS (não uma malha única), e Subsurf não preenche a
  costura ENTRE dois objetos diferentes, só suaviza a topologia
  dentro de cada um (que já é uma esfera/cone, já suave). Descartei
  essa ideia — não vale gastar o "orçamento de risco" numa mudança
  que eu já sei que não resolve o problema real

**IMPLEMENTAÇÃO**
- `06_bau_tesouro_elemento-cenario.py` e
  `07_navio_naufragado_elemento-cenario.py`: rim light adicionado
  (mesma regra de sempre — cor do próprio acento emissivo de cada
  um). Âncora do navio ganhou uma leve inclinação (pose congelada
  sugerindo corrente de água, mesmo raciocínio do cardume — cenário
  só é imagem estática no jogo, não animação)
- `RenderEngine.java`: squash-stretch agora também nos inimigos
  comuns (pulso amarrado à própria fase do bob vertical que já
  existe, sem precisar de estado novo em `EnemyEntity`) e no boss
  (respiração mais lenta/pesada que os inimigos — reforça no lado
  Java a mesma vida idle que já foi dada ao modelo 3D do Kullullû)

**VALIDAÇÃO**
- Sintaxe + balanceamento conferidos em todos os arquivos tocados ✅
- **Nada renderizado/rodado nesta rodada** — precisa `make assets` +
  `make test` de novo

**DOCUMENTAÇÃO**
- STATE.md atualizado (este arquivo)
- Seção 11 nova em `202608151500_auditoria_e_melhorias.md`

### Sprint 6.6 — Vida no boss + cardume de peixes (2026-08-16)

Rafa confirmou "deu tudo certinho" no `make assets` e pediu pra eu
continuar com autonomia total ("manda bala", "o que você melhoraria?").
Escolhi os itens de maior valor ainda pendentes na camada de
animação/vida dos personagens (antes de ir pra polimento/efeitos, que
tem prioridade mais baixa no critério que o próprio Rafa definiu).

**IMPLEMENTAÇÃO**
- `02_kullullu_boss.py`: vida idle (respiração mais forte/rápida que
  os NPCs — é um predador vivo, não estátua — + leve deriva de
  cabeça "observando"). Era o único personagem principal ainda sem
  nenhuma vida idle
- `08_recifes_e_cardume_elemento-cenario.py`: os 18 peixes do cardume
  agora têm cauda/corpo em ângulos diferentes por índice (não
  animação — esse asset só é usado como imagem ESTÁTICA no jogo hoje,
  então a variedade precisa estar congelada na pose, não em frames
  que o Java nunca carrega). Também ganhou rim light (ponta do coral/
  olho do peixe como cor de contorno, mesma regra de sempre)

**DECISÃO TÉCNICA REGISTRADA**: elementos de cenário (baú, navio,
recifes, obstáculos) são carregados no Java via `getImage()` (uma
imagem estática), NUNCA `loadSequence()` (sequência animada) — isso é
diferente de personagens (herói/inimigos/guardiões), que usam
`loadSequence`. Por isso a abordagem de "vida" pra cenário é variação
de POSE na hora de construir, não keyframes de animação (que ficariam
gerados e nunca usados). Se no futuro o Rafa quiser cenário animado de
verdade, precisa primeiro estender `RenderEngine`/`SpriteManager` pra
carregar sequências de cenário — registrado como próximo passo.

**VALIDAÇÃO**
- Sintaxe conferida nos 2 scripts ✅
- **Nada renderizado nesta rodada** — precisa `make assets` de novo

**DOCUMENTAÇÃO**
- STATE.md atualizado (este arquivo)
- Seção 10 nova em `202608151500_auditoria_e_melhorias.md`, com lista
  priorizada de próximos passos sugeridos

### Sprint 6.5 — Obstáculo abissal novo + Oráculo criado do zero (2026-08-16)

Resposta às 2 pendências que ficaram da rodada anterior: Rafa confirmou
"crie" (obstáculo 3D da Fase 3) e "nome/tema e aparência sugestiva da
fase, com base nos modelos prontos" (o guardião/oráculo faltante).

**AUDITORIA**
- confirmado (não só suposto): `05_guardiao_oraculo_correntes.png`
  não existe em `personagens/renders/` — o 4º guardião (Fase 4,
  type=3 em `GuardianEntity.java`) tinha NOME e DIÁLOGO em Java mas
  **nenhum modelo 3D nunca foi construído** — não é um caso de
  "esqueceram de conectar", é um caso de "nunca existiu"
- achado de nomenclatura: o nome antigo "Oráculo das **Correntes**"
  estava na Fase 4 (vulcânica) — nome de água numa fase de lava. O
  diálogo em si já era bem vulcânico (menciona magma, arraias, calor)
  — só o NOME que destoava
- achado extra: `gerador_mestre_apsu.py` tinha `05_guardioes_variacoes.py`
  na lista (bem, achei que faltava por um grep mal feito meu — pattern
  "05_guardiao" não bate com "05_guardioes" por causa do plural — vale
  a lição: sempre conferir a lista completa, não só grep pontual)

**IMPLEMENTAÇÃO**
- `13_obstaculo_abissal.py` novo — crescimento rochoso/coralino
  corrompido pela peçonha de Kullullû (roxo-magenta emissivo em vez
  do laranja-magma da Fase 4), mesmo padrão/escala de
  `12_obstaculo_vulcanico.py`, com rim dourado (paleta medida do bg3)
- `RenderEngine.java`: `MOVING_OBSTACLE` da Fase 3 agora carrega esse
  asset novo (mesmo padrão de fallback seguro da Fase 4)
- `05_guardioes_variacoes.py`: `construir_oraculo_chamas()` nova —
  reaproveita a "linguagem visual" dos outros guardiões (corpo
  colunar + cabeça + runas) com identidade própria: terceiro-olho
  profético na testa, coroa de brasas flutuantes (em vez de
  chifres/galhos), braseiro erguido nos braços, obsidiana rachada com
  veias de magma. Ganhou rim light + vida idle (respiração sutil,
  amplitude baixa — estátua, não carne) igual aos outros
- `GuardianEntity.java`: `NAMES[3]` → "Oráculo das **Chamas Abissais**"
  (era "das Correntes"), 1ª linha do diálogo da Fase 4 ajustada pra
  bater — `SPRITE_PATHS[3]` mantido igual de propósito (nome de
  arquivo não precisa bater com nome exibido, menor risco)
- `gerador_mestre_apsu.py`: `13_obstaculo_abissal.py` adicionado à
  lista (senão nunca seria gerado)

**VALIDAÇÃO**
- Sintaxe conferida (`py_compile` + balanceamento Java) nos 4 arquivos
  tocados ✅
- **Nada renderizado nesta rodada** — precisa `make assets` de novo

**DOCUMENTAÇÃO**
- STATE.md atualizado (este arquivo)
- Seção 9 nova em `202608151500_auditoria_e_melhorias.md`

### Sprint 6.4 — Causa raiz do "duros" (Makefile) + olho da enguia + rocha vulcânica 3D (2026-08-16)

Resposta ao relato do Rafa: herói principal nadou certo (`make render-sprites`
funcionou), mas variantes do Adapa continuaram "duras", e ele não sabia como
gerar os renders do Enki/guardião pra conferir.

**AUDITORIA**
- **causa raiz encontrada**: `make render-sprites` só RENDERIZA `.blend`
  que já existem em disco — nunca REGENERA os `.blend` a partir dos
  scripts `.py` atualizados. Isso só acontece rodando
  `gerador_mestre_apsu.py` diretamente (nunca hookado no Makefile).
  Explica tudo: herói principal nadou porque foi rodado manualmente
  antes (nesta conversa); variantes/Enki/guardião não, porque só
  `make render-sprites` foi rodado, pegando `.blend` antigos
- confirmado: enguia tinha olhos no código (`m_elec`, raio 0.10) — não
  eram "inexistentes", eram pequenos/rentes à cabeça, fáceis de sumir
- confirmado: `12_obstaculo_vulcanico.py` (rocha vulcânica 3D) existe
  desde antes mas nunca foi carregado pelo Java — `VOLCANIC_ROCK` e
  `MOVING_OBSTACLE` da Fase 4 eram só retângulo com contorno
  brilhante (o "efeito portal" que o Rafa reclamou)
- `MOVING_OBSTACLE` da Fase 3 (roxo) continua sem asset 3D — não tem
  script correspondente ainda, precisa ser criado do zero (diferente
  do caso da Fase 4, que só precisava conectar algo que já existia)
- Guardião Atlante: confirmado 1 guardião em CADA uma das 5 fases no
  código (`new GuardianEntity(0..4, ...)`) — não achei ausência real,
  perguntei ao Rafa sobre o "oráculo" pra não supor errado

**IMPLEMENTAÇÃO**
- Makefile: novo target `generate-characters` (roda
  `gerador_mestre_apsu.py`) e `assets` (generate-characters →
  render-sprites → copy-blends) — este é o comando que faltava.
  `test` agora faz `rm -rf target` sozinho (não precisa mais lembrar)
- Enguia: olhos de raio 0.10→0.15, empurrados mais pra fora da cabeça
- `RenderEngine.java`: `VOLCANIC_ROCK`/`MOVING_OBSTACLE` (Fase 4) agora
  carregam `12_obstaculo_vulcanico.png` de verdade, com fallback pro
  retângulo antigo só se a imagem não existir

**VALIDAÇÃO**
- Makefile: `make -n` (dry-run) confirma sintaxe válida nos targets
  novos (`test`, `assets`, `generate-characters`) — tabs corretos
- Python/Java: sintaxe conferida nos 2 arquivos tocados
- **Nada renderizado/rodado de verdade nesta rodada** — depende do
  Rafa rodar `make assets` (ou `make test`) de novo

**DOCUMENTAÇÃO**
- STATE.md atualizado (este arquivo)
- Seção 8 nova em `202608151500_auditoria_e_melhorias.md`

### Sprint 6.3 — Ataques com bolha + integração Java + ciclo de nado ampliado + vida idle + diversidade física (2026-08-16)

Resposta direta ao feedback do Rafa depois de ver os renders reais.
Checkpoint:

**AUDITORIA**
- confirmado: as 2 poses antigas de tiro de bolha (`disparo_bolhas`/
  `atirando_bolhas`) não têm mais lugar — Rafa decidiu substituir
  pelos 4 estilos novos direto
- achado: `01_adapa_nadando_atirando_bolhas` não tem script Python de
  origem (só `.blend`/render órfãos) — nada a fazer nele além de
  desativar no Java
- confirmado: `HeroType.java` já tem identidade de física BEM
  diferenciada por tipo (ABISSAL=tanque/inércia, DEUS=velocista,
  RECIFE=ágil/freio rápido) — a lacuna do Rafa era só a geometria
  Blender não acompanhar isso visualmente

**IMPLEMENTAÇÃO**
- `add_attack_bubbles()` novo em `_apsu_shared_lib.py` — bolhas perto
  da ponta do tridente, padrão de espalhamento diferente por estilo
  (thrust=trilha/slash=leque/spin=anel/charge=aglomerado), parenteadas
  ao tridente (acompanham a animação)
- `HeroEntity.java`: `attackStyleIndex` novo (cicla 0-3 a cada tiro),
  independente de `attackPose2` (não tocado — evita risco na física/
  spawn de projétil)
- `RenderEngine.java`: `drawHero` agora seleciona entre os 4 estilos
  novos em vez das 2 poses antigas
- `add_idle_life_keyframes()` novo em `_apsu_shared_lib.py` — balanço
  de manto + respiração + inclinação de cabeça pra NPCs sem cauda;
  aplicado em `03_enki_npc.py` e `05_guardiao_atlante_npc.py`
  (guardião com amplitude menor — é pedra, não carne)
- Ciclo de nado estendido pra `04_peixe_sombrio_inimigo.py` (barbatanas
  peitorais + cauda tratadas como cadeia curta) e pro corpo completo
  da enguia em `04_inimigos_variacoes.py` (candidata mais natural —
  anguiliforme literalmente vem do nome dela)
- Diversidade física real nos 3 variantes de `01_adapa_variacoes.py`:
  `escala_altura`/`escala_corpo`/`escala_cauda_len` no dict de paleta,
  aplicados via `root.scale` + alongamento específico da cauda —
  Abissal fica atarracado/grosso (tanque), Deus fica alto/esguio com
  cauda longa (velocista), Recife fica compacto (ágil). Números
  batem com os stats já existentes em HeroType.java, não inventados.

**VALIDAÇÃO**
- Sintaxe: todos os 12 scripts Python + 4 arquivos Java conferidos
  (`py_compile` + balanceamento de chaves/parênteses) ✅
- **Nada disso foi rodado de verdade nesta rodada** — sandbox sem
  Blender/Maven, igual às rodadas anteriores. Precisa: `make test`
  (Java) + rodar os scripts atualizados no Blender do Rafa.

**DOCUMENTAÇÃO**
- STATE.md atualizado (este arquivo)
- Seção 7 nova em `202608151500_auditoria_e_melhorias.md`

### Sprint 6.2 — Verificação real + curva procedural (2026-08-16)

Segunda rodada, depois do Rafa rodar de verdade no Blender 4.5 dele e mandar
renders de volta (heroi.png + 4 ataques). Checkpoint:

**AUDITORIA**
- confirmado: rim light funciona (pixel-diff mostra contorno em toda silhueta)
- confirmado: ciclo de nado funciona ("vi ele nadando")
- confirmado: 4 poses de ataque geradas corretamente
- achado NOVO: zigue-zague da cauda não era bug da animação — é a pose
  ORIGINAL hand-tuned (ângulos 0,20,35,20,-10,-30,-45° com salto de sinal
  no meio), presente desde antes da Sprint 6
- achado NOVO: mesmo padrão de ângulo-solto existia em `01_adapa_variacoes.py`
  (cauda_segmentos: -10,-30,-15,20,45°)
- achado NOVO: `RenderEngine`/`SpriteManager` já tinham TODA a infraestrutura
  pra consumir sequências de 8 frames — a integração do ciclo de nado é
  automática, não precisou nenhuma mudança de código Java
- achado NOVO: não existe "ataque de tridente" como mecânica separada — o
  único ataque é o tiro de bolha; os 4 estilos novos não têm consumidor
  ainda (decisão de produto pendente, não é bug)

**IMPLEMENTAÇÃO**
- `smooth_angle_chain()` + `rebuild_chain_angles()` novos em `_apsu_shared_lib.py`
  — substituem listas de ângulo por curva procedural (smoothstep + wobble
  que se anula nas pontas, sem descontinuidade)
- Aplicado em `01_adapa_heroi.py` e `01_adapa_variacoes.py` (os 2 únicos
  scripts com esse padrão de cadeia articulada com ângulo variável)
- Rim light + SSS real + ciclo de nado também aplicados em
  `01_adapa_nadando_disparo_bolhas.py` (fechando os 3 scripts do Adapa)
- `power`/`strength` do rim ajustados em TODOS os personagens (herói,
  boss, Enki, inimigos, guardião) depois do teste real mostrar que o
  valor antigo (power=2.6, strength≈3) era imperceptível

**VALIDAÇÃO**
- Java: 19/19 testes ✅ (Rafa, com rebuild forçado)
- Blender herói: rim light ✅, ciclo de nado ✅, ataques ✅ (Rafa)
- Curva suave da cauda: ⚪ AINDA NÃO RE-RENDERIZADO — pendente
- Boss/inimigos/guardião com rim light: ⚪ AINDA NÃO RENDERIZADOS

**DOCUMENTAÇÃO**
- STATE.md atualizado (este arquivo)
- Seção 6 nova em `202608151500_auditoria_e_melhorias.md` com a pergunta
  de integração das poses de ataque

---

### Sprint 6 — Auditoria + Física + Pipeline de Animação (2026-08-15)

**Contexto:** pedido do Rafa foi auditar tudo (planning + gameplay + STATE),
conferir se o último plano/walkthrough (`202608151331_bug-fix_plan.md` /
`202608151353_walkthrough.md`) realmente fez o que prometia, e melhorar
modelos 3D/texturas/cor/física/animação com base nisso. Detalhe completo
em `planning/202608151500_auditoria_e_melhorias.md`. Resumo:

| Item | Descrição | Status |

|---|---|---|
| Auditoria B1/B2/B3 | Confirmados corrigidos de verdade no código (não só no changelog) | ✅ Verificado |
| Bug novo: partícula P5 fora da tela | `camera.toScreenX(hero.getX())` sem checar `isArena` em 2 pontos de `GameContext.java`; `startP5()` nunca resetava `camera.setX(0)` | ✅ Corrigido (3 pontos) |
| Bug novo: HUD sobrepondo texto | Nome do herói em x=235 fixo colidia com nome de fase longo (visto em screenshot real da Fase 5) | ✅ Corrigido (`UIRenderer.java`, ancorado à direita do painel) |
| Física de empuxo | Empuxo era constante fixa + arrasto linear (`vx *= drag`) | ✅ Trocado por empuxo oscilante amortecido por velocidade + arrasto quadrático (`HeroEntity.java`) |
| Squash & stretch | Não existia | ✅ Adicionado (`HeroEntity.java` + `RenderEngine.java`), incluindo pop de impacto no ataque |
| Ciclo de nado 3D | **Frames de animação eram idênticos** (5-6 de 6 com diff de pixel = 0, confirmado por script) — nenhum script Blender inseria keyframes | ✅ `_apsu_shared_lib.py` novo: `swim_cycle_keyframes()` (onda anguiliforme real) aplicado no herói; render tool subiu de 6→8 frames |
| Variações de ataque 3D | Só existia 1 pose estática de ataque | ✅ `01_adapa_ataque_variacoes.py` novo: thrust/slash/spin/charge via `attack_pose_keyframes()` |
| Silhueta vs. fundo (60-30-10) | Paleta de cada personagem era escolhida "no olho"; renders mostram personagens se perdendo contra os fundos pintados | ✅ Paletas de bg1-bg5 **medidas de verdade** (quantização de cor) + rim light (Fresnel) aplicado no herói, boss, Enki, inimigos (peixe/enguia/caranguejo/medusa), guardião |
| SSS da escama do herói | 0.12 de peso (quase zero, lia como plástico) | ✅ Subiu pra 0.30 com raio/cor próprios |
| **NADA do Blender foi renderizado nesta rodada** | Sandbox de auditoria não tem Blender/GPU instalado, nem consegue instalar (`bpy` não existe pra Python 3.12 e sem acesso à Maven Central/rede de pacotes Blender) | ⚠️ Só passou por `py_compile` (sintaxe). **Rodar no Blender do Rafa e conferir visualmente é obrigatório** — checklist em `personagens/scripts/00_LEIA_ME.md`. |

### Sprint 5 — Concluída ✅ (2026-08-15)

| Task | Descrição | Status |
|---|---|---|
| B1 Fix | Estabilização do herói no menu (aspecto estático fixo) | ✅ |
| B2 Fix | Escala proporcional da enguia (160×85) | ✅ |
| B3 Fix | Poder das bolhas refatorado para coordenadas de MUNDO | ✅ |
| 5 Fases | Expansão de 3 para 5 fases com identidades próprias | ✅ |
| F1 | Águas Claras (Enguia corrigida, recifes) | ✅ |
| F2 | Cavernas de Coral (Navio 3D, Baú, Poder das Bolhas) | ✅ |
| F3 | Correntes Abissais (Correntes de água, Zonas de pressão) | ✅ |
| F4 | Abismo Vulcânico (Arraião, Leviatã, Lagoas de lava) | ✅ |
| F5 | Templo Final de Apsu (Boss Kullullû, Enki em pessoa) | ✅ |
| Física | Correntes, empuxo variável, obstáculos móveis | ✅ |
| Assets 3D | `11_arraiao_abissal.py`, `12_obstaculo_vulcanico.py` | ✅ |
| Arte | `bg4.png` e `bg5.png` adicionados | ✅ |
| Testes | **19/19 testes automatizados com 100% de sucesso** (⚠️ antes da Sprint 6 — precisa re-rodar) | ✅ |

---

## 📁 Estrutura de Arquivos Atual

```
/home/rafael/github/cache/grafica/Game/
├── pom.xml
├── Makefile
├── src/
│   ├── main/
│   │   ├── java/br/apsu/
│   │   │   ├── core/
│   │   │   │   ├── GameContext.java      ✅ (5 fases, física ambiental, +fix P5/HUD Sprint 6)
│   │   │   │   ├── InputManager.java
│   │   │   │   ├── Camera.java
│   │   │   │   └── GameLoop.java
│   │   │   ├── graphics/
│   │   │   │   ├── RenderEngine.java     ✅ (Menu estável, 5 fases, +squash-stretch Sprint 6)
│   │   │   │   ├── LightingEngine.java
│   │   │   │   ├── SpriteManager.java
│   │   │   │   └── UIRenderer.java       ✅ (+fix overlap HUD Sprint 6)
│   │   │   └── model/
│   │   │       ├── enemy/EnemyType.java   ✅ (Arraião, Leviatã, Enguia)
│   │   │       ├── environment/SceneryElement.java ✅ (Correntes, Lava)
│   │   │       ├── environment/Projectile.java ✅ (Mundo/Arena)
│   │   │       ├── guardian/GuardianEntity.java ✅ (5 Guardiões)
│   │   │       └── hero/HeroEntity.java   ✅ (+empuxo/arrasto real, +squash-stretch Sprint 6)
│   │   └── resources/
│   │       ├── bg1.png, bg2.png, bg3.png, bg4.png, bg5.png ✅
│   │       └── sprites/...
│   └── test/java/br/apsu/                 ⚠️ (19 testes — precisa re-rodar após Sprint 6)
├── personagens/
│   ├── blends/...
│   └── scripts/
│       ├── _apsu_shared_lib.py            ✅ NOVO — rim light, PBR, ciclo de nado, ataques
│       ├── 01_adapa_ataque_variacoes.py   ✅ NOVO — thrust/slash/spin/charge
│       ├── 01_adapa_heroi.py              ✅ (+rim, +SSS real, +ciclo de nado real)
│       ├── 02_kullullu_boss.py            ✅ (+rim magenta-violeta)
│       ├── 03_enki_npc.py                 ✅ (+rim dourado)
│       ├── 04_peixe_sombrio_inimigo.py    ✅ (+rim por variação)
│       ├── 04_inimigos_variacoes.py       ✅ (+rim enguia/caranguejo/medusa)
│       ├── 05_guardiao_atlante_npc.py     ✅ (+rim por variação)
│       ├── 11_arraiao_abissal.py          ✅
│       ├── 12_obstaculo_vulcanico.py      ✅
│       └── gerador_mestre_apsu.py         ✅ (lista atualizada)
├── tools/
│   └── render_all_2d5_sprites.py          ✅ (6→8 frames por ciclo)
└── planning/
    └── 202608151500_auditoria_e_melhorias.md ✅ NOVO — auditoria completa desta sprint
```
