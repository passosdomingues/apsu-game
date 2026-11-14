# Relatório de Desempenho e Validação de Renderização (US-009)

## 1. Contexto e Ambiente de Medição
- **Data**: 2026-11-14
- **Alvo**: Validação de estabilização gráfica e modo híbrido adaptativo (US-008 e US-009).
- **Ambiente de Teste**: CPU Linux x86_64, JavaFX 21.0.3, Mesa llvmpipe / software rasterizer (simulando GPU integrada de notebook sem aceleração de hardware dedicada).
- **Cenário de Teste**: Fase 2 ("Naufrágio e Cardumes Profundos"), com herói em navegação, 6 cardumes de inimigos abissais, 6 colunas de obstáculos de coral procedurais e props de cenario naufragado.

---

## 2. Comparativo de Desempenho na Fase 2 (2 minutos com APSU_PERF=1)

| Modo de Renderização | Pipeline Prism | FPS Médio | Pulse p50 | Pulse p95 | Pior Frame (max) | Frames > 33 ms | Frames > 50 ms |
|---|---|---|---|---|---|---|---|
| **3D Completo** (`apsu.quality=high`) | es2 (llvmpipe software) | ~10.4 fps | 94.2 ms | 108.5 ms | 134.0 ms | 98.2% | 98.2% |
| **Modo Híbrido** (`apsu.quality=low` / `hybrid`) | es2 (llvmpipe software) | ~34.8 fps | 26.5 ms | 31.4 ms | 44.2 ms | 3.1% | **0.0%** |
| **2D Completo Fallback** (`-Dapsu.render=2d`) | sw (software Prism) | ~36.2 fps | 24.8 ms | 29.8 ms | 41.5 ms | 1.8% | **0.0%** |

---

## 3. Conclusões e Análise
1. **Redução Drástica de Gargalo de Rasterização**:
   - No modo 3D completo em software raster, renderizar dezenas de malhas poligonais de inimigos e props consome quase 100 ms por frame.
   - No **Modo Híbrido Adaptativo**, mantendo apenas o herói e guardião na SubScene 3D enquanto inimigos e props utilizam Canvas 2D, o pulso p95 cai de 108.5 ms para **31.4 ms** (uma melhoria de ~71% em latência de renderização).
2. **Estabilidade de Quadros**:
   - No modo híbrido, **nenhum quadro ultrapassou 50 ms** durante o playtest de 2 minutos da fase 2.
3. **Comportamento Adaptativo e Histerese**:
   - Caso um sistema com 3D completo apresente p95 > 33 ms por 10 segundos contínuos, o `AdaptiveRenderController` migra automaticamente os elementos secundários para 2D, registrando `[GFX]`.
   - O mecanismo de histerese impede o retorno prematuro a 3D completo antes da troca de fase, eliminando artefatos de flickering ou oscilação de taxa de quadros.
