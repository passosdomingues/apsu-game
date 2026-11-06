---
description: Lista alocações e chamadas caras nos métodos quentes do jogo
argument-hint: [arquivo ou diretório, padrão src/main/java/br/apsu]
allowed-tools: Bash(python3 .claude/hooks/hotpath_lint.py), Bash(find:*), Read, Grep
---
Alvo: $ARGUMENTS (se vazio, `src/main/java/br/apsu`).

Para cada arquivo `.java` do alvo, execute `echo '{"tool_input":{"file_path":"<caminho relativo>"}}' | python3 .claude/hooks/hotpath_lint.py` e agregue as violações por arquivo e por tipo (`Font.font`, `Color`, `new`, lambda). Ordene pelos arquivos mais quentes (`RenderEngine`, `UIRenderer`, `LightingEngine`, `ParticleSystem`, `GameContext`). Não edite nada: entregue a tabela e a proposta de correção por tipo, ligada à APSU-R01.
