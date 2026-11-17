# Бледный меч — первое лицо после исправления equip-опускания

17 ноября 2026. [Причина, исправление и границы проверки](../../PALE_FP_RENDER_FIX_2026-11-17.md). Для проверки в игре нужен **новый JAR и перезапуск Minecraft**: изменён Java-код рендера.

Начать с крупных **FP 960×540, 16:9, 30 FPS**. Прицел и нижняя линия помогают оценить высоту клинков относительно HUD:

- **[Одиночная серия — FP](solo-fp.gif)**: высокий замах → нисходящая диагональ, верхний разруб, прямой укол. [Кадры](solo-fp.png).
- **[Парная серия — FP](pair-fp.gif)**: острия внутрь, высокая подготовка, согласованные положения. [Кадры](pair-fp.png).
- **[Общие атаки 3/4 — FP](pair-3-4-fp.gif)**.
- **[Зарядка ПКМ → рывок — FP](pair-charge-fp.gif)**: прежние углы, позы сдвинуты только вверх. [Кадры](pair-charge-fp.png).
- **[Техническое сравнение до/после](fp-before-after.png)**: прежняя поза с дополнительным equip-опусканием справа и исправленная композиция. Это реконструкция матриц, не скриншот запущенного Minecraft.

Все три вида одновременно — F5 сзади, F5 спереди и широкоформатное первое лицо:

- [Одиночная серия](solo.gif): [1](axe_attack1.gif), [2](axe_attack2.gif), [3](axe_attack3.gif).
- [Парная серия](pair.gif): [1](lmb-pair-1.gif), [2](lmb-pair-2.gif), [3](lmb-pair-3.gif), [4](lmb-pair-4.gif), [5](lmb-pair-5.gif); [фрагмент 3/4](pair-3-4.gif).
- [ПКМ](pair-charge.gif), [парный R](pair-r.gif), [скрещённая защита](parry-cross.gif), [одиночный R](solo-r.gif), [приём слева](left-r.gif).
- R из [первого](interrupt-1.gif), [второго](interrupt-2.gif), [третьего](interrupt-3.gif) одиночного удара.

**F5-источники, поправки и тайминги не перестраивались** — все 29 снимков совпадают с предыдущей версией. Порядок одиночных ударов сохранён по ответу автора: диагональ → верхний разруб → укол. [Нейтраль](baseline.png), авторские модель/исходник/текстура/масштаб прежние.

[Логи проверок](checks.log), [аудит скомпилированного FP-рендера и ванильной формулы](renderer-audit.log), [review](review-report.json), [сборка](build.log), [контрольные суммы входов предпросмотра](preview-inputs.json). Review сохраняет 10 замечаний о резких изменениях скорости/переходах. Цвет различает руки; текстура и физика сущности не симулируются. Minecraft в этой итерации не запускался, художественная оценка — после игрового теста автора. Старые файлы `attackN` / `dual_attackN`, не указанные выше, — история отвергнутой самостоятельной постановки.

```bash
bash ./gradlew build
python3 tools/preview/check_pale.py
python3 tools/preview/check_pale_fp.py --compiled
python3 tools/preview/check_pale_sync.py
python3 tools/preview/review.py pale_linear_greatsword --check-only
python3 tools/preview/review.py pale_linear_greatsword --render --fps 30
python3 tools/preview/pale.py out/pale
python3 tools/preview/pale_fp_compare.py out/pale
```
