# 🔍 Auditoria & Melhorias — Sprint 6 (2026-08-15)

> Pedido original do Rafa: auditar tudo (planning + gameplay + STATE),
> conferir se o último implementation plan/walkthrough fez de fato o
> que prometia, melhorar modelos 3D/textura/cor (60-30-10)/física de
> empuxo/animação estilo Donkey Kong Country, analisar renders, e
> melhorar os scripts Python gerando variações de pose/movimento.
>
> **Ambiente desta auditoria não tem Blender, GPU nem Maven/rede para
> Maven Central** — então tudo que é Java foi editado e checado por
> sintaxe/balanceamento (não compilado de verdade), e tudo que é
> Blender foi editado e checado por `py_compile` (não renderizado).
> **Nenhuma das duas coisas está confirmada visualmente.** Checklists
> de verificação no fim de cada seção.

---

## 0. Sprint 6.1 — verificação real no ambiente do Rafa (2026-08-16)

Rafa rodou de verdade: `make test` (Java) e `01_adapa_heroi.py` no
Blender 4.5 LTS dele, e mandou o log + o render de volta. Isso é
exatamente o ciclo que faltava — resultado:

| Verificação | Resultado |
|---|---|
| `make test` (com `rm -rf target` antes, pra forçar recompile) | ✅ 19/19 passando, compilando os 4 arquivos Java editados de verdade |
| Render do herói — geometria/textura geral | ✅ corpo, tridente, coroa, textura de escama (Voronoi) todos ok |
| Render do herói — rim light | ⚠️ **quase invisível** — amostrei os pixels da borda (script Python, ver abaixo) e só achei um resquício de 1-2px, longe do "contorno visível" pretendido |
| Render do herói — pose da cauda | ❌ **zigue-zague entre segmentos**, confirmado por crop ampliado do render |
| `01_adapa_ataque_variacoes.py` | ❌ `FileNotFoundError` — `__file__` resolveu pra `/01_adapa_ataque_variacoes.py` (rodado com nome nu do arquivo fora da pasta certa) |

### Causa raiz 1 — cauda em zigue-zague (bug real, não só falta de sorte)
`swim_cycle_keyframes()` usa `sin(phase_global - phase_offset)`. No
frame 1 (`phase_global=0`), isso só dá zero pro segmento com
`phase_offset=0` (o primeiro) — os outros ficam cada um num ponto
diferente da onda. Fisicamente faz sentido (onda viajante contínua
não tem "frame neutro"), mas pra um render ESTÁTICO de referência
isso quebra a pose desenhada à mão. **Corrigido**: agora existe um
frame-âncora (`frame_start - 1`, ou seja frame 0) com a pose limpa
original, e a cena fica parada nesse frame ao final da função — o
ciclo de nado continua existindo normal nos frames 1..8 (usado pelo
`render_all_2d5_sprites.py`), só o render ESTÁTICO isolado é que
volta a usar a pose de referência.

### Causa raiz 2 — rim light fraco demais
Amostrei os pixels bem na borda da silhueta do render que o Rafa
mandou (`PIL`, comparando alpha parcial vs. interior opaco). Achei
sinal de contribuição quente em 1-2 pixels exatos da borda, mas nada
perceptível a olho — a faixa de ativação do Fresnel (`power=2.6`
antigo) só começava depois de 61% do Fac, ou seja cobria uma faixa
fisicamente muito estreita nessas formas convexas (esferas/cones) a
essa distância de câmera. **Corrigido**: `power` default caiu pra
1.5 (ativa a partir de 33% do Fac — cobre bem mais silhueta) e
`strength` default subiu de 3.2-4.0 pra 6.0-7.0 em todos os
personagens (precisa competir com as luzes de área/ponto já fortes
da cena + a compressão do Filmic). Atualizado em todos os pontos de
chamada (herói, boss, Enki, inimigos, guardião), não só no default
da função.

### Causa raiz 3 — FileNotFoundError no script de ataque
`__file__` resolveu pra `/01_adapa_ataque_variacoes.py` — Blender
rodado com o nome puro do arquivo, sem o prefixo
`personagens/scripts/`, então `os.path.dirname()` deu `/` (raiz do
sistema). Não é bug de lógica, é a forma de invocar. **Comando certo**
(mesmo padrão que já funcionou pro `01_adapa_heroi.py`):
```
cd ~/github/cache/grafica/Game
/opt/blender-4.5.5-lts/blender --background --python personagens/scripts/01_adapa_ataque_variacoes.py
```
Também adicionei uma busca de fallback (`_find_script_dir()`) nesse
script específico, pra ele achar a pasta certa mesmo se invocado de
um jeito diferente no futuro.

### Ainda não verificado nesta rodada
- Eixo da onda da cauda (`wave_axis=1`) — o zigue-zague dificultou
  ver se o SENTIDO geral do balanço está certo (esquerda/direita na
  tela). Confirma isso de novo depois de rodar com o fix da pose.
