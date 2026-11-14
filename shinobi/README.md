# Kaze — O Último Shinobi

Jogo de plataformas 2D inspirado no clássico Shinobi (Sega), num único ficheiro HTML.
Sem bibliotecas externas, sem imagens nem ficheiros de áudio — tudo gerado por código
(Canvas 2D + Web Audio API).

## Como jogar

1. Descarrega a pasta `shinobi/` (ou só o `index.html`).
2. Abre o `index.html` num browser moderno (Chrome, Firefox, Edge).
3. Prime **Espaço** ou **Z** no menu para começar.

## Controlos

| Tecla | Ação |
|---|---|
| Setas / WASD | Mover, agachar |
| Espaço | Saltar (com salto duplo) |
| Z / J | Espada (combo de 3 golpes) |
| X / K | Shuriken |
| Shift | Dash/roll |
| Esc / P | Pausa |

Também suporta gamepad (mapeamento padrão) e botões táteis em telemóvel.

## Características

- 5 níveis com paleta, parallax, inimigos, armadilhas e chefe próprios.
- Música gerada por código com sequenciador de lookahead e camadas dinâmicas
  (pad, melodia, baixo, bateria, tensão, alerta) que entram conforme o progresso.
- Wall jump, ataque em salto com ricochete, shuriken tripla, checkpoints,
  combos de pontuação, reféns, recorde em localStorage.
