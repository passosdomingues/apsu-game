/*
 * ============================================================
 * MapGenerator.c — Gerador Paralelo de Mapa com MPI
 * ============================================================
 * Demonstração de paralelismo usando os 8 cores do i7-8565U
 * (4 cores físicos / 8 threads via Hyper-Threading)
 *
 * COMPILAR:  mpicc -O2 -Wall -o map_gen MapGenerator.c
 * EXECUTAR:  mpirun -np 8 ./map_gen --phase 1 --output phase-1.json
 * via Make:  make mpi-demo
 *
 * PADRÃO MPI USADO:
 *   Processo 0 (master): Divide o mapa em N faixas horizontais
 *   Processos 1..N-1:    Cada um gera tiles para sua faixa
 *   MPI_Scatter  → distribui coordenadas de faixa para workers
 *   MPI_Gather   → coleta tiles gerados de todos os workers
 *   Processo 0:  Consolida, exibe e serializa o mapa completo em JSON
 *
 * INTEGRAÇÃO COM O JOGO:
 *   Utilitário independente. Gera configuração de nível que
 *   pode ser lida pelo ApsuGame.java (Sprint 4+).
 * ============================================================
 */

#include <mpi.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <math.h>

/* ── Configuração do Mapa ── */
#define MAP_WIDTH      96    /* tiles de largura (divisível por 8) */
#define MAP_HEIGHT     16    /* tiles de altura */
#define PHASE_COUNT    5
#define TILE_WATER     '~'
#define TILE_CORAL     'C'
#define TILE_ROCK      '#'
#define TILE_CHEST     '$'
#define TILE_TABLET    'T'
#define TILE_EMPTY     '.'
#define TILE_ENEMY     'E'
#define TILE_PORTAL    'P'

/* ── Estrutura de Faixa (enviada via MPI_Scatter) ── */
typedef struct {
    int start_col;   /* coluna inicial desta faixa */
    int end_col;     /* coluna final desta faixa (exclusive) */
    int phase;       /* fase do jogo: 1=Águas Claras, 2=Cavernas, 3=Templo */
    int seed;        /* semente aleatória única por processo */
} MapStrip;

/* ── Protótipos ── */
void generate_strip(MapStrip strip, char *out_tiles);
void print_map(char *map, int width, int height, int rank);
int write_json(const char *path, char *map, int phase);
char get_phase_tile(int col, int row, MapStrip strip, unsigned int *seed);
unsigned int lcg_rand(unsigned int *state);

/* ============================================================
 * MAIN
 * ============================================================ */