- Se o rim de 6.0/1.5 ficou forte DEMAIS agora (risco oposto) — é
  mais fácil dar uma reduzida depois de ver forte do que adivinhar um
  valor "no meio" às cegas de novo.
- Os 4 estilos de ataque (thrust/slash/spin/charge) — não rodou ainda.

---

## 1. Auditoria do plano/walkthrough anterior

Verifiquei `202608151331_bug-fix_plan.md` e `202608151353_walkthrough.md`
contra o código real (não só contra o texto do changelog):

| Item reivindicado | Verificação | Resultado |
|---|---|---|
| B1 — herói estável no menu (aspecto fixo) | Lido `RenderEngine.drawHero`: aspecto calculado 1x a partir da imagem estática, não do frame animado | ✅ Confirmado corrigido |
| B2 — enguia com escala 160×85 | Lido `EnemyType.java` | ✅ Confirmado corrigido |
| B3 — projéteis em coordenadas de mundo | Lido `GameContext` (criação de bolha, colisão) e `Projectile.java` | ✅ Confirmado corrigido |
| 19/19 testes passando | Não re-executado (sem Maven neste ambiente) | ⚠️ Não verificável aqui — confiar no relatado, mas re-rodar após as edições desta sprint |

**Achado não coberto pelo plano anterior:** o padrão do fix B3 (`isArena
? hero.getX() : camera.toScreenX(hero.getX())`) foi aplicado em alguns
lugares mas não em todos. Rastreei os 11 usos de
`camera.toScreenX(hero.getX())` no arquivo e achei 2 que ainda
aplicavam a conversão incondicionalmente dentro de contexto de arena
(Fase 5): o burst de partícula na colisão herói↔boss (`updateP5()`,
linha ~648) e o burst do alerta "sem poder de bolha" em `tryShoot()`
(linha ~942). Causa raiz: `startP5()` nunca chamava `camera.setX(0)`
como as outras 4 fases fazem, então a câmera carregava o offset
residual da Fase 4 (até ~2634px num mundo de 4000px) para dentro da
arena do boss, e esses dois pontos específicos não tinham a checagem
`isArena` que os outros já tinham. **Corrigido nos 3 pontos** (ver
seção 3).

**ROADMAP.md estava desatualizado desde 2026-08-07** (Sprint 1 como
"ACTIVE", Fases 2-5 como "TODO") enquanto o STATE.md já registrava a
Sprint 5 concluída em 2026-08-15. **Corrigido** — reescrito para
refletir o estado real e incluir a Sprint 6.

**Projeto tem só 3 commits git**, e a working tree tinha uma
quantidade grande de arquivos novos/modificados nunca commitados
(toda a pasta `personagens/`, o pacote `src/main/java/br/apsu/*`
inteiro, `pom.xml`, `Makefile`, sprites). Não mexi no git (fora de
escopo desta auditoria), só registro pra você decidir quando
commitar.

---

## 2. Playtest — o que dava pra fazer sem Blender/Maven

Não consegui compilar nem rodar o jogo neste ambiente (sem Maven
Central, sem JavaFX vendorizado no repo). Em vez disso, analisei os
**screenshots reais de gameplay** que já existiam em
`personagens/fases/gameplay/` (tiradas por você, 2026-08-15) como
evidência de "como um jogador vê o jogo":

- **Menu**: estável, aspecto do herói correto (confirma B1). Layout
  bom na versão mais recente (13:17) — o painel de stats já estava
  numa caixa própria à direita, sem sobrepor nada.
- **Diálogos**: funcionam, mas o portrait do personagem (Enki/Enki
  Eremita/Enki Celestial) é pequeno e sem muito contraste contra o
  fundo escuro do diálogo — baixa prioridade, mas registrado.
- **Gameplay em Fase 1 (Águas Claras)**: aqui está o achado mais
  importante visualmente — o **fundo pintado é rico e colorido**
  (ruínas douradas, raios de luz, peixes coloridos), mas o **sprite
  do herói é pequeno, quase monocromático (branco/teal) e não lê
  como "homem-peixe"** — lê mais como um robô/boneco de segmentos.
  Ele quase desaparece contra o fundo. Isso motivou o trabalho de
  rim light + paleta medida (seção 4).
- **HUD sobrepondo texto**: confirmado meu palpite ao ler o código —
  no screenshot da Fase 5 ("Templo Submerso"), o texto
  "Tabuletas: 2/3 Bolhas: ATIVO" colide visualmente com
  "— Templo Submerso de Apsu [Médio] Guardião dos Oceanos". Raiz:
  nome do herói desenhado em `x=235` fixo, que não é longe o
  suficiente do nome da fase quando esse nome é comprido. **Corrigido**
  (seção 3).

---

## 3. Fixes aplicados (Java)

Todos em `src/main/java/br/apsu/`. **Não compilados neste ambiente** —
rode `make build && make test` antes de aceitar.

