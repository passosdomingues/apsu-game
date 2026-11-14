# Alvos auxiliares do harness APSU.
# Incluido no fim do Makefile do projeto por: -include tools/harness/harness.mk
# Nao redefine nenhum alvo existente e nao muda o alvo padrao (all).

HARNESS_PY ?= $(or $(PYTHON_CMD),python3)

.PHONY: harness-help harness-verify backlog-lint sprint-status fsm-cycle fsm-telemetry fsm-verify-patch seed

## Lista os alvos do harness
harness-help:
	@echo "make harness-verify   Verifica arquivos, JSON e comportamento dos hooks"
	@echo "make backlog-lint     Valida o backlog (Fibonacci, criterios, dependencias, capacidade)"
	@echo "make sprint-status    Mostra andamento por sprint e epico"
	@echo "make fsm-cycle STORY= Executa um ciclo da FSM deterministica para a historia"
	@echo "make fsm-telemetry    Exibe metricas de transicoes deterministas vs mediadas"
	@echo "make seed TARGET=     Semeia o harness APSU em outro diretorio ou projeto"

## Ciclo da FSM deterministica
fsm-cycle:
	@test -n "$(STORY)" || (echo "Uso: make fsm-cycle STORY=US-025" && exit 1)
	@$(HARNESS_PY) tools/harness/fsm.py cycle $(STORY)

## Telemetria de transicoes da FSM
fsm-telemetry:
	@$(HARNESS_PY) tools/harness/fsm.py telemetry

## Validacao de patch proposto por agente
fsm-verify-patch:
	@$(HARNESS_PY) tools/harness/fsm.py verify-patch

## Semeador Dandelion para outro projeto
seed:
	@test -n "$(TARGET)" || (echo "Uso: make seed TARGET=/caminho/do/outro/projeto" && exit 1)
	@$(HARNESS_PY) tools/harness/seed.py $(TARGET)

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
