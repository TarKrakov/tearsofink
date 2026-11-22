# Поле боя — полный список реально работающих команд (D-131)

**Minecraft 1.21.1 / Fabric.** Операторские команды (permission level 2), тестовое размещение вручную, **не** автоматическая генерация большого поля. Дополнено 22 ноября 2026 (D-133).

## Знамёна

```mcfunction
/tearsofink battlefield banner
/tearsofink battlefield banner regular_standard
/tearsofink battlefield banner regular_bare
/tearsofink battlefield banner paladin_standard
/tearsofink battlefield banner paladin_bare
/tearsofink battlefield banner hunter_standard
/tearsofink battlefield banner hunter_bare
```

- `banner` и `banner regular_standard`: **полный реющий штандарт регулярной армии Шпиля** (5 сегментов вертикального полотна).
- `banner regular_bare`: **настоящее авторское древко регулярной армии без полотна**, неподвижное.
- `paladin_standard` / `paladin_bare`: собственные авторские флаги паладинов Угасающей Звезды (реющий и без полотна).
- `hunter_standard` / `hunter_bare`: собственные авторские флаги исторических Чернильных Охотников (реющий и без полотна). Загружены реальные `.bbmodel` и PNG в коммите `b3d1f5b`, чужой арт не используется.
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

## 14 ситуаций и знамённых узлов (новые постановки layout=3, ID 20–33)

| ID | Команда | Якорь / смысл |
|---|---|---|
| 20 | `/tearsofink battlefield zone regular_squad_line_anchor` | Рубеж регулярной армии — настоящий реющий штандарт Шпиля |
| 21 | `/tearsofink battlefield zone regular_squad_rally_point` | Сбор у отрядного знамени — настоящий реющий штандарт Шпиля |
| 22 | `/tearsofink battlefield zone regular_bare_remnant` | Сорванное знамя Шпиля — настоящий штандарт без полотна |
| 23 | `/tearsofink battlefield zone paladin_holdout_anchor` | Рубеж паладинов с собственным реющим знаменем Души |
| 24 | `/tearsofink battlefield zone paladin_bare_relic` | След паладинов с настоящим голым древком своей фракции |
| 25 | `/tearsofink battlefield zone hunter_strike_mark` | След атаки исторических Чернильных Охотников с собственным реющим знаменем |
| 26 | `/tearsofink battlefield zone hunter_bare_trace` | Чернильный след с собственным авторским сорванным знаменем |
| 27 | `/tearsofink battlefield zone line_collapse` | Обрушение линии — ситуация, без знамени |
| 28 | `/tearsofink battlefield zone grind_center` | Узел ближнего боя — ситуация, без знамени |
| 29 | `/tearsofink battlefield zone breakthrough_lane` | Коридор прорыва — ситуация, без знамени |
| 30 | `/tearsofink battlefield zone aftermath_scatter` | Осколки сражения — ситуация, без знамени |
| 31 | `/tearsofink battlefield zone rear_guard_fallback` | Прикрытие отступления — ситуация, без знамени |
| 32 | `/tearsofink battlefield zone isolated_last_stand_small` | Одинокий заслон — ситуация, без знамени |
| 33 | `/tearsofink battlefield zone mixed_dead_ground` | Молчаливое поле — ситуация, без знамени |

После загрузки всех фракционных BBModel **7 из 14** сцен имеют реальные знамена: 3 регулярных Шпиля, 2 паладинских, 2 Чернильных Охотников. Ещё 7 — осмысленно без знамени. Ранее установленные `layout=2` не мигрируют и сохраняют исходный вид; только новая постановка `layout=3` получает фракционные знамёна.

## Ещё шесть тематических сцен со щитами Души и следами Чернильных Охотников (D-132, ID 34–39)

| ID | Команда | Сюжет и модели |
|---|---|---|
| 34 | `/tearsofink battlefield zone paladin_soul_shield_wall` | Сломанная линия Тарана, Отпора и Сияния — **существующие 3D-щиты Души**, декор ×1,5 |
| 35 | `/tearsofink battlefield zone paladin_mercy_holdout` | Милосердие и Сияние у последнего прохода |
| 36 | `/tearsofink battlefield zone paladin_broken_halo` | Три щита на сломанной дуге, пустое пространство внутри |
| 37 | `/tearsofink battlefield zone ink_hunter_sweep` | Диагональная цепочка боя из реальных моделей клинков |
| 38 | `/tearsofink battlefield zone ink_hunter_ambush` | Два фланга и свободный центр |
| 39 | `/tearsofink battlefield zone ink_hunter_last_trace` | Последний крупный клинок и удалённые следы |

Все шесть сцен имеют 8 детерминированных вариантов и сохраняют проверку рельефа. Масштаб ×1,5 относится только к полевым декоративным щитам Души, а не к боевым щитам игрока. После загрузки `.bbmodel` пять из шести получают **необязательные** собственные фракционные знамена (при нехватке места они пропускаются, ядро композиции остаётся). `ink_hunter_last_trace` оставлен без знамени. Прежние `layout=2` не изменены.

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