1. **`core/GameContext.java`**
   - `startP5()`: adiciona `camera.setX(0)` (causa raiz do bug de
     partícula fora da tela na arena do boss).
   - `updateP5()`: burst de partícula da colisão herói↔boss agora usa
     `hero.getX()` direto (arena = coordenada de tela), não mais
     `camera.toScreenX(hero.getX())`.
   - `tryShoot()`: burst do alerta "sem poder de bolha" agora checa
     `state == State.P5` como os outros pontos já faziam.

2. **`graphics/UIRenderer.java`**
   - `drawHUD()`: nome do herói passou de `x=235` fixo para
     ancorado à direita do painel (`TextAlignment.RIGHT`, x=428) —
     não colide mais com o nome da fase, não importa o tamanho.

3. **`model/hero/HeroEntity.java`** — física + squash&stretch
   - Empuxo: antes era uma constante fixa somada à aceleração
     vertical. Virou empuxo base + uma oscilação amortecida por
     velocidade (`idleBob`, mais forte parado, some quando nadando
     rápido) — representa o corpo boiando de verdade ao redor do
     ponto de flutuação neutra em vez de uma força fixa.
   - Arrasto: trocado de decaimento linear (`vx *= drag`) para
     arrasto quadrático (`F ∝ v·|v|`), coerente com o regime
     turbulento (Reynolds alto) da natação real, em vez do regime
     laminar que o decaimento linear implica. Coeficiente derivado do
     `drag` já calibrado por `HeroType`, igualado na velocidade
     máxima — o "feel" no topo de velocidade continua parecido, só a
     curva muda (mais solto perto do repouso, mais peso perto do
     máximo).
   - Squash & stretch: novo par `squashX/squashY`, reage à variação
     de velocidade quadro a quadro (estica ao acelerar, esmaga ao
     frear) + um pop de squash no pico do estágio "Impulso" do
     ataque (leitura de impacto imediata, estilo Donkey Kong Country).

4. **`graphics/RenderEngine.java`**
   - `drawHero()`: aplica `gc.scale(hero.getSquashX(), hero.getSquashY())`
     depois do flip de direção, então o squash é sempre relativo ao
     eixo "para frente" do personagem, não ao eixo da tela.

### Checklist de verificação (Java)
- [ ] `make build` — compila sem erro
- [ ] `make test` — os 19 testes (mais qualquer novo) continuam passando
- [ ] `make run` — jogar a Fase 5 até bater no boss e ver o burst de
      partícula nascendo NO herói, não fora da tela
- [ ] Jogar sem ter achado o baú da Fase 2, chegar na Fase 5 e apertar
      o botão de disparo — o alerta "Encontre o Baú..." deve nascer
      no herói, não fora da tela
- [ ] Olhar o HUD na Fase 5 (nome de fase mais comprido) e confirmar
      que "Guardião dos Oceanos" não sobrepõe mais o nome da fase
- [ ] Acelerar e frear bruscamente e ver se o squash/stretch é sutil
      (não deve parecer "borracha" — se estiver exagerado, os fatores
      `0.9`/`0.14`/`0.10` em `updatePhysics()` podem ser reduzidos)

---

## 4. Melhorias aplicadas (Blender/Python)

Detalhe completo no header de `_apsu_shared_lib.py` e no
`00_LEIA_ME.md` atualizado. Resumo do raciocínio:

### 4.1 Achado central: não existia ciclo de nado
Comparei os frames renderizados (`frame_001.png`..`frame_006.png`)
pixel a pixel (`PIL.ImageChops.difference`) em vários personagens —
**5 de 6 frames eram idênticos (diff = 0) em todos os casos
testados**. Causa: nenhum script de personagem chamava
`keyframe_insert()` — o loop de render (`scene.frame_set(N)`) não
tinha nada pra avaliar. A vivacidade que já existe no jogo vem
inteiramente do lado Java (tail wave/pitch procedurais sobre a
POSIÇÃO do sprite) — a POSE do sprite em si nunca mudava.

**Fix:** `swim_cycle_keyframes()` na nova `_apsu_shared_lib.py` —
onda anguiliforme de verdade (amplitude cresce da cabeça pra ponta da
cauda, como natação real), + bob de flutuação no root. Aplicado no
herói (`01_adapa_heroi.py`). Os eixos de rotação usados foram
deduzidos por cálculo a partir do rig de câmera de cada script
(câmera girada 90° em X → tela horizontal = eixo mundo X, tela
vertical = eixo mundo Z) — **não confirmado visualmente**, só por
matemática. Ver checklist abaixo.

### 4.2 Variações de pose de ataque (pedido explícito)
`attack_pose_keyframes()` — 4 estilos (thrust/slash/spin/charge),
usados em `01_adapa_ataque_variacoes.py` (script novo). Anima o
tridente + squash-and-stretch de impacto no torso, em cima do mesmo
corpo/ciclo de nado do herói (o personagem continua "vivo" enquanto
ataca).

