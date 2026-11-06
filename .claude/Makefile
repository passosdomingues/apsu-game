.PHONY: check test sprints help

help:
	@echo "Comandos disponiveis em .claude:"
	@echo "  make check    - Valida a integridade do harness e executa os testes dos hooks"
	@echo "  make test     - Executa apenas os testes unitarios dos hooks"
	@echo "  make sprints  - Valida e resume as stories e sprints planejadas"

check: test
	python3 tools/validate_harness.py
	python3 tools/sprint_report.py --quiet

test:
	python3 tests/test_hooks.py

sprints:
	python3 tools/sprint_report.py
