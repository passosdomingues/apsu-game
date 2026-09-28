# Pacote de Assets Procedurais — As Águas de Apsu

Cole cada script na área de **Scripting** do Blender e rode (▶), ou use
`gerador_mestre_apsu.py` (roda todos via `blender --background --python`; use
`make generate-characters` ou `make assets`). `make render-sprites` renderiza
PNGs apenas para o cenário; os personagens são exportados como malhas OBJ/MTL
e desenhados em 3D durante o jogo. Cada script de personagem/prop
é independente — os principais (01-05) limpam a cena ao rodar; os
props de cenário **não** limpam, pra você poder compor junto com um
personagem.

## ⚠️ Atualização 2026-08-15 — leia antes de rodar
Auditoria encontrou que os "ciclos de nado" renderizados eram, na
prática, **o mesmo frame repetido 5-6 vezes** (confirmado por diff de
pixel) — nenhum script inseria keyframes, só o motor Java animava a
posição/rotação do sprite inteiro em cima da pose estática. Isso foi
corrigido na origem: ver `_apsu_shared_lib.py` (novo) e a seção
"Biblioteca compartilhada" abaixo. **Nada disso foi renderizado ainda
num Blender real** (rodou só análise estática de código) — rode e
confira visualmente antes de aceitar. Checklist no fim deste arquivo.

## Biblioteca compartilhada — `_apsu_shared_lib.py`
Novo arquivo, carregado automaticamente pelos scripts já atualizados
(01, 02, 03, 04, 05) via `exec()` no topo — não precisa colar/rodar
ele separado, mas ele TEM que estar na mesma pasta. Fornece:
- **Paletas 60/30/10 medidas de verdade** nos `bg1.png`..`bg5.png`
  (quantização de cor, não "no olho") — `PHASE_PALETTES` / `get_phase_rim()`.
- **Rim light (Fresnel)**: `add_fresnel_rim()` — contorno emissivo que
  faz a silhueta do personagem vencer o fundo, mesmo quando a cor de
  preenchimento é parecida com a do cenário.
- **PBR orgânico**: `make_organic_pbr()` — subsurface scattering de
  verdade (o `criar_mat_escamas` do Adapa usava 0.12, quase zero).
- **Ciclo de nado real**: `swim_cycle_keyframes()` — onda anguiliforme
  (amplitude cresce da cabeça pra ponta da cauda) + bob de flutuação,
  gera keyframes de verdade nos frames que `tools/render_all_2d5_sprites.py`
  (agora 8, era 6) vai capturar.
- **Variações de ataque**: `attack_pose_keyframes()` — thrust/slash/
  spin/charge para armas (tridente etc.), com squash-and-stretch de
  impacto. Usado por `01_adapa_ataque_variacoes.py` (novo).

## Arquivos — personagens
| Arquivo | O que é | v5.0 (2026-08-15) |
|---|---|---|
| `01_adapa_heroi.py` | Herói Adapa | + rim light quente, + SSS real na escama, + ciclo de nado real (8 frames) |
| `01_adapa_nadando_disparo_bolhas.py` | Adapa disparando | (ainda não recebeu o passe de rim/nado — mesmo padrão do `01_adapa_heroi.py` se aplica) |
| `01_adapa_variacoes.py` | Skins alternativas do Adapa | idem acima — pendente |
| `01_adapa_ataque_variacoes.py` | **Novo** — 4 poses de ataque | thrust / slash / spin / charge, reusa o corpo de `01_adapa_heroi.py` |
| `02_kullullu_boss.py` | Boss Kullullû | + rim magenta-violeta (contraste proposital com o dourado do herói e com a magma laranja do próprio corpo) |
| `02_kullullu_boss_variacoes.py` | Variações do boss (glacial/tóxico) | pendente do passe de rim |
| `03_enki_npc.py` | NPC sábio/mentor | + rim dourado "divino" (tema recorrente em todas as fases) |
| `03_enki_npc_variacoes.py` | Variações do Enki | pendente do passe de rim |
| `04_peixe_sombrio_inimigo.py` | Inimigo comum (3 variações) | + rim = cor do próprio olho de cada variação |
| `04_inimigos_variacoes.py` | Enguia / Caranguejo / Medusa | + rim = cor do próprio accent emissivo de cada um |
| `05_guardiao_atlante_npc.py` | NPC guia (3 variações) | + rim = cor da própria runa de cada variação |
| `05_guardioes_variacoes.py` | Variações extras do guardião | pendente do passe de rim |

