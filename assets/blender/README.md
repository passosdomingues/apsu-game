# Pipeline oficial de modelos 3D

As fontes editáveis ficam em `personagens/blends/` e `personagens/fases/`.
Personagens, inimigos, NPCs e elementos de gameplay são exportados como malhas
OBJ/MTL e carregados em `Runtime3DLayer`. Os panoramas de fase são renders de
cenas Blender; não são os modelos dos atores.

Use os alvos do Makefile:

```bash
make refresh-model MODEL=personagens/blends/.../arquivo.blend
make refresh-adapa-models
make render-backgrounds
make assets
```

`make render-3d-previews` gera imagens auxiliares em `personagens/renders/` para
revisão visual; previews não são recursos de gameplay. As ferramentas antigas
de sprite-sheet/GLB permanecem em revisão e não são chamadas pelo pipeline
oficial. O runtime usa malhas 3D, não PNGs desses previews.
