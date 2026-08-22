# Descrição Técnica de Aplicação dos Conceitos de Computação Gráfica

## 1. Finalidade

Este documento descreve como os conceitos de Computação Gráfica estudados na disciplina são aplicados em uma pipeline prática de produção de assets tridimensionais, renderização e desenvolvimento de um jogo.

O projeto utiliza o Blender como ambiente central de produção e estabelece uma relação explícita entre fundamentos teóricos e artefatos verificáveis: modelos 3D, transformações, câmeras, curvas e superfícies, materiais, texturas, iluminação, renders, animações, assets otimizados e integração com uma engine.

O objetivo não é apenas produzir imagens ou modelos visualmente adequados, mas demonstrar a aplicação técnica dos conceitos estudados e sua relação com uma pipeline gráfica real.

---

## 2. Pipeline Técnica Geral

```text
Conceito / Game Design
        ↓
Blockout
        ↓
Modelagem poligonal e/ou procedural
        ↓
Transformações geométricas
        ↓
UV + materiais + texturas
        ↓
Iluminação + câmera
        ↓
Renderização
   ┌────┴────┐
   ↓         ↓
Rasterização Ray/Path Tracing
   └────┬────┘
        ↓
Pós-processamento / Compositor
        ↓
Otimização + Bake
        ↓
Exportação FBX/glTF
        ↓
Integração com engine
        ↓
Level Design + Interatividade
        ↓
Playtesting + Validação
```

Cada etapa produz artefatos que podem ser inspecionados e utilizados como evidência da aplicação dos conceitos da disciplina. fileciteturn0file0L15-L45

---

# 3. Aplicação dos Conceitos

## 3.1 Processamento Gráfico, Imagens e Modelagem

### Conceitos

- Computação gráfica
- Processamento de imagens
- Modelagem tridimensional
- Renderização
- Composição de imagens

### Aplicação

A modelagem é utilizada para produzir personagens, objetos e ambientes tridimensionais. A computação gráfica aparece na transformação desses dados geométricos em imagens por meio da pipeline de renderização.

O processamento de imagens participa da preparação de texturas, geração e tratamento de mapas, composição e pós-processamento.

A visão computacional deve ser tratada como uma aplicação complementar, por exemplo em captura de referência, reconstrução ou tracking. Ela não é necessária para a pipeline principal de modelagem e renderização.

### Evidências

- Malhas 3D.
- Texturas.
- Mapas derivados.
- Renders.
- Composições finais.

---

## 3.2 Hardware Gráfico e Desempenho

### Conceitos

- CPU
- GPU
- VRAM
- Memória
- Dispositivos de entrada
- Desempenho gráfico

### Aplicação

CPU e GPU participam de diferentes etapas da produção e renderização. A capacidade de VRAM influencia a complexidade das cenas, resolução das texturas e possibilidade de utilizar determinados recursos de renderização.

A otimização deve considerar:

- quantidade de polígonos;
- resolução e quantidade de texturas;
- quantidade de materiais;
- custo de renderização;
- quantidade de objetos;
- reutilização e instanciamento de assets.

### Evidências

- Comparação entre configurações de render.
- Medições de tempo de renderização.
- Comparação entre versões otimizada e não otimizada.
- Estatísticas de geometria e memória.

---

## 3.3 Pipeline Gráfica e APIs

### Conceitos

- Pipeline gráfica
- Vértices
- Primitivas
- Transformações
- Rasterização
- Shaders
- APIs gráficas

### Aplicação

A pipeline gráfica pode ser compreendida a partir do fluxo entre geometria, transformações, processamento de vértices, rasterização e geração dos pixels.

APIs como OpenGL, Vulkan, DirectX e Metal são responsáveis pela comunicação entre aplicações e recursos gráficos do sistema. O Blender abstrai grande parte dessa implementação, mas a viewport e a renderização permitem observar os resultados da pipeline.

No contexto de jogos, essa compreensão é importante para relacionar geometria, materiais, iluminação, shaders e desempenho.

### Evidências

- Cena renderizada.
- Visualização em tempo real.
- Comparação entre diferentes técnicas de renderização.
- Asset importado na engine.

---

# 4. Modelagem Poligonal

## 4.1 Conceitos

- Vértices
- Arestas
- Faces
- Malhas
- Topologia
- Resolução
- Refinamento
- Simplificação

## 4.2 Aplicação

Personagens, inimigos, bosses, props e elementos de cenário são construídos como malhas poligonais.

O processo começa com blockout e primitivas geométricas. A geometria é posteriormente refinada por operações de edição e modificadores.

A densidade da malha deve ser determinada pelo uso do asset. Assets próximos da câmera podem exigir maior resolução, enquanto objetos secundários devem priorizar eficiência.

### Ferramentas utilizadas