## Arquivos — cenário/props
`06_bau_tesouro_elemento-cenario.py`, `07_navio_naufragado_elemento-cenario.py`,
`08_recifes_e_cardume_elemento-cenario.py`, `09_ruinas_e_colunas_atlantis.py`,
`10_perigos_e_obstaculos_cenario.py`, `11_arraiao_abissal.py`,
`12_obstaculo_vulcanico.py` — ainda não receberam o passe de rim light
(props tendem a ficar dessaturados de propósito, ver seção de paleta
abaixo, então o ganho é menor — mas dá pra aplicar `add_fresnel_rim`
neles do mesmo jeito se quiser mais "pop" nos obstáculos de perigo).

## Por que "Uanna" virou "Enki"
O `GamePlan.md` define o NPC como "Ancião Enki, Deus da sabedoria e das águas" — o script antigo usava "Uanna". Mantive o arquivo como `enki_npc.py` para não gerar inconsistência de nome quando você ligar isso ao Java (`npc.png`, diálogos etc.).

## Lógica da paleta 60-30-10 (Disney) aplicada
A regra não foi usada só "por char" — foi pensada para o **personagem sempre vencer o contraste contra o fundo**:

- **Cenário (Atlantis submersa)**: dominante fria — azuis/teals escuros e neblina.
- **Heróis/NPCs aliados**: 60% em cor **quente ou muito saturada** que é o oposto do fundo (turquesa vibrante no Adapa, azul-royal profundo mas com brilho no Enki) — o olho vai direto neles.
- **Vilão/inimigos**: 60% em tons **escuros/roxos/pretos** que também destoam do azul de fundo (silhueta "perigo" nítida), com 10% emissivo (magma, veneno) pra telegraph de ataque à distância — importante em side-scroller.
- **Props de cenário**: propositalmente **dessaturados**, pois eles não podem competir visualmente com personagens — servem de camada de profundidade (parallax).

Cada script tem comentário no topo explicando a paleta específica daquele personagem/prop.

## Próximos passos sugeridos
- **Fonte única de assets:** todos os `.blend` devem ir para
  `personagens/blends/<família>/` e os previews para `personagens/renders/`.
  Não use caminhos absolutos ou caminhos copiados de outra máquina: derive a
  raiz com `os.path.dirname(os.path.dirname(os.path.abspath(__file__)))`.
- **Pipeline após editar um script:** rode `make assets`; ele gera os `.blend`,
  renderiza os PNGs 2.5D e copia os modelos organizados para `assets/`.
- Depois de gerar cada personagem, use **File > Export** (ou render com fundo transparente) pra tirar o sprite/frame que vai virar `hero.png`, `boss.png`, `npc.png` no jogo Java.
- Pra manter o estilo Donkey Kong Country (2.5D), renderize sempre do mesmo ângulo de câmera fixo por personagem — todos os scripts já deixam a câmera configurada nesse sentido.
- Aplicar o mesmo passe de `_apsu_shared_lib.py` (rim + SSS + ciclo de
  nado) nos scripts marcados "pendente" acima — o padrão de edição é
  sempre o mesmo (ver diff de `01_adapa_heroi.py` como referência):
  1) cola o loader da lib logo após os `import`;
  2) depois de criar os materiais principais, chama `add_fresnel_rim`;
  3) se o personagem tiver uma cadeia de segmentos tipo cauda/tentáculo,
     coleta eles numa lista e chama `swim_cycle_keyframes` no fim da
     função `construir_*`.

## ✅ Checklist de verificação (rodar no Blender de verdade)
Os modelos runtime OBJ/MTL e as poses de nado já são exportados pelo pipeline.
Confira visualmente no jogo a escala, orientação e leitura das malhas depois de
regenerar os assets. Ao revisar:
1. **Rim light aparece?** Gire a câmera (ou olhe o render final) e
   confira se dá pra ver uma linha de brilho colorida na borda da
   silhueta, mais forte nos cantos "de perfil" da malha.
2. **Onda da cauda está no eixo certo?** `swim_cycle_keyframes` anima
   `rotation_euler[1]` (eixo Y) por dedução de como a câmera de cada
   script está montada (câmera girada 90° em X, olhando pra frente) —
   se a cauda balançar pra "dentro/fora da tela" em vez de
   "esquerda/direita", troque `wave_axis=1` pra `wave_axis=2` ou `0`
   na chamada dentro do script do personagem.
3. **Ataque (thrust/slash/spin/charge)**: rode
   `01_adapa_ataque_variacoes.py` e confira os 4 renders — o braço
   deve se mover de forma legível e voltar pra pose de repouso no
   último frame.
4. Rode `make assets` para regenerar as malhas OBJ/MTL dos personagens e os
   PNGs de cenário; a exportação dos personagens é feita automaticamente.
