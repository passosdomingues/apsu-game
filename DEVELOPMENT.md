# Guia de Desenvolvimento — As Águas de Apsu (`apsu-game`)

Este documento instrui novos desenvolvedores no processo de preparação do ambiente local, compilação, execução da suíte de testes e inicialização do jogo tanto em sistemas **Linux (Debian/Ubuntu/Mint)** quanto em **Windows (10/11)**.

---

## Próximos Passos (Clone -> Setup -> Build -> Test -> Run)

### 1. Clonando o Repositório
```bash
git clone https://github.com/rafael/apsu-game.git
cd apsu-game
```

---

### 2. Preparação Automática de Ambiente (Setup Idempotente)

O alvo padrão `make` verifica somente as dependências de runtime (**Java 21 LTS** e **Apache Maven**). Os OBJ/MTL dos personagens e as imagens do cenário já estão em `src/main/resources`; Blender e OpenMPI só são necessários para regenerar assets ou layouts.

#### No Linux (Debian / Ubuntu / Linux Mint):
```bash
make setup
# ou diretamente via script:
APSU_RUNTIME_ONLY=1 bash tools/setup/setup_environment.sh
```

Para gerar assets ou mapas, instale as ferramentas específicas sob demanda:
```bash
make setup-assets   # Blender
make setup-mpi      # OpenMPI
```

#### No Windows (PowerShell 5.1+):
```powershell
powershell -ExecutionPolicy Bypass -File tools/setup/setup_environment.ps1
```

---

### 3. Compilação do Código Fonte

```bash
# Compilar todas as classes Java 21
make build
# ou via Maven:
mvn compile
```

---

### 4. Execução dos Testes Automatizados

O projeto possui uma suíte com 45 testes JUnit 5 cobrindo motor gráfico, físicas, câmera, eventos, persistência e IA:

```bash
# Rodar todos os testes com relatório
make test
# ou via Maven:
mvn test
```

---

### 5. Execução do Jogo (Runtime 60 FPS)

```bash
# Iniciar a aplicação JavaFX principal
make run
# ou via Maven:
mvn javafx:run
```

---

### 6. Personagens 3D em tempo real e cenário 2.5D (Blender 4.5 LTS)

Caso modifique algum script Python em `personagens/scripts/`:

```bash
# Regenerar modelos Blender, exportar malhas OBJ/MTL para o jogo e renderizar o cenário
make assets

# Exportar novamente apenas os modelos 3D de runtime a partir dos .blend existentes
make export-runtime-models

# Gera as poses 3D animadas de ataque das variantes
make assets-variant-attacks
```

Personagens e elementos de fase (recifes/cardumes, naufrágio, baú, ruínas,
correntes, portal e obstáculos) são malhas OBJ/MTL renderizadas em
`Runtime3DLayer`. Planos de fundo, HUD e efeitos permanecem em 2.5D no Canvas.
Os modelos ficam em `src/main/resources/models/characters/` e
`src/main/resources/models/scenery/`. O nado usa inclinação/balanço leves na
malha base para evitar carregamento de poses OBJ durante a partida; ataques
selecionam um modelo OBJ de estilo pronto com efeito de impacto leve. MPI não
participa do desenho de quadros.

---

### 7. Demonstração de Gerador de Mapas em C com OpenMPI

```bash
# Compilar e rodar a simulação distribuída em 8 processos
make mpi-demo

# Gerar os layouts JSON das 5 fases em mpi/generated/
make mpi-generate
```

Os mapas MPI são pré-processados e reproduzíveis, mas não são distribuídos pelo
loop gráfico. `make mpi-generate` falha se um worker não terminar ou se algum
arquivo de saída estiver vazio; não mascara erros da execução.