- Extrude
- Inset
- Loop Cut
- Bevel
- Bridge Edge Loops
- Merge
- Mirror
- Subdivision Surface
- Solidify
- Remesh
- Decimate

### Critério de validação

A malha deve apresentar topologia compatível com sua finalidade, deformação adequada quando animada e complexidade proporcional à distância e importância do objeto na cena.

---

# 5. Transformações Geométricas

## 5.1 Conceitos

- Translação
- Rotação
- Escala
- Transformações rígidas
- Coordenadas homogêneas
- Transformações compostas
- Hierarquia

## 5.2 Aplicação

Cada objeto possui uma transformação espacial. A combinação de translação, rotação e escala permite posicionar e organizar os elementos da cena.

A hierarquia permite que objetos sejam relacionados entre si, possibilitando, por exemplo, que partes de um personagem acompanhem um osso ou que componentes de um objeto sejam movimentados em conjunto.

As coordenadas homogêneas fornecem uma representação matemática unificada para transformações geométricas e projeções.

### Uso

- Definição de origem.
- Posicionamento.
- Rotação.
- Escala.
- Parenting.
- Bones.
- Constraints.
- Aplicação de transformações antes da exportação quando necessário.

### Critério de validação

O asset deve manter escala, orientação, origem e hierarquia consistentes entre Blender e engine.

---

# 6. Câmeras e Visualização

## Conceitos

- Projeção perspectiva
- Projeção ortográfica
- Transformação de vista
- Clipping
- Visibilidade
- Rasterização

## Aplicação

A câmera determina a transformação entre o espaço da cena e a representação visual observada pelo usuário.

A projeção perspectiva reproduz a redução aparente de objetos com a distância. A projeção ortográfica elimina essa relação de escala e pode ser útil em determinadas interfaces e estilos de jogo.

O clipping determina os limites de profundidade considerados pela câmera. Técnicas de visibilidade reduzem o processamento de elementos que não contribuem para a imagem final.

### Uso

- Câmeras de gameplay.
- Câmeras cinematográficas.
- Perspectiva.
- Ortográfica.
- Distância focal.
- Clipping.
- Enquadramento.

### Critério de validação

A câmera deve produzir uma composição visual coerente e, no jogo, preservar legibilidade e jogabilidade.

---

# 7. Cenas Gráficas

## Conceitos

- Geometria
- Materiais
- Texturas
- Iluminação
- Normais
- Composição

## Aplicação

Uma cena gráfica resulta da combinação entre geometria, aparência, iluminação e câmera.

A qualidade visual depende da relação entre esses elementos, e não apenas da qualidade individual dos modelos.

### Uso

- Montagem de cenários.
- Materiais.
- UV.
- Texturas.
- Iluminação.
- Normais.
- Compositor.

### Critério de validação

A cena deve apresentar coerência espacial, iluminação consistente e leitura visual adequada.

---

# 8. Curvas e Superfícies

## Conceitos

- Curvas paramétricas
- Curvas de Bézier
- B-Splines
- NURBS
- Interpolação
- Continuidade C0, C1 e C2
- Superfícies paramétricas
- Varredura
- Revolução

## Aplicação

Curvas são utilizadas quando a forma depende de um caminho controlável. São adequadas para cabos, fios, trilhos, caminhos, caudas, tubos e outros elementos orgânicos ou técnicos.

A varredura permite deslocar um perfil ao longo de uma trajetória. A revolução permite gerar uma superfície pela rotação de um perfil em torno de um eixo.

### Uso

- Curvas Bézier.
- Bevel de curvas.
- Caminhos.
- Tubos.
- Varredura.
- Revolução.
- Conversão de curvas em malha.

### Critério de validação

A forma resultante deve apresentar continuidade e controle geométrico compatíveis com a finalidade do asset.

---

# 9. Cor e Espaços de Cor

## Conceitos

- RGB
- HSV
- Saturação
- Valor
- Contraste
- Temperatura de cor
- Espaços de cor
- Color management

## Aplicação

A cor possui função estética e funcional. Ela pode estabelecer identidade visual, separar categorias de objetos e melhorar a legibilidade do jogo.

Também é necessário distinguir cor destinada à aparência de dados utilizados por shaders. Mapas como normal, roughness e metallic não devem ser tratados da mesma maneira que uma textura de cor.

### Uso

- Paleta visual.
- Materiais.
- Color management.
- Correção de cor.
- Configuração dos espaços de cor das texturas.

### Critério de validação

As cores devem permanecer consistentes ao longo da pipeline de produção, renderização e exportação.

---

# 10. Rasterização e Ray/Path Tracing

## 10.1 Rasterização

A rasterização transforma primitivas geométricas em fragmentos que podem contribuir para os pixels da imagem. É fundamental em pipelines de renderização em tempo real devido à sua eficiência.

