# Alvos auxiliares do harness APSU.
# Incluido no fim do Makefile do projeto por: -include tools/harness/harness.mk
# Nao redefine nenhum alvo existente e nao muda o alvo padrao (all).

HARNESS_PY ?= $(or $(PYTHON_CMD),python3)

.PHONY: harness-help harness-verify backlog-lint sprint-status

## Lista os alvos do harness
harness-help:
	@echo "make harness-verify  Verifica arquivos, JSON e comportamento dos hooks"
	@echo "make backlog-lint    Valida o backlog (Fibonacci, criterios, dependencias, capacidade)"
	@echo "make sprint-status   Mostra andamento por sprint e epico"

## Verifica a instalacao do harness
harness-verify:
	@$(HARNESS_PY) tools/harness/verify.py

## Valida docs/product/BACKLOG.md e SPRINTS.md
backlog-lint:
	@$(HARNESS_PY) tools/harness/backlog.py lint

## Relatorio de sprints
sprint-status:
	@$(HARNESS_PY) tools/harness/backlog.py report
	@$(HARNESS_PY) tools/harness/backlog.py next
