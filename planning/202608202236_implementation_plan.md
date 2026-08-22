# 🚀 Plano de Implementação — Assets 3D Ricos (Blender), Portal Atlante, Eliminação do Flicker & Paralelismo Inteligente MPI

Este plano atende integralmente ao feedback visual e de performance: substitui todas as formas geométricas/placeholders (incluindo o portal de círculo translúcido) por **modelos 3D renderizados via Blender**, elimina a causa-raiz do piscamento (*flicker*) nos sprites e introduz a **integração paralela inteligente com OpenMPI** para geração e balanceamento de mapa nos 8 núcleos da CPU.

---

## 🎯 Objetivos

1. **Criação do Modelo 3D do Portal Atlante (Blender 2.5D)**
   - Criar o script Python `personagens/scripts/15_portal_atlantica_3d.py` para gerar o Portal de Atlântida 3D (pórtico de obsidiana com inscrições rúnicas douradas e vórtice azul-safira).
   - Integrar no `gerador_mestre_apsu.py` e gerar a sequência de sprites 2.5D em `src/main/resources/sprites/scenery/15_portal_atlantica_3d/`.
   - Substituir o desenho procedurar antigo de `drawPortal` em `RenderEngine.java` pelo sprite 3D do Portal.

2. **Eliminação do Flicker e Correção de Inimigos/Cenários**
   - **Flicker em Cenários (Corais/Rochas/Ruínas):** Cenários 3D são estáticos e não devem ciclar frames como se fossem animação de personagens. O ciclo rápido de frames sem alinhamento causava o piscamento. `RenderEngine.java` passa a utilizar o sprite 3D estático de altíssima definição carregado diretamente.
   - **Inimigos sem Distorção:** Ajustar as proporções de aspect ratio exatas de cada tipo de inimigo (`PEIXE`, `ENGUIA`, `MEDUSA`, `CARANGUEJO`, `ARRAIAO`, `LEVIATA`) para renderizar com transparência alpha perfeita, sem formas geométricas secundárias ou retângulos de fallback.

3. **Paralelismo Inteligente com OpenMPI (`mpi/MapGenerator.c` & `MPIMapLoader.java`)**
   - Expandir `mpi/MapGenerator.c` para gerar mapas procedurais paralelos para as 5 fases do jogo em 8 threads de CPU (utilizando `MPI_Scatter` e `MPI_Gather`).
   - Exportar o mapa gerado para formato estruturado JSON/Text.
   - Criar a classe `br.apsu.core.map.MPIMapLoader` em Java para consumir o mapa gerado pelo OpenMPI e alimentar o layout de perigos, baús e inimigos no `GameContext.java`.

---

## 🛠️ Alterações Propostas

### 1. Pipeline 3D (Blender)

#### [NEW] [15_portal_atlantica_3d.py](file:///home/rafael/github/cache/grafica/Game/personagens/scripts/15_portal_atlantica_3d.py)
- Script Blender para construção da geometria do Portal Atlante 3D (arco de obsidiana + detalhes metálicos emissivos).

#### [MODIFY] [gerador_mestre_apsu.py](file:///home/rafael/github/cache/grafica/Game/personagens/scripts/gerador_mestre_apsu.py)
- Adicionar o novo script `15_portal_atlantica_3d.py` à lista de geração automática.

---

### 2. Paralelismo MPI & Integração Java

#### [MODIFY] [MapGenerator.c](file:///home/rafael/github/cache/grafica/Game/mpi/MapGenerator.c)
- Atualizar o gerador MPI para suporte às 5 fases e geração de matrizes de obstáculo/layout serializadas para arquivo.

#### [NEW] [MPIMapLoader.java](file:///home/rafael/github/cache/grafica/Game/src/main/java/br/apsu/core/map/MPIMapLoader.java)
- Carregador de layouts paralelos gerados via MPI para integrar a distribuição de inimigos e elementos no `GameContext`.

---

### 3. Motor de Renderização

#### [MODIFY] [RenderEngine.java](file:///home/rafael/github/cache/grafica/Game/src/main/java/br/apsu/graphics/RenderEngine.java)
- `drawPortal`: Renderizar o sprite 3D do Portal Atlante com halo bioluminescente.
- `drawOrganicCoral`, `drawP2`, `drawP3`, `drawP4`: Utilizar os sprites 3D estáticos sem variação de frame para eliminar 100% do flicker.
- `drawEnemy`: Garantir preservação estrita do aspect ratio da imagem 3D sem fallback para retângulos.

---

## 🧪 Plano de Verificação

### Automated Tests
- Executar `mvn test` para garantir aprovação de todos os testes unitários e de integração.

### Manual Verification
- Executar `make assets` para compilar o modelo 3D do Portal no Blender.
- Executar `make mpi-demo` para validar a execução do gerador paralelo MPI em 8 núcleos de CPU.
- Executar `mvn javafx:run` ou `make run` e verificar visualmente o novo Portal 3D, a fluidez de 60 FPS e a ausência total de piscamentos.
