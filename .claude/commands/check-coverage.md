---
description: Verifica cobertura JaCoCo por pacote e lista classes abaixo do gate minimo
allowed-tools: Bash(mvn verify:*), Bash(python3 .claude/tools/:*), Read
---
1. Rodar verificação com JaCoCo:
```bash
mvn verify -q
```

2. Ler o XML em `target/site/jacoco/jacoco.xml` e exibir cobertura de linha e branch por pacote `br.apsu.*`.

3. Listar classes com cobertura de linha abaixo de 50%, indicando o arquivo de teste correspondente a criar ou expandir.

4. Se o gate falhar no `mvn verify`, a sprint nao pode ser finalizada: listar exatamente quais classes puxaram a cobertura para baixo e sugerir os testes minimos.
