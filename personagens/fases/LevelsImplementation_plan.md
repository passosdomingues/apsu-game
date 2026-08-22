# Plano de Implementação: Fase 3D de Atlantis Submersa ("As Águas de Apsu") via Blender Python (`bpy`)

Criação de um gerador procedural completo em Python (`bpy`) para o Blender 4.5 que constrói uma fase inteira 3D no estilo **Cidade Perdida de Atlantis / Templo Submerso de Apsu**, inspirada na arte conceitual `ApkalluScene.png`, integrando todos os personagens, inimigos, NPCs, perigos, tesouros, ruínas e sistemas de animação procedural em uma única cena 3D coesa e cinematográfica.

---

## 1. Visão Geral do Cenário e Composição da Fase

O cenário será modelado e montado no espaço 3D simulando uma progressão de fase de jogo (2.5D com profundidade de camadas e parallax cinematográfico), dividido em 4 zonas conectadas:

```
[Zona 1: Entrada dos Recifes & Santuário] 
    └── Cardumes, corais bioluminescentes, naufrágio antigo, baús e o sábio ENKI em seu altar.
[Zona 2: A Grande Via Sacra de Atlantis] 
    └── Colunatas monumentais em mármore azulado e ouro mesopotâmico, arcos em ruínas, cristais de energia atlante e estátuas gigantes (Guardiões Atlantes / Lamassu).
[Zona 3: Desfiladeiro Abissal de Perigos] 
    └── Gêiseres hidrotermais ativos emitindo bolhas e fumaça, minas de espinhos atlantes, medusas elétricas e peixes sombrios patrulhando.
[Zona 4: O Portal do Templo de Apsu & Covil do Boss] 
    └── O grandioso portal submarino adornado com runas reluzentes onde o colossal Boss KULLULLÛ espreita nas profundezas.
```

---

## 2. Elementos Integrados e Técnicas `bpy`

### A. Personagens e NPCs (Procedurais com Shaders e Animações)
1. **Adapa (O Herói Apkallu)**:
   - Modelo com corpo turquesa em escamas procedurais (Voronoi Bump + Subsurface), armadura dourada e tridente.
   - **Animação de Nado**: Ondulação senoidal na cauda e corpo, flutuação e movimento dos braços/barbatanas.
   - **Deslocamento**: Travessia 3D ao longo do cenário.
   - **Disparo de Bolhas / Vórtices**: Projéteis de água bioluminescentes atirados durante a natação.
2. **Kullullû (Boss Abissal)**:
   - Posicionado no portal profundo da Zona 4 com animação de idle respiratório/ameaçador, cauda sinuosa e olhos/fendas vulcânicas emissivas.
3. **Enki (Sábio / NPC Guia)**:
   - Posicionado no Santuário de Pedra (Zona 1), segurando seu cajado com orbe d'água místico giratório e halo celestial.
4. **Inimigos & Fauna Abissal**:
   - Peixes Sombrios e Enguias em patrulha ondulante.
   - Cardumes de peixes procedurais ao redor dos recifes.
   - Medusas bioluminescentes flutuando em oscilação vertical suave.
5. **Guardiões de Atlantis**:
   - Estátuas colossais esculpidas em pedra ancestral e cobre com runas douradas nas laterais da via sacra.

### B. Arquitetura e Elementos de Cenário
1. **Chão Oceânico & Topografia 3D**: Terreno submarino com relevo de areia abissal e fendas rochosas.
2. **Ruínas & Templos de Atlantis**: Colunas caneladas (íntegras, partidas e tombadas), arquitraves, pórticos em arco, pedestais rúnicos e cristais de força atlante.
3. **Navio Naufragado**: Casco de galeão antigo em madeira envelhecida partido entre as rochas e cobertos de algas.
4. **Baús de Tesouro Ancestrais**: Baú entreaberto revelando moedas e joias cintilantes com feixes de luz emanando do interior.
5. **Recifes e Flora Submarina**: Corais-cérebro, colunas de coral-chifre, anêmonas luminescentes e florestas de algas ondulantes.
6. **Perigos e Obstáculos**: Gêiseres emitindo fluxos de partículas/bolhas animadas subindo até a superfície e minas de espinhos flutuantes.

### C. Atmosfera, Iluminação e Shaders Subaquáticos
- **Atmosfera Subaquática Profunda**: Gradiente de iluminação ambiente oceânica profunda.
- **God Rays / Cáusticas Subaquáticas**: Luzes com projeção cáustica simulando luz solar refratada pela superfície do mar sobre as ruínas.
- **Luzes Pontuais Bioluminescentes**: Cristais, olhos, orbes e lava hidrotermal criando alto contraste (Paleta 60-30-10 aplicada).
- **Névoa Volumétrica**: Shader de volume volumétrico para densidade e profundidade realista de água do oceano.

### D. Câmera e Animação Geral
- Linha do tempo configurada para visualização contínua com looping.
- **Câmera de Acompanhamento / Cinemática**: Câmera que acompanha a travessia de Adapa pelo cenário mostrando cada zona com profundidade de campo e transição no boss.

---

## 3. Estrutura dos Arquivos Criados

| Arquivo | Descrição |
|---|---|
| `scripts/fase_atlantis_apsu_3d.py` | Script Python Mestre que cria e orquestra a fase inteira, todos os personagens, props, iluminação, animações e renderiza os previews. |
| `blends/fase_atlantis_apsu/fase_atlantis_apsu_3d.blend` | Arquivo `.blend` completo e organizado em Coleções para edição e visualização no Blender 4.5. |
| `renders/fase_atlantis_apsu/` | Renders em alta definição dos principais momentos da fase. |

---

## 4. Plano de Verificação

1. **Execução Automatizada sem Interface Gráfica**:
   - Rodar o script via `blender.exe --background --python scripts/fase_atlantis_apsu_3d.py`.
   - Garantir execução sem erros de sintaxe ou de API do Blender 4.5.
2. **Renderização de Frames de Teste**:
   - Renderizar frames de demonstração (visão panorâmica e close de ação).
3. **Verificação de Coleções e Timeline**:
   - Garantir que o `.blend` contenha coleções limpas (`01_Heroi_Adapa`, `02_Boss_Kullullu`, `03_NPC_Enki`, `04_Inimigos`, `05_Ruinas_Atlantis`, `06_Flora_Recifes`, `07_Props_Tesouros`, `08_Perigos`, `09_Iluminacao_Mundo`, `10_Cameras`).