## 10.2 Ray Tracing / Path Tracing

O ray tracing determina a interação entre raios e superfícies. A partir dessas interações podem ser calculados fenômenos como sombras, reflexão e refração.

O path tracing utiliza múltiplas amostras e trajetórias de luz para aproximar a iluminação global.

No Blender, o Cycles utiliza path tracing. A comparação com uma pipeline de tempo real permite demonstrar o trade-off entre qualidade física aproximada e custo computacional.

### Evidências

- Render em tempo real.
- Render com path tracing.
- Comparação de qualidade.
- Comparação de tempo de processamento.
- Uso de denoising.

### Critério de validação

O relatório deve identificar explicitamente qual técnica foi utilizada em cada resultado, evitando tratar rasterização e ray tracing como etapas equivalentes da mesma pipeline.

---

# 11. Animação

## Conceitos

- Keyframes
- Interpolação
- Timing
- Hierarquia
- Rigging
- Bones
- Shape Keys
- Estados de animação

## Aplicação

Personagens podem possuir estados como:

- Idle.
- Caminhada.
- Corrida.
- Ataque.
- Dano.
- Morte.

O rigging estabelece uma estrutura de controle. Os keyframes registram transformações ao longo do tempo, enquanto a interpolação determina o movimento entre os estados.

### Critério de validação

As animações devem funcionar isoladamente no Blender e, posteriormente, ser verificadas após a importação na engine.

---

# 12. Blender como Ambiente de Produção

O Blender funciona como ferramenta central da pipeline porque reúne modelagem, UV, materiais, texturização, iluminação, animação, renderização e composição.

A ferramenta não substitui os fundamentos da Computação Gráfica. Ela fornece uma abstração operacional sobre eles.

Essa distinção é importante academicamente: o software é o meio de aplicação; os conceitos estudados são os objetos de análise.

---

# 13. Otimização e Bake

## Conceitos

- Low poly
- High poly
- Normal map
- Ambient occlusion
- Bake
- Draw calls
- Instanciamento
- Reutilização de materiais

## Aplicação

Detalhes geométricos de uma versão high poly podem ser transferidos para uma versão low poly por meio de mapas, reduzindo o custo de processamento durante a execução em tempo real.

A otimização deve considerar o conjunto da cena, não apenas a contagem de polígonos.

### Estratégias

- Redução de geometria.
- Bake de detalhes.
- Normal maps.
- Ambient occlusion.
- Reutilização de materiais.
- Instanciamento.
- Redução de texturas desnecessárias.
- Controle de resolução.

### Critério de validação

Comparar visualmente e tecnicamente a versão original com a versão otimizada, registrando a redução de custo e a eventual perda visual.

---

# 14. Exportação e Integração

## Conceitos

- FBX
- glTF
- Escala
- Sistemas de coordenadas
- Orientação
- Materiais
- Texturas
- Animações
- Colisores

## Aplicação

A exportação é uma etapa de interoperabilidade entre o software de produção e o motor de jogos.

Devem ser verificados:

- escala;
- orientação dos eixos;
- origem;
- hierarquia;
- materiais;
- texturas;
- animações;
- nomes;
- colisores.

Problemas de escala, orientação ou transformação devem ser detectados antes de considerar o asset validado.

### Critério de validação

Um asset só deve ser considerado concluído após ser importado na engine e testado no contexto real de uso.

---

# 15. Level Design, Interatividade e Playtesting

## Conceitos

- Blockout
- Modularidade
- Escala
- Fluxo
- Interatividade
- Polimento
- Playtesting

## Aplicação

O blockout permite validar escala e fluxo antes do detalhamento.

Kits modulares permitem construir ambientes maiores reutilizando componentes. Isso reduz o número de assets únicos e facilita manutenção.

A interatividade acrescenta colisores, triggers, navegação, comportamento de inimigos e respostas às ações do jogador.

O playtesting valida o resultado que não pode ser inferido apenas pelo arquivo do Blender: compreensão da fase, dificuldade, legibilidade e comportamento durante a execução.

---

# 16. Entregáveis e Evidências

| Entregável | Conceitos demonstrados | Evidência |
|---|---|---|
| Personagem | Modelagem, transformação, materiais, rigging | Arquivo 3D + render + animação |
| Inimigo/Boss | Modelagem, hierarquia, animação | Asset + animações |
| Props | Modelagem e materiais | Assets individuais |
| Cenário | Transformações, modularidade, iluminação | Cena modular |
| Curvas | Bézier, varredura, superfícies | Exemplos geométricos |
| Render | Câmera, iluminação, ray/path tracing | Render + configurações |
| Asset otimizado | Low poly, bake, normal map | High poly vs low poly |
| Game | Integração, câmera, interatividade | Build executável |
| Fase | Level design e modularidade | Cena jogável |
| Teste | Validação | Registro de testes |

