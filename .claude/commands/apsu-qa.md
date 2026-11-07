---
description: Prepara e preenche a checklist de QA de uma fase com evidência separada entre verificado e a jogar
argument-hint: [fase 1 a 5]
allowed-tools: Read, Grep, Glob, Write, Bash(make test), Bash(make coverage), Bash(git log:*)
---
Prepare o QA da fase $ARGUMENTS usando a checklist da seção "QA por fase" do README.

1. Rode `make test` e registre resultado e número de testes.
2. Para cada item da checklist, classifique: verificado por teste automatizado (cite o teste), verificado por leitura de código (cite arquivo), ou verificado jogando com `/apsu-aiplay` (cite a captura), ou depende de humano (áudio, sensação, GPU real).
3. Gere `docs/qa/fase-$ARGUMENTS.md` com: build/commit (git log -1), comandos rodados, tabela item x estado x evidência, e um roteiro de playtest manual com passos numerados, estado inicial e resultado esperado para os itens que dependem de jogar.
4. Não marque aprovado o que não foi verificado. Itens sem verificação ficam "a jogar".
