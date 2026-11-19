# Walkthrough: Fase 3D de Atlantis ("As Águas de Apsu")

O cenário 3D completo da fase submersa de Atlantis foi construído proceduralmente em Python (`bpy`) e salvo no Blender 4.5, integrando todos os modelos, personagens, animações, shaders procedurais e iluminação aquática.

---

## Estrutura do Cenário 3D & Seções da Fase

O cenário possui 64 unidades de extensão e é dividido em 4 zonas interconectadas:

1. **Zona 1: Entrada dos Recifes & Santuário do Sábio Enki**:
   - Santuário com o sábio **Enki** segurando seu cajado com orbe mágico giratório.
   - Galeão naufragado tombado entre as rochas e baú de tesouro ancestral aberto com iluminação dourada.
   - Floresta de recifes de corais variados (chifre, cérebro), anêmonas luminescentes e kelp.
2. **Zona 2: A Grande Via Sacra de Atlantis**:
   - Colunatas monumentais em mármore azulado com capitéis mesopotâmicos dourados e grande arco quebrado.
   - Obeliscos com cristais de energia atlante bioluminescentes (ciano/teal).
   - Estátuas gigantes dos Guardiões Atlantes protegendo as entradas.
3. **Zona 3: Desfiladeiro Abissal de Perigos**:
   - Gêiseres hidrotermais (*black smokers*) expelindo colunas animadas de bolhas e magma quente.
   - Minas de espinhos defensivas flutuantes com luzes de alerta.
   - Peixes Sombrios e Medusas elétricas em rotas de patrulha.
4. **Zona 4: O Portal do Templo de Apsu & Covil do Boss Kullullû**:
   - O grandioso portal escuro onde o colossal **Kullullû** espreita com pele de obsidiana vulcânica, chifres pontiagudos e iluminação de magma sob seus pés.

---

## Animações e Dinâmica de Jogo

- **Herói Adapa**:
  - Ciclo contínuo de nado horizontal com ondulação senoidal da cauda segmentada e nadadeiras dorsais/caudais.
  - Trajetória de deslocamento 3D atravessando a fase inteira (Frames 1 a 240).
  - Feixe e projéteis de bolhas de água bioluminescentes à frente do tridente.
- **Boss Kullullû**:
  - Idle de respiração ameaçadora e flutuação nas profundezas.
- **Fauna & Cenário**:
  - Orbe de Enki girando em 360°, medusas pulsando verticalmente, peixes sombrios em patrulha senoidal e bolhas dos gêiseres subindo continuamente.
- **3 Câmeras Prontas**:
  - `Camera_Gameplay_25D`: Acompanha Adapa suavemente pela fase com efeito de parallax nas ruínas e recifes.
  - `Camera_Panoramica_Overview`: Visão aberta de cinema mostrando toda a extensão da cidade submersa.
  - `Camera_Boss_Confronto`: Enquadramento tenso no confronto final com o Boss Kullullû.

---

## Arquivos Relacionados

| Arquivo | Localização | Descrição |
|---|---|---|
| **Script Procedural da Fase** | `personagens/scripts/fase_atlantis_apsu_3d.py` | Código Python que gera a fase completa, shaders, iluminação e animações. |
| **Cena Blender 3D** | `personagens/blends/fase_atlantis_apsu_3d.blend` | Arquivo 3D completo do Blender 4.5 organizado em coleções no Outliner. |
| **Imagens Renderizadas** | `personagens/renders/` | Imagens em alta resolução capturando os setores da fase. |

---

## Como Visualizar e Reproduzir a Animação no Blender

1. Abra o Blender 4.5 e execute o script `personagens/scripts/fase_atlantis_apsu_3d.py`.
2. Pressione **Espaço** (Spacebar) para dar **Play** na timeline:
   - Você verá o **Adapa** nadando suavemente ondulando a cauda, disparando bolhas e avançando por toda a fase de Atlantis.
   - O orbe de **Enki** gira, os peixes e medusas patrulham e os gêiseres soltam bolhas.
3. Para trocar a câmera ativa para o modo Gameplay 2.5D:
   - Selecione a câmera `Camera_Gameplay_25D` e pressione `Ctrl + NumPad 0`.