### 4.3 Cor — 60/30/10 medido de verdade
Os renders de personagem (`personagens/renders/*.png`) mostram
paletas escolhidas "no olho", sem checar contra os fundos reais.
Medi (quantização MedianCut, PIL) a paleta real de `bg1.png`..`bg5.png`
— resultado em `PHASE_PALETTES` dentro de `_apsu_shared_lib.py`.
Achado principal: **a maioria dos fundos é MUITO escura** (V 9-30%
em bg2/bg3/bg4) — nessas fases, cor de preenchimento importa menos
que ter alguma auto-iluminação. Já bg1 é muito saturado em ciano — um
personagem na mesma família de cor se perde nele independente do
brilho.

### 4.4 Rim light (Fresnel) — a correção mais impactante
Em vez de reescolher a cor de preenchimento de cada personagem (alto
risco de descaracterizar o design já aprovado), adicionei um contorno
emissivo (Fresnel → borda) que faz a silhueta vencer o fundo
independente da cor de preenchimento. Aplicado em:
- **Herói**: dourado-coral quente universal (funciona em 4 das 5
  paletas medidas; a 5ª, caverna quase preta, qualquer cor clara já
  contrasta).
- **Boss Kullullû**: magenta-violeta deliberadamente DIFERENTE do
  laranja da própria magma e do dourado do herói — reforça
  "antagonista" só pela cor de contorno.
- **Enki, inimigos, guardião**: cada um reusa a própria cor de
  destaque já desenhada (olho/runa/núcleo emissivo) como cor de rim —
  reforça a identidade que o design já tinha, sem inventar cor nova.

### 4.5 Subsurface scattering real
`criar_mat_escamas` do herói tinha SSS em 0.12 (quase zero — lia como
plástico). Subiu pra 0.30 com raio/cor própria — pele/escama debaixo
d'água precisa de translucidez visível.

### Checklist de verificação (Blender — TUDO abaixo é obrigatório)
- [ ] Rodar `01_adapa_heroi.py` no Blender 4.5 do Rafa — conferir se
      o rim aparece como uma linha de brilho na borda da silhueta
- [ ] Conferir se a cauda ondula **da esquerda pra direita na tela**
      (não pra dentro/fora da tela) — se estiver no eixo errado,
      trocar `wave_axis=1` por `wave_axis=2` ou `0` na chamada de
      `swim_cycle_keyframes()` dentro do script
- [ ] Rodar `01_adapa_ataque_variacoes.py` — conferir os 4 renders
      (thrust/slash/spin/charge), braço deve se mover de forma legível
- [ ] Rodar `make render-sprites` completo (pipeline novo, 8 frames)
      e abrir a sequência de frames do herói pra confirmar que agora
      SÃO diferentes entre si (repetir o teste de diff de pixel que
      motivou este achado, se quiser confirmar por script)
- [ ] Depois de aprovar visualmente, aplicar o mesmo padrão (rim +
      SSS + ciclo de nado) nos scripts ainda "pendente" listados no
      `00_LEIA_ME.md` (variações extras do boss/enki/guardião, e os
      props de cenário se fizer sentido)

---

## 5. O que ficou de fora desta sprint (escopo consciente)

- **Continuidade de silhueta dos modelos 3D**: os personagens são
  construídos como pilha de primitivas independentes (esfera + cones)
  — lêem como "totem"/"boneco de neve", não como corpo contínuo de
  peixe. Resolver isso de verdade exigiria trocar a técnica de
  modelagem (Skin modifier numa curva, ou Boolean+Remesh voxel pra
  fundir as juntas) — mudança estrutural maior, com risco de quebrar
  a hierarquia de parenting que o resto do pipeline (animação,
  export) depende. Fica pro backlog do ROADMAP.
- **`tools/blender_render_sprites.py`** (pipeline MPI paralelo,
  `make mpi-demo`) não recebeu o mesmo passe — é um caminho de
  render separado do `render_all_2d5_sprites.py` (que é o usado por
  `make render-sprites`, o alvo "oficial"). Mesma lógica se aplicaria
  lá se você quiser estender.
- **Variações extras** (`01_adapa_variacoes.py`,
  `02_kullullu_boss_variacoes.py`, `03_enki_npc_variacoes.py`,
  `05_guardioes_variacoes.py`) e **props de cenário** (baú, navio,
  recifes, ruínas, obstáculos) não receberam rim light — o padrão de
  edição está documentado no `00_LEIA_ME.md` pra aplicar rápido
  quando quiser.
- **Commit no git**: não commitei nada (fora do meu escopo decidir
  isso por você) — mas com 3 commits só e tanta coisa untracked,
  vale considerar commitar em blocos lógicos.

---

## 6. Sprint 6.2 — curva procedural + decisão de ataque (2026-08-16)

