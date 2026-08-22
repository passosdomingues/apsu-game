# Plano de Implementação: Expansão e Refinamento Procedural 3D dos Personagens e Cenários (Águas de Apsu)

Melhorar todos os modelos procedurais 3D em Blender (`bpy`), aplicando a regra de cores **60-30-10**, enriquecendo o nível de detalhe geométrico e de texturização procedural (Voronoi, Noise, ColorRamps, Bump, Subsurface/Emission), criando novas variações de cada categoria (heróis, deuses/NPCs, inimigos comuns, boss e props de cenário) e incluindo o exemplo especial de **Adapa nadando em alta velocidade e disparando projéteis/esferas de bolhas de água**.

---

## Estrutura do Projeto & Esquema de Cores 60-30-10

A paleta de cores 60-30-10 garante alto contraste visual e legibilidade imediata em 2.5D (estilo Donkey Kong Country / Rayman Origins / Ori):
- **60% (Cor Dominante)**: Base do corpo/casco/pele/manto (estabelece a silhueta primária contra o fundo marinho).
- **30% (Cor Estrutural / Secundária)**: Armaduras, barbatanas, chifres, joias, detalhes de relevo e silhueta secundária.
- **10% (Cor de Acento / Ponto Focal / Emissivo)**: Olhos, runas, pontas de tridente, orbes, magmas/venenos ou energia hidrocinética (telegraph de ataques e pontos interativos).

---

## Módulos e Scripts a Serem Aprimorados e Criados

### 1. Herói Adapa (`01_adapa_heroi.py` & `01_adapa_heroi_variacoes.py`)
- **Aprimoramentos no Modelo**:
  - Anatomia semidivina aprimorada (torso atlético, ombreiras mesopotâmicas com relevo de escamas e filigranas douradas).
  - Cauda serpentina com escamas procedurais multicamadas (Voronoi + Fresnel + Iridescência) e nadadeiras dorsais fluidas com nervuras translúcidas.
  - Rosto mais detalhado: barba tradicional mesopotâmica frisada/em anéis, tiara real com pedra aquamarina central, olhos emissivos.
  - Tridente lendário de Apsu com cabo entalhado, 3 pontas curvadas com núcleo de plasma de água.
- **Variações de Paleta e Visual**:
  - *Guardião dos Oceanos* (Turquesa 60% / Ouro 30% / Coral-Laranja 10%)
  - *Abismo Profundo* (Azul-Petróleo Noturno 60% / Platina-Prata 30% / Ciano Luminescente 10%)
  - *Ascensão Divina / Celestial* (Dourado-Bronze 60% / Púrpura Real 30% / Esmeralda Mística 10%)
  - *Guardião dos Recifes* (Verde-Esmeralda 60% / Cobre-Âmbar 30% / Rubi/Magenta 10%)

### 2. Exemplo Especial: Adapa Nadando e Disparando Bolhas (`01_adapa_nadando_disparo_bolhas.py`)
- **Pose Dinâmica e Ação**:
  - Pose hidrodinâmica horizontal de nado veloz com ondulação da cauda e nadadeiras traseiras esticadas.
  - Braço direito estendido concentrando uma esfera de água/bolha gigante com vórtice espiral e ondas de choque.
  - Feixe de bolhas de ataque (projéteis procedurais com reflexo iridescente, refração e brilho interno).
  - Rastro de turbulência de nado com pequenas microbolhas e iluminação cinematográfica aquática (efeito caustics/god rays).

### 3. Boss Kullullû (`02_kullullu_boss.py` & `02_kullullu_boss_variacoes.py`)
- **Aprimoramentos no Modelo**:
  - Silhueta monstruosa colossal com carapaça de obsidiana pontiaguda e placas dorsais gigantes.
  - Textura procedural de magma submerso com rachaduras profundas que emitem luz pulsante.
  - Braços monstruosos com garras de pedra vulcânica e lança corrompida de fogo abissal.
  - Mandíbula e múltiplos olhos incandescentes para leitura clara de perigo.
