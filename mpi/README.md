# Gerador Paralelo de Mapas (C + OpenMPI 4.x)

Este módulo implementa a geração procedural distribuída da malha do mapa de *As Águas de Apsu* utilizando a linguagem C e a biblioteca **OpenMPI 4.x**.

## Estrutura do Módulo

```text
mpi/
├── src/
│   └── MapGenerator.c    # Código-fonte principal do gerador distribuído
├── include/              # Cabeçalhos C (se aplicável)
├── generated/            # Layouts JSON gerados (phase-1.json a phase-5.json)
└── README.md
```

## Arquitetura de Comunicação Distribuída

O mapa do jogo é dividido em colunas verticais e distribuído entre os processos de CPU via primitivas de comunicação coletiva MPI:

- `MPI_Scatter`: Distribui os intervalos de cálculo de colunas entre os nós trabalhadores.
- `MPI_Gather`: Consolida a malha de mapa gerada em paralelo no processo raiz (Rank 0).

## Como Compilar e Executar

### Compilação Manual
```bash
mpicc -O2 -Wall -o mpi/map_gen mpi/src/MapGenerator.c
```

### Execução de Demonstração (8 Processos)
```bash
mpirun --oversubscribe -np 8 mpi/map_gen --phase 1
```

### Geração dos Layouts JSON de todas as Fases
```bash
for phase in 1 2 3 4 5; do
    mpirun --oversubscribe -np 8 mpi/map_gen --phase $phase --output mpi/generated/phase-$phase.json
done
```

### Automação via Makefile Principal
```bash
make mpi-demo       # Compila e roda a demonstração paralela
make mpi-generate   # Gera os 5 layouts JSON em mpi/generated/
```
