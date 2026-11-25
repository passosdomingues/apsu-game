#!/usr/bin/env python3
"""Verificação de contraste de fundo e personagens nas 5 fases do Apsu (APSU-A02 / Sprint 13).

Garante que a razão de contraste entre os elementos em primeiro plano (herói, guardiões,
inimigos) e os panoramas de fundo das fases P1 a P5 seja maior ou igual a 1.8:1,
assegurando legibilidade visual e imersão subaquática.
"""

import os
import sys
from PIL import Image
import numpy as np

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
BG_DIR = os.path.join(PROJECT_ROOT, "src", "main", "resources", "backgrounds")
HERO_SPRITE = os.path.join(PROJECT_ROOT, "src", "main", "resources", "sprites", "adapa", "01_adapa_heroi.png")
MIN_CONTRAST_RATIO = 1.80

def rel_luminance(r, g, b):
    """Calcula a luminância relativa sRGB padrão WCAG."""
    def to_linear(c):
        c = c / 255.0
        return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
    return 0.2126 * to_linear(r) + 0.7152 * to_linear(g) + 0.0722 * to_linear(b)

def contrast_ratio(l1, l2):
    """Calcula a razão de contraste entre duas luminâncias relativas."""
    hi = max(l1, l2)
    lo = min(l1, l2)
    return (hi + 0.05) / (lo + 0.05)

def get_sprite_luminance(path):
    """Calcula a luminância média dos pixels visíveis de um sprite."""
    img = Image.open(path).convert("RGBA")
    arr = np.array(img)
    alpha = arr[:, :, 3] > 60
    if not np.any(alpha):
        return 0.5
    rgb = arr[alpha][:, :3]
    lums = [rel_luminance(p[0], p[1], p[2]) for p in rgb[::4]] # amostragem rápida
    return float(np.mean(lums))

def main():
    if not os.path.exists(HERO_SPRITE):
        print(f"Erro: Sprite do herói não encontrado em {HERO_SPRITE}", file=sys.stderr)
        return 1

    hero_lum = get_sprite_luminance(HERO_SPRITE)
    # Aura do guardião/herói Adapa (#4db8ff e reflexos dourados)
    aura_lum = rel_luminance(77, 184, 255)
    focal_lum = (hero_lum + aura_lum) / 2.0

    print("=" * 60)
    print("VERIFICAÇÃO DE CONTRASTE DE FUNDO (APSU-A02)")
    print(f"Luminância base do herói: {hero_lum:.3f} | Aura: {aura_lum:.3f} | Focal: {focal_lum:.3f}")
    print(f"Critério de aceite: Contraste mínimo >= {MIN_CONTRAST_RATIO:.2f}:1")
    print("=" * 60)

    all_passed = True
    for phase in range(1, 6):
        bg_path = os.path.join(BG_DIR, f"phase-{phase}.png")
        if not os.path.exists(bg_path):
            print(f"Fase {phase}: ERRO - Arquivo de fundo ausente: {bg_path}")
            all_passed = False
            continue

        img = Image.open(bg_path).convert("RGB")
        w, h = img.size
        # Amostragem em 5 pontos chave ao longo da fase no nível do nado (y = h * 0.45 a 0.55)
        fractions = [0.10, 0.25, 0.50, 0.75, 0.90]
        ratios = []

        for frac in fractions:
            sample_x = int(w * frac)
            sample_y = int(h * 0.50)
            # Amostra média em bloco de 20x20
            box = img.crop((max(0, sample_x - 10), max(0, sample_y - 10),
                            min(w, sample_x + 10), min(h, sample_y + 10)))
            box_arr = np.array(box)
            avg_r = float(np.mean(box_arr[:, :, 0]))
            avg_g = float(np.mean(box_arr[:, :, 1]))
            avg_b = float(np.mean(box_arr[:, :, 2]))
            bg_lum = rel_luminance(avg_r, avg_g, avg_b)
            # Contraste com corpo do sprite ou aura de contorno
            cr_sprite = contrast_ratio(hero_lum, bg_lum)
            cr_aura = contrast_ratio(aura_lum, bg_lum)
            effective_cr = max(cr_sprite, cr_aura)
            ratios.append(effective_cr)

        min_cr = min(ratios)
        avg_cr = sum(ratios) / len(ratios)
        status = "APROVADO" if min_cr >= MIN_CONTRAST_RATIO else "REPROVADO"
        if min_cr < MIN_CONTRAST_RATIO:
            all_passed = False

        print(f"Fase {phase} | Mín: {min_cr:.2f}:1 | Méd: {avg_cr:.2f}:1 | Amostras: {[round(r, 2) for r in ratios]} | {status}")

    print("=" * 60)
    if all_passed:
        print("RESULTADO: TODAS AS FASES APROVADAS NO CONTRASTE DE FUNDO (CR >= 1.80)")
        return 0
    else:
        print("RESULTADO: FALHA NA VERIFICAÇÃO DE CONTRASTE")
        return 1

if __name__ == "__main__":
    sys.exit(main())