int main(int argc, char *argv[]) {

    int rank, size;

    /* Inicializar MPI — sempre a primeira chamada em qualquer programa MPI */
    MPI_Init(&argc, &argv);
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);  /* ID deste processo (0..N-1) */
    MPI_Comm_size(MPI_COMM_WORLD, &size);  /* Total de processos */

    int requested_phase = 1;
    const char *output_path = NULL;
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "--phase") == 0 && i + 1 < argc) requested_phase = atoi(argv[++i]);
        else if (strcmp(argv[i], "--output") == 0 && i + 1 < argc) output_path = argv[++i];
    }
    if (requested_phase < 1 || requested_phase > PHASE_COUNT) {
        if (rank == 0) fprintf(stderr, "[ERRO] --phase deve estar entre 1 e %d\n", PHASE_COUNT);
        MPI_Finalize();
        return 1;
    }

    /* Validar número de processos */
    if (MAP_WIDTH % size != 0) {
        if (rank == 0) {
            fprintf(stderr,
                "[ERRO] MAP_WIDTH (%d) deve ser divisível por np (%d)\n"
                "       Use: mpirun -np 8 ./map_gen\n",
                MAP_WIDTH, size);
        }
        MPI_Finalize();
        return 1;
    }

    int cols_per_process = MAP_WIDTH / size;
    int strip_tiles = cols_per_process * MAP_HEIGHT;

    /* ── Processo 0: Preparar e distribuir faixas ── */
    MapStrip *all_strips = NULL;
    char     *full_map   = NULL;

    if (rank == 0) {
        printf("\n");
        printf("╔══════════════════════════════════════════════════╗\n");
        printf("║   As Águas de Apsu — Gerador de Mapa Paralelo   ║\n");
        printf("╠══════════════════════════════════════════════════╣\n");
        printf("║  Processos MPI: %-3d   Mapa: %dx%d tiles           ║\n",
               size, MAP_WIDTH, MAP_HEIGHT);
        printf("║  Faixa/processo: %d colunas × %d linhas           ║\n",
               cols_per_process, MAP_HEIGHT);
        printf("╚══════════════════════════════════════════════════╝\n\n");

        /* Alocar faixas para todos os processos */
        all_strips = (MapStrip *)malloc(size * sizeof(MapStrip));
        full_map   = (char *)malloc(MAP_WIDTH * MAP_HEIGHT * sizeof(char));

        /* Configurar cada faixa com sua posição e fase */
        for (int i = 0; i < size; i++) {
            all_strips[i].start_col = i * cols_per_process;
            all_strips[i].end_col   = (i + 1) * cols_per_process;

            /* Cada execução produz uma fase inteira, não trechos misturados. */
            all_strips[i].phase = requested_phase;

            /* Semente única por processo para variedade */
            all_strips[i].seed = (int)(time(NULL)) ^ (i * 0x9e3779b9 + 0x6c62272e);
        }
    }

    /* ── MPI_Scatter: Distribuir uma faixa para cada processo ── */
    MapStrip my_strip;
    MPI_Scatter(
        all_strips,           /* dados no root (processo 0) */
        sizeof(MapStrip),     /* tamanho de cada elemento enviado */
        MPI_BYTE,             /* tipo MPI: bytes brutos */
        &my_strip,            /* buffer de recepção neste processo */
        sizeof(MapStrip),     /* tamanho do buffer de recepção */
        MPI_BYTE,
        0,                    /* rank do root */
        MPI_COMM_WORLD
    );

    /* ── Cada processo gera sua faixa ── */
    char *my_tiles = (char *)malloc(strip_tiles * sizeof(char));
    generate_strip(my_strip, my_tiles);

    printf("[Core %d / PID %-6d] ⚙  Gerou faixa colunas %2d–%2d  "
           "(Fase %d: %s)\n",
           rank,
           (int)MPI_Wtime(),  /* timestamp simplificado */
           my_strip.start_col,
           my_strip.end_col - 1,
           my_strip.phase,
           my_strip.phase == 1 ? "Águas Claras  " :
           my_strip.phase == 2 ? "Cavernas Coral" :
                                 "Templo Submerso");

    /* Garantir que todos os processos terminaram antes de coletar */
    MPI_Barrier(MPI_COMM_WORLD);

    /* ── MPI_Gather: Coletar tiles de todos os processos no root ── */
    /*
     * Nota: MPI_Gather coleta dados em ordem de rank.
     * Cada processo envia strip_tiles chars.
     * Processo 0 recebe size * strip_tiles chars no full_map.
     *
     * Porém, o mapa está em "column-major strips" — precisamos
     * reordenar para row-major no final.
     */
    char *gathered = NULL;
    if (rank == 0) {
        gathered = (char *)malloc(MAP_WIDTH * MAP_HEIGHT * sizeof(char));
    }

    MPI_Gather(
        my_tiles,      /* dados enviados por este processo */
        strip_tiles,   /* quantidade de elementos enviados */
        MPI_CHAR,
        gathered,      /* buffer de recepção (só usado no root) */
        strip_tiles,   /* quantidade de elementos por processo */
        MPI_CHAR,
        0,             /* rank do root */
        MPI_COMM_WORLD
    );

    /* ── Processo 0: Reorganizar e exibir o mapa ── */
    if (rank == 0) {
        /*
         * Reorganização: gathered está como [strip0_row0..rowN, strip1_row0..rowN, ...]
         * Precisamos de:  full_map[row * MAP_WIDTH + col]
         *
         * gathered layout: [proc_id * strip_tiles + row * cols_per_proc + local_col]
         */
        for (int p = 0; p < size; p++) {
            int col_offset = p * cols_per_process;
            for (int row = 0; row < MAP_HEIGHT; row++) {
                for (int lc = 0; lc < cols_per_process; lc++) {
                    int src = p * strip_tiles + row * cols_per_process + lc;
                    int dst = row * MAP_WIDTH + col_offset + lc;
                    full_map[dst] = gathered[src];
                }
            }
        }

        printf("\n");
        print_map(full_map, MAP_WIDTH, MAP_HEIGHT, rank);

        if (output_path != NULL && !write_json(output_path, full_map, requested_phase)) {
            fprintf(stderr, "[ERRO] Não foi possível gravar %s\n", output_path);
        }

        /* Legenda */
        printf("\nLegenda:\n");
        printf("  %c = Água (background)    %c = Coral (obstáculo)\n",
               TILE_WATER, TILE_CORAL);
        printf("  %c = Rocha (parede)       %c = Baú (easter egg)\n",
               TILE_ROCK, TILE_CHEST);
        printf("  %c = Tabuleta             %c = Inimigo\n",
               TILE_TABLET, TILE_ENEMY);
        printf("  %c = Portal de fase       %c = Vazio\n",
               TILE_PORTAL, TILE_EMPTY);
        printf("\n✓ Mapa %dx%d gerado por %d processos MPI em paralelo.\n\n",
               MAP_WIDTH, MAP_HEIGHT, size);

        free(all_strips);
        free(full_map);
        free(gathered);
    }

    free(my_tiles);

    /* Finalizar MPI — sempre a última chamada */
    MPI_Finalize();
    return 0;
}