Исторические `layout=0`, `layout=1` и прежний `layout=2` **не меняются**. Новая постановка фракционных ситуаций сохраняет те же ID и использует `layout=3`, расширяя `Contacts` только в конец. Повторная установка нужна лишь для нового вида уже существовавших сцен; сохранённый мир не переписывается.

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


## Два монументальных клинка — D-134 (8 вариантов каждого)

```mcfunction
/tearsofink battlefield monument linear
/tearsofink battlefield monument linear 0
/tearsofink battlefield monument linear 7
/tearsofink battlefield monument cleaving
/tearsofink battlefield monument cleaving 0
/tearsofink battlefield monument cleaving 7
```

`linear` использует исходную модель Бледного линейного двуручника; `cleaving` — Меч Рассечения. Варианты 0–7: масштаб 7,5–10×, наклон 0–45°, небольшой yaw ±12°, заглубление острия. Это редкие многоблочные по **видимому размеру**, но с одним бесколлизионным block-anchor и отдельным нетикающим клиентским рендером, а не генератором мира. Команды ищут безопасное свободное место в **уже загруженных чанках** перед оператором; не ломают деревья/структуры, не ставят новый арт. Мелкое оружие и щиты в сценах теперь состарены автоматически из отдельных производных PNG, боевые предметы остаются прежними. Генератор полного поля добавит монументы как отдельные доминанты позднее.

## Композиции гигантских мечей с древним снаряжением
```mcfunction
/tearsofink battlefield monument linear tableau 0
/tearsofink battlefield monument linear tableau 7
/tearsofink battlefield monument cleaving tableau 0
/tearsofink battlefield monument cleaving tableau 7
```
Режим `tableau` размещает один огромный состаренный меч и связанную микроисторию из трёх малых состаренных предметов, стоящих/лежащих с разными углами и привязанных к реальной высоте земли. Обязательные части проверяются до записи, дополнительные могут отсутствовать. Это один нетикающий сценовый якорь с NBT контактов; старые ID 0–39 не изменены. Для одиночного гиганта используйте прежние `monument linear 0..7` и `cleaving 0..7`.

## D-135 — Автоматическое поле в настоящем мире

Следующие команды работают в операторском режиме (permission 2); **не ставят поле вручную** и не меняют старые сцены:

```mcfunction
/tearsofink battlefield field locate
/tearsofink battlefield field status
/tearsofink battlefield field approach
```

- `locate`: вывести координаты запланированного древнего поля и точки начала следов с существующей магистрали.
- `status`: загружена ли центральная область и есть ли подтверждающая отметка новой мирогенерации.
- `approach`: перенести оператора на главную дорогу в начало цепочки следов; пройти от дороги самостоятельно без отключения тумана, чтобы оценить раскрытие леса и поле.

Нужна **новая генерация** указанного района Чернильных Земель (надежнее новый тестовый мир). На старых посещённых чанках система специально не дорисовывает поле. Генерация создаёт первое связное поле возле дороги: 2 монументальных меча, 19 связанных малых сцен и 6 ориентирующих следов (27 **плановых слотов**, не принудительная квота при крутых склонах). После подхода к новому полю клиентский туман плавно расширяется в пределах его границ. Это автоматический игровой контент, а не `/battlefield zone` тестовая расстановка.

### D-136: проверка исправленного автоматического поля

```mcfunction
/tearsofink battlefield field locate
/tearsofink battlefield field approach
/tearsofink battlefield field status
```

Статус должен сообщить **реально поставленные композиции из 27**, а не число рецептов. Первое появление происходит после подгрузки чанков; если территория новая, под сцены теперь разрешено менять естественную землю и деревья (местные насыпи/срезы), между ними создаются собственные боевые рытвины и ямы. На старых версиях подготовленные участки будут дополняться при наличии достоверных следов прежней генерации. Если `status` сообщает, что подтверждения генерации нет, автоматическое изменение неподтверждённого старого чанка сознательно не запускается. Проверка художественного результата и тумана — только в Minecraft, без `/inkfog`.


### D-137 — проверка плотности оружия и доступности дальнего берега

```mcfunction
/tearsofink battlefield field status
/tearsofink battlefield field approach
```

Теперь `status` выводит **три самостоятельные стадии**, а не одну сумму: `сюжетные композиции X/27` (оригинальные знамена/монументы), `россыпи оружия Y/N` (новые многочисленные состаренные клинки/щиты) и `связующие переходы A/B` (частями построенные природные подходы на высокий берег). Переходы и россыпи возникают автоматически рядом с игроком в **уже готовых** чанках, не требуют команд ручной постановки. Рельеф и дальние участки заполняются постепенно, не мгновенно.

В нормальном тумане внутри поля почти не должно быть объёмных клубов, а снаружи вокруг него они должны оставаться плотными. Наблюдать выброшенные цветы, зависшие блоки и путь по земле через прежнюю реку; для новых зон используется специальная защита первичной генерации. Не изменять игровые предметы/модели через эти команды.
