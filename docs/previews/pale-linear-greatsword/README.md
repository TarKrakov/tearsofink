# Бледный двуручник — новые движения от авторских поз, D-026

17 ноября 2026, новая проба после обновления авторской модели. Кадр: F5 сзади, строгий фронтальный F5, первое лицо; 30 FPS, часы. [Что сделано и проверено](../../PALE_AUTHOR_POSES_REBUILD_2026-11-17.md).

Начать с [нейтральной стойки](baseline.png) и [сравнения alias до/после](baseline-alias-comparison.png). В сравнении одинаковая новая геометрия; сверху старые технические левые переопределения, снизу — точные авторские Display обеих рук. Это сравнение наследования поз, не переделка авторского арта.

- **[Одиночная серия](solo.gif)** — [диагональ](attack1.gif), [разруб сверху](attack2.gif), [мах вправо](attack3.gif).
- **[Парная серия](pair.gif)** — [рассечения наружу](dual_attack1.gif), [два укола](dual_attack2.gif), [общий разруб](dual_attack3.gif).
- **[Парный ПКМ: раскрытие, удержание → рывок](pair-charge.gif)**.
- [Парный R](pair-r.gif), [одиночный R](solo-r.gif), [умение левой руки](left-r.gif).
- R прерывает [первый](interrupt-1.gif), [второй](interrupt-2.gif), [третий](interrupt-3.gif) удар.

Плотные контактные листы старта, impact ±0.06/0.12 с, стыка и возврата:

- [Диагональ](attack1.png), [одиночный разруб](attack2.png), [мах вправо](attack3.png).
- [Парные рассечения](dual_attack1.png), [парные уколы](dual_attack2.png), [парный разруб](dual_attack3.png).
- Полные серии: [одиночная](solo.png), [парная](pair.png), [зарядка](pair-charge.png).

[Логи проверок](checks.log), [отчёт review](review-report.json), [Gradle build](build.log). Проверки прошли; review сохраняет одно предупреждение о быстром перемещении острия левого парного разруба. Старое [исследование шести принятых образцов](reference-study.png) оставлено как справочный материал.

Это технический рендер настоящей геометрии. Красный/малиновый различают руки; текстура, движение сущности при прыжке и физика Minecraft не воспроизводятся. Художественная приёмка и новая проверка в игровом клиенте ещё требуются.

```bash
python3 tools/preview/check_pale.py
python3 tools/preview/check_pale_space.py
python3 tools/preview/check_pale_edge.py
python3 tools/preview/review.py pale_linear_greatsword --check-only
python3 tools/preview/review.py pale_linear_greatsword --render --fps 30
python3 tools/preview/pale.py out/pale
python3 tools/preview/pale_baseline.py out/pale
# Gradle под JDK 25, bytecode release 21
bash ./gradlew build
```
