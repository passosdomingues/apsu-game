---
description: Gate completo de qualidade: compile, JUnit, JaCoCo e lint de hot path
allowed-tools: Bash(mvn compile:*), Bash(mvn test:*), Bash(mvn verify:*), Bash(python3 .claude/hooks/:*), Bash(grep:*), Read
---
Execute a sequência completa de verificações em ordem. Pare no primeiro passo que falhar.

1. Compilação: `mvn compile -q` - deve retornar BUILD SUCCESS.

2. Testes JUnit: `mvn test` - todos devem passar com 0 falhas.

3. Cobertura JaCoCo: `mvn verify -q` - gate de 50% linha / 30% branch.

4. Lint de hot path nos arquivos modificados (use `git diff HEAD --name-only` para listar):
```bash
echo '{"tool_name":"Edit","tool_input":{"file_path":"ARQUIVO"}}' | python3 .claude/hooks/hotpath_lint.py
```

5. Exibir tabela de resultado:
```
Compilação      OK / FALHOU
Testes JUnit    OK / N falhas
JaCoCo gate     OK / FALHOU
Hot path lint   OK / N violações
```

Se qualquer passo falhar, nao commitar e listar o que corrigir.
