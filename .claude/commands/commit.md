---
description: Prepara um commit atômico em Conventional Commits
allowed-tools: Bash(git status:*), Bash(git diff:*), Bash(git add src/:*), Bash(git add tools/:*), Bash(git add .claude/:*), Bash(git add docs/:*), Bash(git commit:*), Bash(make test)
---
1. `git status` e `git diff`. Se houver mudanças de mais de uma intenção, proponha dividir em commits.
2. `make test` precisa estar verde.
3. Adicione apenas os arquivos da intenção, por caminho. Nunca `git add -A` nem `git add .`.
4. Mensagem: `<tipo>: <imperativo curto>` com tipo feat, fix, perf, refactor, docs, test, build, chore. Para tremor use `fix:`, para ganho de FPS use `perf:`. Corpo opcional com o número medido (por exemplo, "render 14,1 ms para 8,7 ms em P3").
5. Sem emojis. Execute o commit e mostre o hash.
