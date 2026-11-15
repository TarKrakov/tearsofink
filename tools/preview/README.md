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