- **Variações**:
  - *Magma Vulcânico* (Obsidiana Roxa-Negra 60% / Vermelho Sangue 30% / Magma Laranja 10%)
  - *Bile Tóxica do Abismo* (Preto Piche 60% / Verde Ácido 30% / Amarelo Venenoso 10%)
  - *Gelo Negro Abissal* (Azul Glacial Escuro 60% / Índigo 30% / Ciano Congelado 10%)

### 4. NPC Deus Ancião Enki (`03_enki_npc.py` & `03_enki_npc_variacoes.py`)
- **Aprimoramentos no Modelo**:
  - Manto cerimonial fluindo como água com dobras e broches cuneiformes de prata e ouro.
  - Coroa com chifres divinos triplos mesopotâmicos (símbolo de alta divindade suméria).
  - Barba longa em camadas geométricas detalhadas.
  - Tabuleta sagrada de cuneiforme com relevos luminosos e Cajado de Apsu com orbe de água que levita e gira.
- **Variações**:
  - *Deus Sábio Primordial* (Azul-Royal Profundo 60% / Marfim-Prata 30% / Dourado Divino 10%)
  - *Eremita das Profundezas* (Verde Jade/Musgo 60% / Bronze Envelhecido 30% / Ciano Astral 10%)
  - *Espírito Celestial das Águas* (Branco Pérola Eclíptico 60% / Turquesa Claro 30% / Ouro Solar 10%)

### 5. Inimigos Comuns Variados (`04_inimigos_variacoes.py`)
- **Novas Criaturas Inimigas Procedurais**:
  1. *Peixe Sombrio Corrompido* (com dentes agudos, antena bioluminescente de predador abissal e espinhos venenosos).
  2. *Medusa Elétrica Abissal* (cúpula translúcida procedural com tentáculos ondulados e filamentos de alta voltagem).
  3. *Enguia Predadora do Vórtice* (corpo serpentino segmentado com barbatana contínua e mandíbula dupla).
  4. *Caranguejo Couraçado das Fendas* (casca rochosa com garras espinhosas e olhos pontiagudos).

### 6. NPCs Guardiões e Sentinelas (`05_guardioes_variacoes.py`)
- **Variantes de Sentinelas Antigos**:
  1. *Guardião Rúnico do Templo* (estátua colunar com hieróglifos cuneiformes entalhados e braços de pedra).
  2. *Sentinela de Coral Vivo* (estátua parcialmente consumida por anêmonas, esponjas e conchas luminosas).
  3. *Lamassu Aquático Ancestral* (totem sagrado com rosto divino, asas estilizadas e torso marinho).

### 7. Elementos de Cenário e Props de Fases (`06` a `10`)
- `06_bau_tesouro_elemento-cenario.py`: Versão aprimorada com variantes (Fechado, Entreaberto e Aberto exibindo moedas, pérolas e relíquia fóssil da Tartaruga Ancestral).
- `07_navio_naufragado_elemento-cenario.py`: Galeão naufragado em ruínas com mastro partido, cordames, costelas estruturais, âncora colossal e algas bioluminescentes.
- `08_recifes_e_cardumes_elemento-cenario.py`: Gerador procedural de floresta de corais (corais cérebro, leque, tubo, anêmonas) + enxame dinâmico de cardumes de fundo.
- `09_ruinas_e_colunas_atlantis.py` (NOVO): Pilares dórico-mesopotâmicos submersos, arcos cuneiformes partidos e pedestais de cristal luminoso.
- `10_perigos_e_obstaculos_cenario.py` (NOVO): Gêiseres termais submarinos com partículas de bolhas aquecidas, minas orgânicas e esporos explosivos.

### 8. Gerador Mestre (`gerador_mestre_apsu.py`)
- Script com menu interativo/função única no Blender para gerar qualquer um dos modelos ou compor cenários completos com iluminação submarina cinematográfica em 1 clique!

---

## Verificação e Testes

1. Executar os scripts Python com o executável do Blender (`C:\Program Files\Blender Foundation\Blender 4.5\blender.exe --background --python <script>`) para validar sintaxe, integridade de nós de shader e geração de geometria sem erros.
2. Renderizar previews automáticos de alta resolução para validar a estética, volumetria, cores 60-30-10 e detalhes.
3. Atualizar a documentação `00_LEIA_ME.md` com o catálogo completo de arquivos, comandos e guia de uso no Blender.