---

# 17. Matriz de Rastreabilidade

| Unidade | Conceito | Aplicação | Evidência |
|---|---|---|---|
| I | Processamento gráfico e hardware | GPU, CPU, VRAM e desempenho | Medições e configuração |
| II | Bibliotecas gráficas | Pipeline e APIs | Renderização/engine |
| III | Modelagem e transformações | Personagens, props e cenários | Assets |
| IV | Câmeras e visualização | Projeção, clipping e rasterização | Câmeras e renders |
| V | Cenas gráficas | Materiais, texturas e iluminação | Cena final |
| VI | Curvas e superfícies | Bézier, B-Spline, varredura e revolução | Modelos |
| VII | Ray tracing | Path tracing, sombras e reflexos | Render comparativo |
| VIII | Modelagem 3D e games | Bake, exportação, engine e level design | Jogo |

---

# 18. Especificação Técnica dos Assets

## 18.1 Escala

- Utilizar unidade métrica de forma consistente.
- Manter transformações coerentes.
- Aplicar escala quando necessário antes da exportação.
- Definir origem de forma compatível com a função do objeto.

## 18.2 Organização

Prefixos sugeridos:

- `CH_`: personagens.
- `EN_`: inimigos.
- `PR_`: props.
- `SM_`: módulos de cenário.
- `MT_`: materiais.
- `TX_`: texturas.

## 18.3 Modelagem

- Topologia adequada à função.
- Edge loops em áreas deformáveis.
- Evitar geometria desnecessária.
- Priorizar simplicidade em assets secundários.
- Controlar modificadores destrutivos.

## 18.4 UV e Texturas

- UV consistente.
- Densidade de texel adequada.
- Atlas quando houver benefício.
- Resolução proporcional à importância do asset.

## 18.5 Materiais

- Utilizar materiais PBR quando apropriado.
- Evitar materiais únicos desnecessários.
- Nomear mapas de forma consistente.
- Diferenciar texturas de cor de mapas de dados.

## 18.6 Animação

- Ações nomeadas.
- Bones necessários apenas.
- Ciclos reutilizáveis.
- Teste na engine.

## 18.7 Exportação

- Conferir escala.
- Conferir orientação.
- Conferir origem.
- Exportar somente o necessário.
- Testar importação.

---

# 19. Critérios Técnicos de Validação

Um asset ou cena será considerado validado quando:

- [ ] A geometria corresponde à finalidade do objeto.
- [ ] As transformações estão corretas.
- [ ] A escala é consistente.
- [ ] A origem está adequada.
- [ ] Os materiais funcionam corretamente.
- [ ] As texturas utilizam o tratamento de cor adequado.
- [ ] A câmera produz a projeção esperada.
- [ ] A iluminação é coerente.
- [ ] A técnica de renderização está identificada.
- [ ] O asset otimizado mantém qualidade visual aceitável.
- [ ] O bake funciona corretamente.
- [ ] A exportação não altera indevidamente escala, orientação ou animação.
- [ ] O asset é importado corretamente na engine.
- [ ] A interação funciona no contexto do jogo.
- [ ] O desempenho é aceitável para o objetivo definido.

---

# 20. Critérios de Avaliação do Projeto

Além da qualidade visual, o projeto deve ser avaliado pela capacidade de demonstrar a relação entre teoria e implementação.

Os principais critérios são:

1. **Rastreabilidade:** cada conceito relevante deve possuir uma aplicação identificável.
2. **Evidência:** cada aplicação deve gerar um artefato observável.
3. **Reprodutibilidade:** a pipeline deve poder ser executada novamente.
4. **Interoperabilidade:** os assets devem sobreviver à passagem Blender → engine.
5. **Desempenho:** as decisões de otimização devem possuir justificativa técnica.
6. **Validação:** resultados devem ser testados no contexto de uso.
7. **Coerência:** nomenclatura, escala, materiais e organização devem permanecer consistentes.

---

# 21. Conclusão

O projeto constitui uma aplicação integrada dos fundamentos de Computação Gráfica. Modelagem e topologia produzem a geometria; transformações posicionam e organizam os objetos; curvas e superfícies permitem representar formas controladas; materiais, texturas e cor definem a aparência; iluminação e câmeras determinam a composição; rasterização e ray/path tracing produzem imagens; animação introduz a dimensão temporal; otimização e bake adaptam os assets ao tempo real; e a exportação conecta a produção gráfica ao motor de jogos.

A principal característica técnica do projeto é a rastreabilidade entre conceito, implementação e evidência. Dessa forma, o resultado deixa de ser apenas uma produção visual e passa a constituir uma demonstração verificável da aplicação dos fundamentos da disciplina em uma pipeline de produção gráfica e desenvolvimento de jogos.
