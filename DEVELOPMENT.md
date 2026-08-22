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

O projeto inclui scripts que detectam e configuram automaticamente as dependências de sistema necessárias (**Java 21 LTS**, **Apache Maven**, **Blender 4.5 LTS** e **OpenMPI**):

#### No Linux (Debian / Ubuntu / Linux Mint):
```bash
make setup
# ou diretamente via script:
bash tools/setup/setup_environment.sh
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

### 6. Pipeline de Assets 3D -> 2.5D (Blender 4.5 LTS)

Caso modifique algum script Python em `personagens/scripts/`:

```bash
# Regenerar arquivos .blend e renderizar spritesheets PNG transparentes
make assets

# Pipeline rápido (apenas as poses de ataque das variantes)
make assets-variant-attacks
```

---

### 7. Demonstração de Gerador de Mapas em C com OpenMPI

```bash
# Compilar e rodar a simulação distribuída em 8 processos
make mpi-demo

# Gerar os layouts JSON das 5 fases em mpi/generated/
make mpi-generate
```
