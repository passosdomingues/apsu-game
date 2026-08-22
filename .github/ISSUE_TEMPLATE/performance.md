---
name: Performance Issue
about: Proposta de otimização de frame-time, GC pressure ou uso de memória
title: '[PERF] '
labels: 'performance'
assignees: ''
---

## Gargalo de Performance Identificado
Descreva onde ocorre o gargalo (ex: colisão $O(N^2)$, alocação excessiva de objetos no `update`, consumo de VRAM).

## Métricas Atuais vs Esperadas
- **FPS Atual:** ex: 45 FPS em cenas com 100 projéteis
- **FPS Alvo:** 60 FPS cravados
- **Frame-time Alvo:** <= 16.6 ms

## Módulos Afetados
- [ ] Core Engine / FrameMetrics
- [ ] Gráficos / SpriteManager / LightingEngine
- [ ] Gameplay / Spatial QuadTree
- [ ] Pipeline Blender / Baking

## Critérios de Aceitação
- [ ] Otimização validada com `FrameMetricsTest`
- [ ] Nenhuma alteração no comportamento visual ou lógica de jogo
