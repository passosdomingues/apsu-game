# Sonda de entrada (X11)

No Linux com X11, segurar uma tecla gera pares KEY_RELEASED e KEY_PRESSED em sequencia. Para confirmar no seu ambiente, rode o jogo com `APSU_INPUT_TRACE=1` (story APSU-F01 adiciona a flag) e segure a seta por 3 s: o log deve mostrar zero liberacoes confirmadas. Sem a correcao, cada par pode cair entre dois ticks e zerar a aceleracao por 1 frame, o que aparece como tremor ao andar.
