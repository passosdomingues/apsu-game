---
name: javafx-perf-budget
description: Regras de custo para JavaFX 21 no jogo: Canvas, SubScene 3D, MeshView, alocação no loop, culling e flags de pipeline. Use ao otimizar render, UI ou carga de modelos.
---
# Orçamento e custo em JavaFX

## Alocação no loop (regra 11)
- Fontes: `UiFonts` estático. Cores: `Palette` estática. Gradientes: gerar na carga e guardar como `Image` ou campo.
- Nada de `String.format`, `stream`, lambda, `new` em `update` e `render`. Para texto que muda pouco, guarde a `String` e só regenere quando o valor muda.
- Partículas e projéteis em pool com capacidade fixa e índice vivo.
- Marcação `// hot-alloc-ok` só com justificativa de raridade na mesma linha.

## Canvas
- Texto (`fillText`) é caro: pré-renderize rótulos estáticos em `Image`.
- Um `drawImage` por camada de parallax, sem escala em runtime. Imagem na altura final de 768 px.
- Dois Canvas empilhados (fundo e efeitos) custam menos que clipar e redesenhar tudo.

## 3D
- Custo por `MeshView` pesa mais que por triângulo: poucos materiais por ator, atlas de textura.
- Troque `MeshView.setMesh` para animar por frames, com topologia idêntica entre frames.
- `visible = false` e fora do grafo para o que está fora da janela da câmera mais 100 px.
- Instâncias compartilham `TriangleMesh` (`ObjModelLoader.instance`), variando tint por material.
- Carga de OBJ fora do thread FX. Pré-carregue o que a fase precisa antes do primeiro frame.

## Pipeline e JVM
- Confirmar o pipeline com `-Dprism.verbose=true`: es2 ou d3d, nunca sw no hardware de referência.
- Comparar flags em A/B com 60 s cada e `perf_gate`. Registrar a escolha em `docs/DEVELOPMENT.md`.

## Orçamento por frame (60 FPS = 16,7 ms)
- update até 2 ms, render até 9 ms, folga para o pulso do JavaFX.