/* ============================================================
 * GENERATE STRIP
 * Cada processo chama esta função para sua faixa de colunas.
 * Usa LCG (Linear Congruential Generator) para reprodutibilidade.
 * ============================================================ */
void generate_strip(MapStrip strip, char *out_tiles) {
    int cols = strip.end_col - strip.start_col;
    unsigned int rng_state = (unsigned int)strip.seed;

    for (int row = 0; row < MAP_HEIGHT; row++) {
        for (int lc = 0; lc < cols; lc++) {
            int global_col = strip.start_col + lc;
            out_tiles[row * cols + lc] = get_phase_tile(
                global_col, row, strip, &rng_state
            );
        }
    }
}

/* ============================================================
 * GET_PHASE_TILE
 * Retorna o tipo de tile baseado na posição e fase do jogo.
 * Simula a geração de nível do ApsuGame.
 * ============================================================ */
char get_phase_tile(int col, int row, MapStrip strip, unsigned int *rng) {
    int h = MAP_HEIGHT;
    int w = MAP_WIDTH;
    float norm_col = (float)col / w;

    /* Bordas sempre sólidas */
    if (row == 0 || row == h - 1) {
        return (strip.phase == 2) ? TILE_CORAL : TILE_WATER;
    }

    /* Portal de transição de fase */
    if ((norm_col > 0.33f && norm_col < 0.34f) ||
        (norm_col > 0.67f && norm_col < 0.68f)) {
        if (row == h / 2) return TILE_PORTAL;
    }

    /* Tabuletas — colocadas nos 3/4 do mapa em cada fase */
    if (col == (int)(w * 0.30f) && row == h / 2) return TILE_TABLET;
    if (col == (int)(w * 0.63f) && row == h / 2) return TILE_TABLET;
    if (col == (int)(w * 0.95f) && row == h / 2) return TILE_TABLET;

    /* Easter Egg — baú na Fase 2 */
    if (strip.phase == 2 && col == (int)(w * 0.48f) && row == h - 2) {
        return TILE_CHEST;
    }

    /* Fase 1: Águas Claras — poucos obstáculos, muita água */
    if (strip.phase == 1) {
        unsigned int r = lcg_rand(rng);
        if (r % 30 == 0 && row > 2 && row < h - 2) return TILE_ENEMY;
        return TILE_WATER;
    }

    /* Fase 2: Cavernas de Coral — corais densos no teto e chão */
    if (strip.phase == 2) {
        unsigned int r = lcg_rand(rng);
        /* Corais no teto (linha 1-3) */
        if (row <= 3 && (col % 7 < 3)) return TILE_CORAL;
        /* Corais no chão (últimas 3 linhas) */
        if (row >= h - 4 && (col % 7 < 3)) return TILE_CORAL;
        /* Rochas dispersas */
        if (r % 20 == 0) return TILE_ROCK;
        /* Inimigos (caranguejos) */
        if (r % 25 == 0 && row > 3 && row < h - 4) return TILE_ENEMY;
        return TILE_WATER;
    }

    /* Fase 3: Correntes Abissais — colunas de pilares e inimigos densos */
    if (strip.phase == 3) {
        unsigned int r = lcg_rand(rng);
        /* Pilares do templo a cada 12 colunas */
        if ((col - strip.start_col) % 12 == 0 && (row <= 4 || row >= h - 5)) {
            return TILE_ROCK;
        }
        /* Inimigos mais densos */
        if (r % 15 == 0 && row > 2 && row < h - 2) return TILE_ENEMY;
        return TILE_WATER;
    }

    /* Fase 4: Abismo Vulcânico — rochas e corredores estreitos. */
    if (strip.phase == 4) {
        unsigned int r = lcg_rand(rng);
        if ((row == 3 || row == h - 4) && col % 11 < 4) return TILE_ROCK;
        if (r % 22 == 0 && row > 2 && row < h - 2) return TILE_ENEMY;
        return TILE_WATER;
    }

    /* Fase 5: Templo de Apsu — arena limpa, inimigos apenas nas bordas. */
    if (strip.phase == 5) {
        if ((col < 12 || col > w - 13) && row > 3 && row < h - 4 && col % 5 == 0) return TILE_ENEMY;
        if ((col == 8 || col == w - 9) && row == h / 2) return TILE_ROCK;
        return TILE_WATER;
    }

    return TILE_EMPTY;
}

