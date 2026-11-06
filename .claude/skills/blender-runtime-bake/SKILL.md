---
name: blender-runtime-bake
description: Pipeline Blender para assets de runtime do jogo: atlas de materiais, decimação, frames de animação com mesma topologia, sprite sheets de projétil e efeitos, e export OBJ/MTL. Use em R03 a R05, G02, G06 e G07.
---
# Blender para o runtime

## Atlas e materiais
1. Unifique UV de todos os objetos do modelo em um atlas (1024x1024 para o herói).
2. Faça bake de cor, rugosidade e emissão para o atlas; mantenha rim light e SSS aprovados como texturas, não como shader.
3. Um material por atlas. Exporte com `Ke` apontando para mapa de emissão separado, nunca o diffuse (ver ARCHITECTURE.md).

## Decimação
- Modificador Decimate em modo colapso, razão calculada por `alvo / triângulos atuais`, depois triangule.
- Preserve silhueta: aplique em peças orgânicas, proteja olhos, tridente e coroa.
- Verifique normais depois (Shade Smooth com auto smooth por ângulo).

## Frames de animação
- Nado: 8 frames de um ciclo. Ataque: 6 frames (antecipação 2, disparo 2, recuperação 2).
- Todos os frames com contagem de vértices e UV idênticas. O script falha se divergir.
- Exporte `nome_frame_00.obj` a `nome_frame_07.obj` em pasta própria; o runtime carrega todos na inicialização.

## Projétil e efeitos
- Bolha: esfera de baixo poliedro, shader translúcido, rim light da paleta da fase.
- Sprite sheets: 8 frames de oscilação 64x64, 6 de impacto 96x96, 6 de flash 96x96, fundo transparente.
- Render por `make render-sprites`. Sem halo escuro: use alfa associado (premultiplied) na saída.

## Validação
```
python3 .claude/tools/mesh_budget.py --only hero
python3 .claude/tools/mesh_budget.py --only enemy --only boss
python3 .claude/tools/mesh_budget.py --only scenery
```
Comandos `make` de assets pedem confirmação. Nunca edite `.blend`, `.png` ou `.obj` à mão.
