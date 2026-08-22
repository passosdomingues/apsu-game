# As Águas de Apsu — Biblioteca de Assets 3D Procedurais

Todas as melhorias de geometria, sombreamento procedural (60-30-10), enquadramento de câmera (sem tracking bugado) e organização por subpastas em `blends/` foram concluídas com sucesso.

---

## 📁 Estrutura de Diretórios Padronizada

```text
code/personagens/
├── blends/
│   ├── 01_adapa_heroi/
│   │   ├── 01_adapa_heroi.blend
│   │   ├── 01_adapa_nadando_disparo_bolhas.blend
│   │   ├── 01_adapa_var_abissal.blend
│   │   ├── 01_adapa_var_deus_dourado.blend
│   │   └── 01_adapa_var_recife.blend
│   ├── 02_kullullu_boss/
│   │   ├── 02_kullullu_boss.blend
│   │   ├── 02_kullullu_var_glacial.blend
│   │   └── 02_kullullu_var_toxico.blend
│   ├── 03_enki_npc/
│   │   ├── 03_enki_npc.blend
│   │   ├── 03_enki_var_celestial.blend
│   │   └── 03_enki_var_eremita.blend
│   ├── 04_peixe_sombrio_inimigo/
│   │   ├── 04_peixe_sombrio_inimigo.blend
│   │   ├── 04_inimigo_caranguejo_blindado.blend
│   │   ├── 04_inimigo_enguia_abissal.blend
│   │   └── 04_inimigo_medusa_eletrica.blend
│   ├── 05_guardiao_atlante_npc/
│   │   ├── 05_guardiao_atlante_npc.blend
│   │   ├── 05_guardiao_coral_vivo.blend
│   │   └── 05_guardiao_lamassu_aquatico.blend
│   ├── 06_bau_tesouro_elemento-cenario/
│   │   └── 06_bau_tesouro_elemento-cenario.blend
│   ├── 07_navio_naufragado_elemento-cenario/
│   │   └── 07_navio_naufragado_elemento-cenario.blend
│   ├── 08_recifes_e_cardume_elemento-cenario/
│   │   └── 08_recifes_e_cardume_elemento-cenario.blend
│   ├── 09_ruinas_e_colunas_atlantis/
│   │   └── 09_ruinas_e_colunas_atlantis.blend
│   └── 10_perigos_e_obstaculos_cenario/
│       └── 10_perigos_e_obstaculos_cenario.blend
├── renders/                               (24 PNGs renderizados em alta definição com transparência)
└── scripts/                               (16 scripts Python bpy automatizados)
```

---

## 🎨 Catálogo Completo dos Assets e Paletas 60-30-10

### 1. Herói: Adapa de Eridu
* **`01_adapa_heroi`**: Pose vertical clássica com tridente de ouro solar, cauda serpentina com escamas Voronoi e orbe de água.
* **`01_adapa_nadando_disparo_bolhas`**: Pose de ação horizontal (16:9), disparando projétil de água comprimida envolto por 4 anéis de vórtice e bolhas bioluminescentes.
* **Variações**:
  * *Abissal*: 60% Azul-Marinho / 30% Platina / 10% Ciano Neon.
  * *Deus Dourado*: 60% Ouro Solar / 30% Púrpura Real / 10% Esmeralda Divina.
  * *Recife*: 60% Verde-Esmeralda / 30% Cobre / 10% Magenta Rubi.

### 2. Chefe: Kullullû (Homem-Peixe / Leviatã de Magma)
* **`02_kullullu_boss`**: 60% Obsidiana basáltica negra, 30% carapaça carmesim, 10% rachaduras de magma submarino emissivo e lança do abismo.
* **Variações**:
  * *Tóxico*: Rachaduras verde-ácido e espinhos violeta.
  * *Glacial*: Obsidiana azul-ártico com rachaduras de gelo estelar.

### 3. Mentor: Enki (Deus da Sabedoria e das Águas Doces)
* **`03_enki_npc`**: 60% Manto azul-royal com textura ondulada (ShaderNodeTexWave), 30% barba farta em mechas mesopotâmicas, 10% cajado cuneiforme e halo solar.
* **Variações**:
  * *Eremita*: Manto verde musgo abissal e orbe aqua neon.
  * *Celestial*: Manto translúcido de luz astral e tabuleta de fogo divino.

### 4. Inimigos Comuns
* **`04_peixe_sombrio_inimigo`**: Peixe abissal predador com escamas Voronoi verde-doentio (60%), barbatanas púrpuras venenosas (30%) e lâmpada isca bioluminescente amarela ácida (10%).
* **`04_inimigo_enguia_abissal`**: Corpo serpentino segmentado com orbes elétricos ciano.
* **`04_inimigo_caranguejo_blindado`**: Carapaça couraçada com garras duplas e núcleo emissivo.
* **`04_inimigo_medusa_eletrica`**: Cúpula bioluminescente com anéis concêntricos e 6 tentáculos em cascata.

### 5. Guardião Atlante (NPC / Estátua Viva)
* **`05_guardiao_atlante_npc`**: 60% Arenito monolítico com textura de ruína, 30% faixas de pátina de cobre oxidado e 10% runas cuneiformes e olhos dourados que acendem na interação.
* **Variações**:
  * *Coral Vivo*: Arenito terracota com chifres de coral ramificado e runas bio.
  * *Lamassu Aquático*: Estátua com barba mesopotâmica em camadas e tiara de 5 chifres.

### 6. Cenários e Mecânicas de Fase
* **`06_bau_tesouro_elemento-cenario`**: Baú de madeira ripada com reforços de teal metálico e jóias internas.
* **`07_navio_naufragado_elemento-cenario`**: Galeão de 7 metros partido com costelas expostas, castelo de popa, mastro quebrado, grande âncora de ferro e anêmonas bioluminescentes.
* **`08_recifes_e_cardume_elemento-cenario`**: Cluster variado de corais (cérebro, leque, galhos e tubos com anéis de luz) e cardume de 18 peixes subindo em espiral de parallax.
* **`09_ruinas_e_colunas_atlantis`**: Plataforma de mármore com 3 colunas (íntegra, quebrada e inclinada), capitéis e altar com grande cristal de energia octaédrico flutuando.
* **`10_perigos_e_obstaculos_cenario`**: Chaminé hidrotermal basáltica expelindo coluna de bolhas de gases superaquecidos e mina naval de espinhos vermelhos com núcleo pulsante.
