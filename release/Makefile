ifeq ($(OS),Windows_NT)
PLATFORM_NAME := Windows
.PHONY: jogar
jogar:
	@echo Sistema detectado: $(PLATFORM_NAME)
	@powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$(dir $(abspath $(lastword $(MAKEFILE_LIST))))jogar-windows.ps1"
else
PLATFORM_NAME := $(shell uname -s)
.PHONY: jogar
jogar:
	@echo Sistema detectado: $(PLATFORM_NAME)
	@bash "$(dir $(abspath $(lastword $(MAKEFILE_LIST))))jogar.sh"
endif
