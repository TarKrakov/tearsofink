# D-058 — Варианты пения Скитальцев

Первый набор сохраняет камерное звучание исходника. Во втором заметнее протяжные нижние ответы. В третьем верхняя линия сильнее выступает к кульминации. Сводные записи выровнены по интегральной громкости и сохраняют полное затухание. Сольные дорожки выровнены по отдельности с приоритетом пикового запаса, поэтому их сумма отличается от сведения.

## Записи и замеры

Все файлы закодированы в Ogg Vorbis, 44,1 кГц. Замеры сделаны после декодирования с помощью FFmpeg loudnorm.

| Запись | Каналы | Секунды | LUFS-I | dBTP |
| --- | ---: | ---: | ---: | ---: |
| [01_Close_Prayer_Choir](01_Close_Prayer_Choir.ogg) | 2 | 17.28 | -20.45 | -9.19 |
| [01_Close_Prayer_Voice_1_Lead](01_Close_Prayer_Voice_1_Lead.ogg) | 1 | 16.00 | -20.45 | -6.52 |
| [01_Close_Prayer_Voice_2_Low](01_Close_Prayer_Voice_2_Low.ogg) | 1 | 16.00 | -21.70 | -6.28 |
| [01_Close_Prayer_Voice_3_High](01_Close_Prayer_Voice_3_High.ogg) | 1 | 16.00 | -22.97 | -6.22 |
| [02_Echoes_of_Grief_Choir](02_Echoes_of_Grief_Choir.ogg) | 2 | 18.80 | -20.47 | -8.42 |
| [02_Echoes_of_Grief_Voice_1_Lead](02_Echoes_of_Grief_Voice_1_Lead.ogg) | 1 | 16.00 | -20.45 | -6.44 |
| [02_Echoes_of_Grief_Voice_2_Low](02_Echoes_of_Grief_Voice_2_Low.ogg) | 1 | 16.00 | -23.24 | -6.34 |
| [02_Echoes_of_Grief_Voice_3_High](02_Echoes_of_Grief_Voice_3_High.ogg) | 1 | 16.00 | -22.47 | -6.25 |
| [03_Light_in_Exile_Choir](03_Light_in_Exile_Choir.ogg) | 2 | 19.60 | -20.47 | -7.28 |
| [03_Light_in_Exile_Voice_1_Lead](03_Light_in_Exile_Voice_1_Lead.ogg) | 1 | 16.00 | -21.39 | -6.23 |
| [03_Light_in_Exile_Voice_2_Low](03_Light_in_Exile_Voice_2_Low.ogg) | 1 | 16.00 | -21.08 | -6.31 |
| [03_Light_in_Exile_Voice_3_High](03_Light_in_Exile_Voice_3_High.ogg) | 1 | 16.00 | -20.91 | -6.25 |

Для сравнения, прежние три игровые записи D-058 дают от −21,07 до −20,13 LUFS-I и от −7,19 до −6,33 dBTP. Некоторые отдельные ответы тише этого диапазона, чтобы сохранить динамику при пике ниже −6 dBTP.

## Воспроизведение обработки

Исходник — авторский `09_Pale_Invocation.wav` из вложения в чат. SHA-256 исходника: `4792dabe30c03df2a2675bb4f3fc70dc76b0b3b5f0c7a9c0dfaedbcd822b283e`. Для повторного рендера нужны Python с NumPy и SciPy и FFmpeg с Rubber Band и libvorbis.

```bash
python tools/render_strider_lament_previews.py /path/to/09_Pale_Invocation.wav docs/previews/pale-strider-lament
```

Ведущая линия сохраняет мелодию исходной записи. Поддерживающие голоса получены из полос исходного микса с примесью октавных слоёв, отдельными огибающими фраз, небольшими сдвигами по времени и индивидуальным пространством. Источник содержит несколько нот одновременно, и отдельные партии исходной записи не выделялись. Автор выбрал все три комплекта для [игрового репертуара](../../STRIDER_LAMENT_AUDIO_REFINEMENT.md#игровой-репертуар-18-ноября-2026).
