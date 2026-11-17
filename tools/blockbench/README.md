# Tears of Ink — Blockbench animation workspace (prototype, 17.11.2026)

> **ВАЖНО — 17.11.2026, автор уже проверил сцену в настоящем Blockbench:** `pale_animation_workspace.bbmodel` открывается и timeline работает, **но модель игрока/рук отображается прозрачными контурами и FP/F5 сцены визуально мешают друг другу**. Причина подтверждена: у вспомогательных кубов `faces.*.texture=null`. **Этот файл не готов для работы художника.** Не нужно дальше пытаться поставить на нём анимацию. Codex должен сначала подготовить исправленную непрозрачную сцену с понятным переключателем FP/F5, а затем делать импортёр. Конкретные требования внесены в [задание Codex](../../docs/CODEX_BLOCKBENCH_WEAPON_ANIMATION_PIPELINE_2026-11-17.md). Не трогать авторские модели/текстуры игры.

**Open:** [pale_animation_workspace.bbmodel](pale_animation_workspace.bbmodel). This is a **separate editable staging project**, not a replacement for the Minecraft item model. Its first two demo animations are disposable examples, not approved artistic choreography.

## What is already in this prototype

- The *actual author's* Pale Linear Greatsword source geometry (31 original cuboids) and original embedded PNG texture, duplicated four times without changing coordinates relative to grip `[7.75,2,7.75]`.
- A schematic Minecraft player, head/torso/legs and two independently pivoted arms in the `F5_RIG` group.
- A separate schematic first-person pair of arms in the `FP_RIG` group.
- Editable bones: `F5_right_arm`, `F5_left_arm`, `F5_right_sword`, `F5_left_sword`, plus the corresponding four `FP_*` bones. Each sword bone is nested under its arm for natural joint movement.
- Two purely illustrative 1-second animations on the Animate timeline: `animation.toi.demo_single_guard` and `animation.toi.demo_dual_guard`.

The format is **native Blockbench Bedrock Animated Model** (`meta.model_format=bedrock`), so Animate and the bone timeline work **without adding GeckoLib to the mod**. If desired, a copy can later be converted to GeckoLib Animated Model. The artist's original `.bbmodel`, model JSON, textures and game animation JSON remain untouched.

## How to try it

1. Download **only** `pale_animation_workspace.bbmodel` from this folder. In Blockbench use **File → Open Model**.
2. Open **Animate**, select either `animation.toi.demo_single_guard` or `animation.toi.demo_dual_guard`, and press Play.
3. In Outliner toggle `FP_RIG` and `F5_RIG` visibility to work on each staging view without the other being in the way. Under the rig, select an **arm** to move the whole sword with it, or a **sword** to adjust its blade about the grip; keep their parent-child connection.
4. Save your **own copy** of the project with a new name. Don't export a Bedrock entity over the current Java item model.
5. The demo clips are only to prove the editable workflow. You can add new animations and edit both view variants independently.

## Important current limitations

- **This is not yet a pixel-accurate 16:9 first-person camera** nor a verified Minecraft F5 reference. The two scene rigs are spatially separated on the editing canvas; the eye/FOV, vanilla held-item transforms and third-person shoulder/wrist alignment need calibration against the live game and the existing Python previewer.
- **There is no automatic .bbmodel → Tears of Ink animation importer yet.** A Minecraft 1.21.1 client cannot directly consume this staging project. The current game animations still live in `assets/tears-of-ink/weapon_anims/pale_linear_greatsword.json` and their server hit timings in `data/tears-of-ink/weapon_combos/`.
- An authored swing is transferable **only after** a tested conversion from the rig's hierarchical bone keyframes to the current FP item and F5 arm/wrist conventions, correct left-hand author Display, and server-defined hit windows. Simply copying XYZ angles is not sufficient.
- The Steve body and simplified arms are explicitly *technical visual guides* created for this staging file, not new game artwork. They must not replace any author's model/texture.

**The next Codex implementation assignment** is [docs/CODEX_BLOCKBENCH_WEAPON_ANIMATION_PIPELINE_2026-11-17.md](../../docs/CODEX_BLOCKBENCH_WEAPON_ANIMATION_PIPELINE_2026-11-17.md). It includes the calibrated FP/F5 cameras, validated Blockbench scene, importer and one end-to-end test before any promise of “draw once, works in game”.

**Source fidelity:** `src/main/resources/assets/tears-of-ink/models/item/pale_linear_great.bbmodel` and the live `models/item/pale_linear_great.json` are authoritative. The staging project copies geometry relative to `grip=[7.75,2,7.75]`, but keeps its own independent scene coordinates. Keep its source files untouched.
