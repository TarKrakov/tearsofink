# Превью ударов (для Claude)

Рисует от первого лица и Стива в третьем лице (сзади и спереди) по файлам `weapon_anims/*.json` — той же математикой, что `client/combat/ThirdPerson.java` и `WeaponAnims.java`. **Менять их вместе:** если меняется расчёт в Java — повторить здесь.

Нужны `numpy`, `pillow` (шрифт DejaVu для подписей).

- `third.py` — математика: сэмплинг движений (spline, hold), рукоять/кончик (`axis`), третье лицо `solve4` (с поправками `third`: wrist_hold, mirror, center, lateral, no_cross), простой рендер.
- `run.py` — `cell()` одна поза; `python3 run.py <архетип> [движения]` — лист поз.
- `tl.py` — `build(arch, steps, dual, out.gif)` — гифка серии (шаги из `weapon_combos`); `python3 tl.py executioner scimitar …` — все архетипы.
- `pair.py` — `pair(arch, main, off, times, fix=…)` — обе руки, поправки на пробу.
- `edge.py` — `edge_align(clip, keys)` — довернуть клинок вокруг оси, чтобы кромка (выпуклая сторона) шла по ходу движения.
- `hilt.py` — расчёт позы «клинок назад вдоль руки» (удар эфесом).
- `twtest.py` — `run(clipname, clip, times, out, single=…)` — проверить движение до записи в JSON.

## Пилот Бледного линейного двуручника (17 ноября 2026)

- `python3 tools/preview/check_pale.py` — проверка семи шагов, моментов попаданий, направления уколов и неподвижности pivot (2 480 выборок).
- `python3 tools/preview/pale.py out/pale` — семь отдельных ударов, обе серии, R из каждого одиночного удара, парные ПКМ/R и умение слева. Каждый кадр включает все три ракурса.
- `tl.py` учитывает настройки `third` архетипа и смешивает фактически показанные позы рук и кистей, как игровой клиент. `all_for` учитывает `tempo` в длительности шагов.
- Готовые основные GIF: [docs/previews/pale-linear-greatsword](../../docs/previews/pale-linear-greatsword/README.md).

Просмотрщик окрашивает геометрию для различения рук; он не является захватом материалов/текстуры из Minecraft и не моделирует физику сущности или серверные попадания.