/* ============================================================
 * PRINT_MAP
 * Exibe o mapa completo com bordas decoradas.
 * ============================================================ */
void print_map(char *map, int width, int height, int rank) {
    if (rank != 0) return;

    /* Borda superior */
    printf("┌");
    for (int c = 0; c < width; c++) printf("─");
    printf("┐\n");

    /* Linhas do mapa */
    for (int r = 0; r < height; r++) {
        printf("│");
        for (int c = 0; c < width; c++) {
            char tile = map[r * width + c];
            /* Colorir tiles especiais (ANSI escape codes) */
            switch (tile) {
                case TILE_WATER:  printf("\033[34m%c\033[0m", tile); break; /* azul */
                case TILE_CORAL:  printf("\033[31m%c\033[0m", tile); break; /* vermelho */
                case TILE_ROCK:   printf("\033[90m%c\033[0m", tile); break; /* cinza */
                case TILE_ENEMY:  printf("\033[32m%c\033[0m", tile); break; /* verde */
                case TILE_TABLET: printf("\033[33m%c\033[0m", tile); break; /* amarelo */
                case TILE_CHEST:  printf("\033[93m%c\033[0m", tile); break; /* dourado */
                case TILE_PORTAL: printf("\033[96m%c\033[0m", tile); break; /* ciano */
                default:          printf("%c", tile);
            }
        }
        printf("│\n");
    }

    /* Borda inferior */
    printf("└");
    for (int c = 0; c < width; c++) printf("─");
    printf("┘\n");
}

/* Formato estável consumido pelo MPIMapLoader Java. */
int write_json(const char *path, char *map, int phase) {
    FILE *out = fopen(path, "w");
    if (!out) return 0;

    fprintf(out, "{\n  \"version\": 1,\n  \"phase\": %d,\n  \"width\": %d,\n  \"height\": %d,\n  \"tiles\": [\n",
            phase, MAP_WIDTH, MAP_HEIGHT);
    for (int row = 0; row < MAP_HEIGHT; row++) {
        fprintf(out, "    \"");
        for (int col = 0; col < MAP_WIDTH; col++) fputc(map[row * MAP_WIDTH + col], out);
        fprintf(out, "\"%s\n", row + 1 == MAP_HEIGHT ? "" : ",");
    }
    fprintf(out, "  ]\n}\n");
    fclose(out);
    return 1;
}

/* ============================================================
 * LCG_RAND — Linear Congruential Generator
 * Gerador pseudo-aleatório simples e reprodutível.
 * Não use para criptografia — apenas para geração de mapa.
 *
 * Parâmetros de Knuth (MMIX):
 *   a = 6364136223846793005
 *   c = 1442695040888963407
 * ============================================================ */
unsigned int lcg_rand(unsigned int *state) {
    *state = (*state) * 1664525u + 1013904223u;
    return *state;
}
