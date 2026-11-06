---
description: Gera o corpo de Pull Request para uma sprint de performance com tabela de métricas
allowed-tools: Bash(git log:*), Bash(git diff:*), Bash(git branch:*), Bash(python3 .claude/tools/:*), Read
---
Gere um PR description preenchido para a sprint de performance concluida.

1. Listar commits da branch: `git log main..HEAD --oneline`
2. Listar arquivos modificados: `git diff main --name-only`
3. Ler metricas de `docs/perf-sprint-report.md` para preencher a tabela antes/depois.
4. Verificar o checklist de segurança antes de exibir:
   - `make test` verde
   - Nenhum arquivo fora do escopo da story foi modificado
   - Nenhuma alocação nova no hot path
   - Assets intocados

Formato de saída:

```
## APSU-RNN - [Título]

### Problema
[gargalo, arquivo e linha]

### Solução
[abordagem em 2-3 linhas]

### Arquivos modificados
| Arquivo | Tipo |
|---------|------|

### Métricas de performance
| Métrica | Antes | Depois | Delta |
|---------|-------|--------|-------|
| FPS médio | | | |
| p95 frame | | | |
| GC/min | | | |

### Checklist
- [ ] make test verde
- [ ] mvn verify verde
- [ ] Nenhuma alocação nova no hot path
- [ ] Assets intocados
- [ ] Conventional Commits
```
