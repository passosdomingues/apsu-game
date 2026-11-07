---
paths:
  - "personagens/**"
  - "assets/**"
  - "tools/assets/**"
  - "src/main/resources/**"
---
# Regras para assets e pipeline Blender

- Nunca exclua asset sem justificativa técnica explícita (regra 08). O harness pede confirmação para exclusões.
- Recursos empacotados em src/main/resources vêm de `make assets` ou de `make export-model MODEL=...`. Não edite binários à mão.
- `make assets-regenerate` e `make generate-characters` sobrescrevem fontes .blend; só com autorização do humano.
- Todo modelo novo declara: nome estável, fase, escala e unidade, orientação frontal e eixos, origem, material único quando possível e destino no jogo.
- Panoramas de fase são renders, não cenário 3D navegável. Não marque como implementado só porque o arquivo existe; confirme o uso no runtime.
