# Бледный меч — ЛКМ топоров и новые парные ПКМ/R

Актуальная версия D-027, 17 ноября 2026. [Что перенесено и проверено](../../PALE_AXE_SERIES_TRANSFER_2026-11-17.md). Каждая GIF — 30 FPS, одновременно F5 сзади, F5 спереди и первое лицо.

- **[Одиночная серия топора](solo.gif)**: [первый](axe_attack1.gif), [второй](axe_attack2.gif), [третий](axe_attack3.gif).
- **[Полная парная серия топоров, пять шагов](pair.gif)**: [правый](lmb-pair-1.gif), [левый](lmb-pair-2.gif), [общий](lmb-pair-3.gif), [повторный общий](lmb-pair-4.gif), [финальный](lmb-pair-5.gif).
- **[ПКМ: рукояти назад-вверх, острия вниз-внутрь → рывок](pair-charge.gif)**.
- **[R: скрещивание → раскрытие → подъём в прыжке → удар вниз](pair-r.gif)**.
- [Скрещённая защита с удержанием](parry-cross.gif).
- [Сохранённый одиночный приём](solo-r.gif), [приём слева](left-r.gif).
- Прерывание R из [первого](interrupt-1.gif), [второго](interrupt-2.gif), [третьего](interrupt-3.gif) одиночного удара.

Листы: [одиночная серия](solo.png), [парная](pair.png), [зарядка](pair-charge.png), [скрещённая защита](parry-cross.png), [парный R](pair-r.png). Плотные кадры ударов: [соло 1](axe_attack1.png), [соло 2](axe_attack2.png), [соло 3](axe_attack3.png), [парный финал](lmb-pair-5.png).

[Нейтраль](baseline.png), [сравнение устранённого конфликта alias](baseline-alias-comparison.png) остаются прежними: авторские Display/геометрия/текстура не менялись. Старые файлы `attackN` / `dual_attackN`, не указанные выше, относятся к отвергнутой собственной постановке; актуальные ЛКМ — только `axe_*` / `lmb-pair-*`.

[Логи проверок](checks.log), [диагностика review](review-report.json), [успешная сборка](build.log). Review сохраняет 15 предупреждений о скорости/резких переходах исходной серии топоров; её движения не переставлялись заново. FP-посадка по глубине предотвращает прохождение длинного меча через камеру. Цвет обозначает руку, материал и прыжок сущности не симулируются. Это предпросмотр геометрии, не запись Minecraft и не художественная приёмка.

```bash
python3 tools/preview/check_pale.py
python3 tools/preview/check_pale_space.py
python3 tools/preview/check_pale_edge.py
python3 tools/preview/review.py pale_linear_greatsword --check-only
python3 tools/preview/review.py pale_linear_greatsword --render --fps 30
python3 tools/preview/pale.py out/pale
bash ./gradlew build
```
