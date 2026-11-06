---
description: Registra baseline de cobertura JaCoCo e testes antes de iniciar uma sprint de performance
allowed-tools: Bash(mvn verify:*), Bash(mvn test:*), Bash(git status:*), Bash(git rev-parse:*), Bash(git branch:*), Bash(python3 .claude/tools/:*), Read
---
Execute os passos abaixo antes de qualquer modificação de código.

1. Confirmar que nao há mudanças nao comitadas:
```bash
git status
```
Se houver mudanças pendentes, informe o usuario antes de continuar.

2. Rodar verificação completa com JaCoCo:
```bash
mvn verify -q
```

3. Exibir resumo de cobertura por pacote com `python3 .claude/tools/perf_gate.py` se houver log disponivel, ou resumir o XML JaCoCo em `.claude/perf/baseline-coverage.txt`.

4. Registrar em `docs/perf-sprint-report.md`:
- Branch: resultado de `git rev-parse --abbrev-ref HEAD`
- Commit: resultado de `git rev-parse --short HEAD`
- Data: data atual
- Cobertura linha e branch por pacote
- Testes: N passed

5. Confirmar ao usuario: "Baseline registrado. Pronto para /story-start APSU-R0N."