### Cauda: de ângulos soltos pra curva procedural
Achado real (render + crop do Rafa): o zigue-zague não era bug da
animação — eram os ÂNGULOS ORIGINAIS de `01_adapa_heroi.py` E
`01_adapa_variacoes.py`, escolhidos um a um à mão, com inversão de
sinal no meio da cadeia (ex.: `...20°, 35°, 20°...` — sobe e desce).
Substituído por `smooth_angle_chain()`/`rebuild_chain_angles()`
(novo em `_apsu_shared_lib.py`): interpolação smoothstep entre os
ângulos inicial/final originais (preserva a intenção de design nas
pontas), com ondulação opcional que se anula nos extremos — sem
nenhuma inversão de sinal entre segmentos vizinhos (verificado com
script Python isolado, fora do Blender).

### Ciclo de nado — integração Java já existe, de graça
`RenderEngine`/`SpriteManager` (`loadSequence`, `FrameSeq.get`) já
carregam e ciclam sequências de 8 frames pra herói/inimigo/boss/
guardião — essa infra já existia ANTES da Sprint 6. O motivo de nunca
se ver animação de verdade nunca foi falta de integração — era só a
ausência de frames diferentes (a causa raiz original desta auditoria).

### Ataques — decisão do Rafa: usar os 4 estilos novos, capricho total
Rafa confirmou: os 4 estilos novos (thrust/slash/spin/charge)
substituem as 2 poses antigas de ataque (`disparo_bolhas`/
`atirando_bolhas`, consideradas malfeitas — "essa na horizontal é a
pior"). Precisa adicionar bolha modelada nos 4 renders novos (a pose
horizontal antiga tinha isso, os novos não) pra não perder o resíduo
visual de "acabei de soltar uma bolha". Ver seção 7 pra implementação.

---

## 7. Sprint 6.3 — capricho nos ataques + vida nos NPCs + diversidade física (2026-08-16)

