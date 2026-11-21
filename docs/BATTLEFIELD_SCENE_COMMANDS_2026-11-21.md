# Поле боя — полный список реально работающих команд (D-131)

**Minecraft 1.21.1 / Fabric.** Операторские команды (permission level 2), тестовое размещение вручную, **не** автоматическая генерация большого поля. Актуально на 21 ноября 2026.

## Знамёна

```mcfunction
/tearsofink battlefield banner
/tearsofink battlefield banner regular_standard
/tearsofink battlefield banner regular_bare
```

- `banner` и `banner regular_standard`: **полный реющий штандарт регулярной армии Шпиля** (5 сегментов вертикального полотна).
- `banner regular_bare`: **настоящее авторское древко регулярной армии без полотна**, неподвижное.
- Для паладинов Угасающей Звезды и Чернильных Охотников пока **нет загруженных .bbmodel знамён**; команды `paladin_standard`, `hunter_bare` и т. п. **не зарегистрированы**, чужие знамёна не подставляются.
- Размещение учитывает реальный объём древка/колыхания на сложном рельефе, не срубает деревья и не очищает землю.

## Команды сцен

Форма вызова:

```mcfunction
/tearsofink battlefield zone list
/tearsofink battlefield zone <scene_id>
/tearsofink battlefield zone <scene_id> 0
/tearsofink battlefield zone <scene_id> 7
/tearsofink battlefield zone preview <scene_id>
/tearsofink battlefield zone place <scene_id>
```

`variant` можно указать **от 0 до 7**; если не указан — выбирается детерминированно от seed/координат. **Прямой вызов, `preview` и `place` сейчас одинаково ставят реальные проверочные блоки** — это не прозрачные клиентские «призраки». Вариант с цифрой работает и для `preview`/`place`. Повторное выполнение не удаляет прежние превью. Автоматической команды `clear` сейчас нет — намеренно нет небезопасного удаления декора из мира.

## 14 новых ситуаций и знамённых узлов (layout=2, ID 20–33)

| ID | Команда | Якорь / смысл |
|---|---|---|
| 20 | `/tearsofink battlefield zone regular_squad_line_anchor` | Рубеж регулярной армии — настоящий реющий штандарт Шпиля |
| 21 | `/tearsofink battlefield zone regular_squad_rally_point` | Сбор у отрядного знамени — настоящий реющий штандарт Шпиля |
| 22 | `/tearsofink battlefield zone regular_bare_remnant` | Сорванное знамя Шпиля — настоящий штандарт без полотна |
| 23 | `/tearsofink battlefield zone paladin_holdout_anchor` | Опорный рубеж паладинов (без флага) — **пока без авторского знамени**, не канонический арсенал |
| 24 | `/tearsofink battlefield zone paladin_bare_relic` | След угасающей звезды (без флага) — **пока без авторского знамени**, не канонический арсенал |
| 25 | `/tearsofink battlefield zone hunter_strike_mark` | След удара охотников (без флага) — **пока без авторского знамени**, не канонический арсенал |
| 26 | `/tearsofink battlefield zone hunter_bare_trace` | Последний след охотников (без флага) — **пока без авторского знамени**, не канонический арсенал |
| 27 | `/tearsofink battlefield zone line_collapse` | Обрушение линии — ситуация, без знамени |
| 28 | `/tearsofink battlefield zone grind_center` | Узел ближнего боя — ситуация, без знамени |
| 29 | `/tearsofink battlefield zone breakthrough_lane` | Коридор прорыва — ситуация, без знамени |
| 30 | `/tearsofink battlefield zone aftermath_scatter` | Осколки сражения — ситуация, без знамени |
| 31 | `/tearsofink battlefield zone rear_guard_fallback` | Прикрытие отступления — ситуация, без знамени |
| 32 | `/tearsofink battlefield zone isolated_last_stand_small` | Одинокий заслон — ситуация, без знамени |
| 33 | `/tearsofink battlefield zone mixed_dead_ground` | Молчаливое поле — ситуация, без знамени |

Три новых композиции реально опираются на готовые знамена: две — на `regular_standard`, одна — на `regular_bare`. Остальные **11 сцен** сознательно обходятся без знамени. Неподтверждённые фракционные модели не выдуманы и не подменены чужими.

## Все прежние команды — сохранены (layout=1, ID 0–19)

```mcfunction
/tearsofink battlefield zone last_stand
/tearsofink battlefield zone banner_line
/tearsofink battlefield zone broken_wall
/tearsofink battlefield zone spear_collapse
/tearsofink battlefield zone abandoned_post
/tearsofink battlefield zone blood_duel
/tearsofink battlefield zone spire_clash
/tearsofink battlefield zone hunter_trace
/tearsofink battlefield zone pale_oath
/tearsofink battlefield zone blade_grove
/tearsofink battlefield zone fallen_edge
/tearsofink battlefield zone twin_standards
/tearsofink battlefield zone cross_fire
/tearsofink battlefield zone final_trace
/tearsofink battlefield zone shield_crescent
/tearsofink battlefield zone knife_scatter
/tearsofink battlefield zone axe_breach
/tearsofink battlefield zone blade_fan
/tearsofink battlefield zone crossbow_cache
/tearsofink battlefield zone lone_blade
```

Сцены с историческими `layout=0` и `layout=1` **не переделывались**: уже установленные preview-сцены читают прежнюю раскладку и индексы по blockstate. Новые сценовые IDs добавлены в конец, формат NBT высот точек контакта не менялся.

## Скопировать и проверить все 14 новых сцен (вариант 0)

```mcfunction
/tearsofink battlefield zone regular_squad_line_anchor 0
/tearsofink battlefield zone regular_squad_rally_point 0
/tearsofink battlefield zone regular_bare_remnant 0
/tearsofink battlefield zone paladin_holdout_anchor 0
/tearsofink battlefield zone paladin_bare_relic 0
/tearsofink battlefield zone hunter_strike_mark 0
/tearsofink battlefield zone hunter_bare_trace 0
/tearsofink battlefield zone line_collapse 0
/tearsofink battlefield zone grind_center 0
/tearsofink battlefield zone breakthrough_lane 0
/tearsofink battlefield zone aftermath_scatter 0
/tearsofink battlefield zone rear_guard_fallback 0
/tearsofink battlefield zone isolated_last_stand_small 0
/tearsofink battlefield zone mixed_dead_ground 0
```

## Принципы ручной художественной проверки

1. **Не ставить баннер ради баннера:** линия регулярной армии имеет общий фронт, сбор — более тыловую точку, сорванный знак — след разрушения.
2. Ситуации отдельно: сломанная линия, разорванный коридор, узел боя, редкие следы, отступление, одинокий заслон. Защищать **пустое пространство** между группами, не превращать их в одинаковые кучи.
3. Оружие сохраняет независимые произвольные `yaw / pitch / roll` и правильную опору: кончик клинка вниз, рукоять не закопана; щиты, копья и арбалеты лежат/наклонены осмысленно.
4. Неровная земля допустима, но обязательные участники сцены должны уместиться. Ничего не срубать ради ровного квадрата.
5. При появлении реального авторского знамени паладинов/охотников заменить *отсутствующий якорь* в их рецептах, **не** переименовывая или переупорядочивая scene ID.
6. Это операторский **арт-пилот**. Массовая worldgen-генерация целого BattlefieldPlan, тонкая интеграция леса/тумана, оценка FPS/multiplayer — отдельно, не называем уже готовыми.