### Ataques: bolha nos 4 estilos novos, poses antigas desativadas
Rafa confirmou: os 4 estilos (thrust/slash/spin/charge) substituem
`disparo_bolhas`/`atirando_bolhas` de vez ("as duas antigas estavam
malfeitas, a horizontal é a pior"). Adicionei `add_attack_bubbles()`
(bolhas perto da ponta do tridente, padrão de espalhamento diferente
por estilo) pra não perder o resíduo visual de "acabei de atirar", e
reescrevi a seleção de sprite em `RenderEngine.drawHero()` — agora
cicla os 4 estilos a cada tiro (`HeroEntity.attackStyleIndex`) em vez
de alternar 2 poses. Não toquei em `attackPose2` nem no cálculo de
spawn da bolha (`getBubbleSpawnX/Y`) — mantidos como estavam, pra não
arriscar a física/colisão do projétil por uma troca cosmética.

### Ciclo de nado: peixe sombrio e enguia
Peixe sombrio não tem cauda articulada (corpo único) — tratei as 2
barbatanas peitorais + a cauda triangular como uma cadeia curta pro
`swim_cycle_keyframes` (amplitude cresce em direção à cauda, que é
quem realmente propele). Enguia já é uma cadeia de 6 segmentos +
cauda bifurcada — aplicação direta, é o caso mais natural de onda
anguiliforme de todo o elenco.

### Vida idle: Enki e Guardião Atlante
Novo `add_idle_life_keyframes()` — não usa cadeia de segmentos (esses
NPCs não têm cauda), usa balanço de manto (rotação, mesmo eixo do
ciclo de nado) + respiração (pulso de escala) + inclinação de cabeça
defasada. Aplicado nos dois; Guardião com amplitude bem menor e sem
balanço de manto (é pedra, não carne — mantém a leitura de estátua
viva, não de personagem de pano).

### Diversidade física real dos Adapa (altura/gordura, não só cor)
Achado importante: `HeroType.java` **já tinha** identidade de física
bem diferenciada por tipo — ABISSAL="Tanque Pesado, muita inércia",
DEUS="Velocista, altíssima velocidade", RECIFE="Tático Ágil,
desaceleração rápida" — só a geometria Blender não acompanhava isso,
só a cor mudava. Adicionei `escala_altura`/`escala_corpo`/
`escala_cauda_len` no dict de paleta de cada variante em
`01_adapa_variacoes.py`, aplicados via `root.scale` (altura/gordura
geral) + um alongamento específico só na cauda (não no torso/cabeça):
- **Abissal** (tanque): 0.90 altura / 1.24 corpo / 0.90 cauda —
  atarracado e grosso, cauda curta e forte
- **Deus** (velocista): 1.10 altura / 0.86 corpo / 1.18 cauda — alto
  e esguio, cauda longa pra mais propulsão
- **Recife** (ágil): 0.93 altura / 0.96 corpo / 0.98 cauda — compacto,
  proporção mais neutra (agilidade vem de resposta, não de tamanho)

Os números batem com os stats que JÁ existiam em HeroType.java — não
inventei uma identidade nova, só fiz a silhueta contar a mesma
história que os atributos de jogo já contavam.

### O que ainda não foi tocado nesta rodada
- Boss (`02_kullullu_boss_variacoes.py`) sem idle life — fica parado
  na arena, não "nada" continuamente do mesmo jeito; se fizer sentido
  dar vida nele também (respiração/pulso de magma?), é próximo passo.
- `01_adapa_nadando_disparo_bolhas.py` e `01_adapa_ataque_variacoes.py`
  ficam como scripts standalone válidos (já melhorados com rim/SSS/
  ciclo de nado ou bolhas), só não são mais o caminho ativo de
  render do ataque principal — `render_all_2d5_sprites.py` continua
  gerando os sprites deles se rodado, só o Java não usa mais.
- **Nada renderizado nesta rodada** — sandbox sem Blender. Precisa
  rodar os scripts atualizados e `make test` antes de aceitar.

---

## 8. Sprint 6.4 — causa raiz do "duros" era o Makefile (2026-08-16)

### O pipeline tinha um elo faltando
`make render-sprites` sempre existiu, mas ele só faz UMA coisa: abrir
os `.blend` que já estão em `personagens/blends/` e renderizar os 8
frames de cada. Ele **nunca** roda os scripts `.py` de novo. Quem
regenera o `.blend` a partir do script atualizado é
`gerador_mestre_apsu.py` — que precisa ser chamado com
`python3 gerador_mestre_apsu.py` (não `blender --python`, ele mesmo
invoca o Blender como subprocess pra cada script da lista). Esse
comando nunca esteve no Makefile. Resultado: editar um script `.py`
não tinha efeito nenhum até alguém rodar esse orquestrador manualmente
— exatamente por isso o herói principal (rodado manualmente nesta
conversa) nadou, e as variantes/Enki/guardião (só via
`make render-sprites`) continuaram com os `.blend` antigos.

Corrigido: `make generate-characters` (só regenera os `.blend`) e
`make assets` (pipeline completo: regenera → renderiza → organiza em
assets/). **Este é o comando que o Rafa deve rodar agora.**

### Rocha vulcânica: outro caso de "asset pronto, nunca conectado"
Mesmo padrão do ciclo de nado (Sprint 6) e das poses de ataque
(Sprint 6.3) — `12_obstaculo_vulcanico.py` já existia, já tinha sido
gerado, e nunca tinha um `spriteManager.getImage(...)` chamando ele em
lugar nenhum. `VOLCANIC_ROCK` e `MOVING_OBSTACLE` da Fase 4 caíam pro
retângulo com contorno laranja brilhante — que, visto de longe/em
movimento, lê como um portal genérico, não uma rocha. Conectado agora
(com fallback pro retângulo antigo só se a imagem não carregar, por
segurança).

### O que ainda falta em cenário (pedido explicitamente, não resolvido ainda)
- `MOVING_OBSTACLE` da **Fase 3** (roxo/água) não tem asset 3D
  equivalente — diferente do caso da Fase 4, aqui não tem nada pronto
  pra conectar, precisaria de um script novo (ex.:
  `13_obstaculo_abissal.py`, uma pedra/coral corrompido temático de
  água profunda) seguindo o mesmo padrão dos scripts 06-12.
- `GEYSER`, `CURRENT`, `PRESSURE_ZONE` continuam 100% efeito
  procedural de Canvas (glow/partículas) — isso é DIFERENTE de
  "placeholder ruim": geralmente esse tipo de elemento (corrente de
  água, zona de pressão) faz mais sentido como efeito translúcido
  animado do que como objeto sólido 3D, então não teria o mesmo
  problema visual do retângulo-portal. Não mexi nisso — avisar se o
  Rafa achar que também precisa de asset 3D aqui.
- "Oráculo" — Rafa mencionou que talvez falte um guardião/oráculo nas
  Fases 3/4. Conferido: as 5 fases têm 1 `GuardianEntity` cada
  (índices 0-4) no código. Não fica claro se "Oráculo" é um pedido de
  personagem NOVO (diferente do Guardião Atlante já existente) ou só
  o nome que o Rafa quer dar/associar ao guardião dessas fases —
  perguntei antes de supor e criar algo que não é o que ele imaginou.

---

## 9. Sprint 6.5 — obstáculo abissal + Oráculo criado do zero (2026-08-16)

### O Oráculo nunca existiu de verdade
Confirmei em disco: `personagens/renders/05_guardiao_oraculo_correntes.png`
não existe. `GuardianEntity.java` já tinha nome, diálogo e sprite path
pra ele (type=3, usado na Fase 4), mas nenhum script Blender jamais o
construiu — diferente do caso da rocha vulcânica (Sprint 6.4), que
JÁ existia e só faltava conectar, aqui faltava o modelo inteiro.

Criado `construir_oraculo_chamas()` em `05_guardioes_variacoes.py`,
reaproveitando a mesma "gramática visual" dos outros guardiões
(corpo colunar de pedra + cabeça + runas + luzes no mesmo enquadramento)
mas com identidade visual própria — terceiro-olho profético (gema na
testa), coroa de brasas flutuantes ao redor do elmo (no lugar dos
chifres do padrão / galhos do coral), e um braseiro erguido nos braços.
Obsidiana rachada com veias de magma como material principal.

### Nome corrigido: "Correntes" → "Chamas Abissais"
O nome antigo "Oráculo das Correntes" tinha sido colocado na Fase 4
(vulcânica) — um nome de água numa fase de lava. Só o NOME destoava;
o diálogo já mencionava magma/arraias/calor vulcânico corretamente.
Troquei só `NAMES[3]` e a primeira linha do diálogo (a que o
personagem usa pra se apresentar) — mantive `SPRITE_PATHS[3]` com o
nome de arquivo antigo (`05_guardiao_oraculo_correntes.png`) de
propósito, pra não precisar tocar em mais nada.

### Obstáculo da Fase 3
`13_obstaculo_abissal.py` novo — mesmo padrão/escala de
`12_obstaculo_vulcanico.py` (que resolveu o mesmo problema na Fase 4
na sprint anterior), mas tematizado como um crescimento rochoso/
coralino corrompido pela peçonha de Kullullû: rocha escura com veias
tóxicas roxo-magenta e esporos flutuantes, em vez de rocha vulcânica
com fissuras de magma laranja. Escolha de tema amarra com a narrativa
já estabelecida (Kullullû envenenou Apsu) e garante leitura de cor
diferente da Fase 4 (roxo vs. laranja), já que os dois usam a mesma
mecânica de jogo (MOVING_OBSTACLE).

### Lição pra próxima auditoria de asset
Ao conferir se um script está na lista do `gerador_mestre_apsu.py`,
`grep` por substring simples pode falhar por causa de plural/sufixo
(`"05_guardiao"` não bate com `"05_guardioes"`). Da próxima vez,
comparar a lista inteira contra `ls personagens/scripts/*.py` é mais
confiável — foi assim que achei (e desfiz) uma duplicata que eu
mesmo quase introduzi nesta rodada.

---

## 10. Sprint 6.6 — vida no boss + cardume, e próximos passos sugeridos (2026-08-16)

### Boss e cardume
Kullullû era o único personagem principal sem nenhuma vida idle —
corrigido, com respiração mais forte/rápida que os NPCs (é um
predador, não pedra). O cardume de peixes (08) ganhou variedade real
de pose por peixe — mas via variação na CONSTRUÇÃO, não keyframe de
animação, porque descobri que **cenário nunca é carregado como
sequência animada no Java** (`getImage()` sempre, nunca
`loadSequence()`) — diferente de personagens. Registrar isso evita
reinvestir esforço em animação de cenário que nunca seria vista.

### Próximos passos — minha recomendação priorizada
Ordem pensada pelo mesmo critério que o Rafa definiu (bugs > estado/
física > escala > silhueta > animação > integração > iluminação >
paleta > efeitos > docs), aplicado ao que resta:

1. **Verificar visualmente esta rodada** (boss, cardume, Oráculo,
   obstáculo abissal) — nada disso foi confirmado ainda, é sempre o
   passo mais importante antes de continuar empilhando trabalho.
2. **Vida idle nos elementos de cenário restantes** (baú — tampa com
   leve inclinação; navio — âncora balançando na corrente) — mesmo
   raciocínio do cardume, rápido de fazer.
3. **GEYSER/CURRENT/PRESSURE_ZONE** — continuam efeito puro de Canvas.
   Minha avaliação: isso é OK pra correntes/zonas de pressão (efeito
   translúcido combina mais com "água se movendo" do que um objeto
   3D sólido) — mas vale o Rafa confirmar se concorda ou se quer
   asset 3D ali também.
4. **Squash-stretch/banking só existe pro herói no lado Java** —
   `RenderEngine` nunca ganhou o mesmo tratamento pra inimigos/boss.
   Extensão simples, mesmo padrão já provado.
5. **Continuidade de silhueta dos modelos 3D** — os personagens ainda
   são pilha de primitivas (esfera+cones independentes), leem como
   "totem" mais do que corpo contínuo. Resolver de verdade precisaria
   trocar técnica de modelagem (Skin modifier numa curva, ou
   Boolean+Remesh voxel) — mudança estrutural maior, ainda no
   backlog desde a Sprint 6 original.
6. **Áudio** — nada tocado ainda nesta auditoria inteira. Os 4 estilos
   de ataque, as bolhas, a vida idle dos NPCs — nenhum tem som
   dedicado. `AudioManager.java` existe mas não foi auditado.
7. **`tools/blender_render_sprites.py`** (pipeline MPI paralelo) nunca
   recebeu o passe da biblioteca compartilhada — caminho secundário,
   `render_all_2d5_sprites.py` continua sendo o oficial.
8. **Git**: projeto ainda com poucos commits e muita coisa untracked
   desde o início desta auditoria. Não mexi (não é minha decisão),
   mas em algum momento vale commitar em blocos lógicos antes de
   acumular mais.

---

## 11. Sprint 6.7 — polimento final desta rodada (2026-08-16)

### Squash-stretch chegou nos inimigos e no boss
Antes só o herói tinha esse efeito no lado Java. Pra inimigos comuns,
reusei a matemática que já existe (`EnemyEntity.update` já calcula um
bob senoidal) — o pulso de squash é só `sin()` amarrado à posição de
tela + tipo, sem precisar guardar nenhum estado novo na entidade. Pro
boss, uma respiração mais lenta e mais pesada, complementando (não
duplicando) a vida idle que já tinha sido dada ao modelo 3D dele.

### Duas ideias descartadas conscientemente
- **Áudio novo**: cobertura atual já é razoável, e sem ferramenta de
  síntese de áudio neste ambiente não dá pra gerar clipes novos de
  verdade — mudar por mudar sem conteúdo novo não teria valor.
- **Subdivision Surface como alternativa "segura" ao remesh**: pensei
  nisso desde a Sprint 6 original como forma de suavizar a silhueta
  sem o risco do remesh completo. Reconsiderando com mais cuidado:
  não funcionaria — os segmentos são objetos de malha SEPARADOS, e
  Subsurf não costura a lacuna ENTRE dois objetos diferentes, só
  suaviza a topologia dentro de cada um individualmente (que já é
  esfera/cone, já suave). Registrar isso evita alguém (eu ou outra
  sessão) tentar de novo achando que é uma opção de baixo risco.

### O que resta em aberto (nenhum destes tem solução barata/segura ainda)
- Continuidade de silhueta de verdade (fundir os segmentos numa malha
  única) — precisaria Boolean+Remesh voxel ou Skin modifier numa
  curva, mudança estrutural, alto risco sem conseguir renderizar e
  ver o resultado neste ambiente.
- GEYSER/CURRENT/PRESSURE_ZONE — minha avaliação continua sendo que
  o efeito de Canvas atual é apropriado pra esses (água/pressão
  combinam com efeito translúcido), mas segue sendo pergunta aberta
  pro Rafa confirmar.
- Git — projeto segue com poucos commits e bastante coisa untracked.

---

## 12. Sprint 6.8 — Leviatã Menor + auditoria de cobertura completa (2026-08-16)

### O maior inimigo do jogo era invisível
`EnemyType.LEVIATA` — descrito no próprio comentário do código como
"sub-boss da Fase 4" — é o maior inimigo comum do jogo (180×160,
maior que o Arraião e mais que o dobro da Medusa) e está posicionado
na Fase 4 desde sempre (`GameContext.java`, `startP4()`). Nunca teve
asset 3D. Terceiro caso desse padrão nesta auditoria (depois do
Oráculo e da rocha vulcânica) — mas desta vez fui atrás
SISTEMATICAMENTE em vez de esperar o Rafa notar visualmente: extraí
todo caminho `sprites/...` citado em qualquer lugar do código Java
(`grep -roh 'sprites/[...]'`) e cruzei contra os renders que
realmente existem em `personagens/renders/`. Confirmação: TODOS os
outros — 5 inimigos, 4 heróis, 3 Enkis, 5 guardiões, cenário — batem.
Só o Leviatã faltava.

### Design do Leviatã
Serpente marinha — corpo anguiliforme como a enguia, mas maior/mais
grosso (8 segmentos vs 6, raio inicial quase o dobro), cabeça com
mandíbula articulada e dentes, crista de espinhos dorsais ao longo do
corpo inteiro (silhueta reconhecível de longe, importante pra um
"sub-boss"). Material reusa a técnica Voronoi de basalto+magma do
próprio Kullullû (veias de emissão só nas áreas "quentes" do padrão,
resto escuro) — amarra visualmente o Leviatã ao boss da mesma fase,
como se fosse "da mesma família" de criaturas corrompidas pelo calor
vulcânico. Ciclo de nado aplicado (candidato ainda mais natural que a
enguia — é literalmente uma serpente).

### Nota de nomenclatura
O arquivo do script é `14_leviata_menor.py` (próximo número
disponível na sequência), mas a função `salvar()` grava a saída como
`"12_leviata_menor"` — esse nome de SAÍDA precisa bater exatamente
com o que `EnemyType.LEVIATA` espera em Java (`12_leviata_menor.png`),
que não necessariamente segue a numeração dos arquivos de script.
Registrando isso pra não confundir uma futura auditoria (o número do
.py e o número do .png/.blend gerado não são a mesma coisa aqui).

### Conclusão desta rodada
Com o Leviatã, a cobertura de assets 3D do projeto está completa —
todo personagem/inimigo/cenário referenciado no código Java agora tem
um script Blender correspondente. O que resta são itens de POLIMENTO
(silhueta/costura entre segmentos, GEYSER/CURRENT como efeito de
Canvas) ou verificação (nada desta sprint 6.8 foi renderizado ainda).
