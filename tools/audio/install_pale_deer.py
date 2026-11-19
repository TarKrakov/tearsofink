#!/usr/bin/env python3
"""Install D-076 in a validated checkout. Render first; fail on any changed anchor."""
import json
from pathlib import Path
from render_pale_deer import DURATIONS, ROOT

MAIN=Path("src/main/java/com/tarkrakov/tearsofink")
CLIENT=Path("src/client/java/com/tarkrakov/tearsofink/client")
ASSET=ROOT.parent
DOC=Path("docs")
# client attenuation, server delivery is +2, mixer preview base level, RU, EN.
EVENTS={
 "voice": (30,.54,"Олень подаёт голос","Deer calls"),
 "snort": (18,.42,"Олень фыркает","Deer snorts"),
 "step": (16,.36,"Олень шагает","Deer steps"),
 "eat": (8,.28,"Олень жуёт","Deer grazes"),
 "alert_snort": (22,.54,"Олень настороженно фыркает","Deer snorts in alarm"),
 "watch_roar": (48,.82,"Дозорный олень ревёт","Sentry deer bellows"),
 "roar": (36,.62,"Олень отвечает рёвом","Deer bellows"),
 "run_step": (36,.48,"Олень скачет","Deer gallops"),
}
def replace(text,old,new):
    assert text.count(old)==1, f"Changed or duplicate anchor: {old[:160]}"
    return text.replace(old,new)
def edit(path,fn):
    before=path.read_text()
    after=fn(before)
    assert before!=after,str(path)
    path.write_text(after)
def append_json(path,entries):
    old=path.read_text()
    parsed=json.loads(old)
    assert not (parsed.keys() & entries.keys()),path
    i=old.rfind("}")
    assert old[i+1:].strip()==""
    body=json.dumps(entries,ensure_ascii=False,indent=2)[2:-2]
    new=old[:i].rstrip()+",\n"+body+"\n}"+old[i+1:]
    assert json.loads(new)==(parsed|entries)
    path.write_text(new)
def main():
    assert set(EVENTS)==set(DURATIONS)
    for event in EVENTS:
        assert all((ROOT/f"pale_deer_{event}_{i:02d}.ogg").is_file() for i in range(1,4))
    audio=MAIN/"entity/PaleDeerAudio.java"
    assert not audio.exists()
    audio.write_text(Path("tools/audio/PaleDeerAudio.java.template").read_text())
    def entity(s):
        s=replace(s,"import net.minecraft.world.level.Level;","import net.minecraft.world.level.Level;\nimport net.minecraft.world.level.block.state.BlockState;")
        s=replace(s,"    private final Map<UUID, Vec3> samples = new HashMap<>();",
                    "    private final Map<UUID, Vec3> samples = new HashMap<>();\n    private final PaleDeerAudio deerAudio=new PaleDeerAudio();")
        s=replace(s,"    @Override public boolean removeWhenFarAway(double distance) { return false; }",
                    "    @Override public boolean removeWhenFarAway(double distance) { return false; }\n    // Own hoof contacts replace generic block step sounds, not other entity sounds.\n    @Override protected void playStepSound(BlockPos pos,BlockState state) {}")
        s=replace(s,"        if (!active || (!getNavigation().isDone() && !pathArea(server))) stopRoute();\n        super.tick();",
                    "        if (!active || (!getNavigation().isDone() && !pathArea(server))) stopRoute();\n        double oldX=getX(), oldZ=getZ();\n        super.tick();")
        s=replace(s,"        data.tick(server,g,this);\n        int guard=g.guards.indexOf(getUUID());",
                    "        data.tick(server,g,this);\n        tickBehavior(server,data,g);\n        double dx=getX()-oldX,dz=getZ()-oldZ;\n        deerAudio.tick(this,g.members.size(),suspect!=null&&suspicion>0,dx*dx+dz*dz);\n    }\n    private void tickBehavior(ServerLevel server,PaleDeerHerd data,PaleDeerHerd.Group g) {\n        int guard=g.guards.indexOf(getUUID());")
        return s
    edit(MAIN/"entity/PaleDeerEntity.java",entity)
    declarations="\n\t/** D-076 — independent deer palette, fixed delivery range. */\n"
    for name,(dist,vol,ru,en) in EVENTS.items():
        id="pale_deer_"+name
        declarations+=f'\tpublic static final SoundEvent {id.upper()} = Registry.register(BuiltInRegistries.SOUND_EVENT, TearsOfInk.id("{id}"), SoundEvent.createFixedRangeEvent(TearsOfInk.id("{id}"), {dist+2}.0f));\n'
    edit(MAIN/"sound/ModSounds.java",lambda s:replace(s,"\tprivate ModSounds() {}",declarations+"\n\tprivate ModSounds() {}"))
    entries={}
    for name,(dist,vol,ru,en) in EVENTS.items():
        id="pale_deer_"+name
        entries[id]={"sounds":[{"name":f"tears-of-ink:{id}_{i:02d}","attenuation_distance":dist} for i in range(1,4)],"subtitle":"subtitles.tears-of-ink."+id}
    append_json(ASSET/"sounds.json",entries)
    for lang,idx,group in [("ru_ru",2,"Олени"),("en_us",3,"Deer")]:
        d={"mixer.tears-of-ink.group.deer":group}
        d.update({"subtitles.tears-of-ink.pale_deer_"+name:spec[idx] for name,spec in EVENTS.items()})
        append_json(ASSET/"lang"/(lang+".json"),d)
    def mixer(s):
        s=replace(s,"FOREST, MUSIC, OWLS, CATERPILLARS, DIARY;","FOREST, MUSIC, OWLS, CATERPILLARS, DIARY, DEER;")
        rows=""
        for name,(_,vol,_,_) in EVENTS.items():
            id="pale_deer_"+name
            rows+=f'        new Entry(TearsOfInk.id("{id}"), Group.DEER, SoundSource.NEUTRAL, {vol:.4f}f, "subtitles.tears-of-ink.{id}"),\n'
        return replace(s,'        new Entry(TearsOfInk.id("diary_page_turn")',rows+'        new Entry(TearsOfInk.id("diary_page_turn")')
    edit(CLIENT/"audio/AudioMixer.java",mixer)
    # The path/range/event correspondence must be exact. Keep all previous entries.
    sounds=json.loads((ASSET/"sounds.json").read_text())
    for k,v in entries.items():
        assert sounds[k]==v
        assert all((ROOT/(x["name"].split(":")[1]+".ogg")).is_file() for x in v["sounds"])
    for k,spec in EVENTS.items():
        id="pale_deer_"+k
        assert id.upper() in (MAIN/"sound/ModSounds.java").read_text()
        assert id.upper() in audio.read_text()
        assert id in (CLIENT/"audio/AudioMixer.java").read_text()
    (DOC/"PALE_DEER_HERDS.md").write_text((DOC/"PALE_DEER_HERDS.md").read_text()+"""
## D-076 — Пробное авторское по задаче аудио (19 ноября 2026)
Автор попросил добавить звуки сейчас, отменив прежнюю отсрочку только для оленей. Оригинальный процедурный набор (не полевые записи, не ванильные сэмплы): 8 событий × 3 моно Vorbis 44,1 кГц. Голос — неровные гортанные импульсы, меняющиеся форманты и носовой оттенок; фырканье — короткие воздушные выбросы; копыта — сухой контакт и затухающие низкие резонансы; еда — отдельные короткие щелчки. Никакого непрерывного общего шуршания, музыка и остальные существа неизменны. Длительности рёва укладываются в авторские анимации с учётом высоты голоса. Дальности 30/18/16/8/22/48/36/36 по таблице handoff, индивидуальная громкость и суммарная компенсация стада. Серверный PaleDeerAudio использует одновременно stamp и возраст клипа: рёв один раз при входе в состояние, при загрузке посреди клипа не повторяется. Копыта идут по пересечению маркеров 500/1500 мс в walk, 750/0 мс в run, с индивидуальным темпом и фазой, только при фактическом движении по земле (маркеры предварительные, визуальная сверка в игре необходима); звуки кормления в 1350/2350/4000/5000 мс. Настороженное фырканье на возникновение подозрения, голос/фырканье в спокойных состояниях по независимым таймерам. Vanilla block step подавлен только у оленя. Звуки урона/смерти специально не менялись. Категория NEUTRAL, фиксированная дальность доставки на 2 блока больше attenuation_distance. F8 / /inksound: группа «Олени», 8 отдельных регуляторов, прежние проценты и группы не изменяются, новые по 100%. Источник tools/audio/render_pale_deer.py, параметры установки tools/audio/install_pale_deer.py. Художественное качество синтеза и слышимость ещё требуют авторского прослушивания в игре; это пробная палитра, не утверждённый финальный арт.
""")
    with (DOC/"DECISIONS.md").open("a") as f:
        f.write("\n\n### D-076 — Пробные звуки бледных оленей (19 ноября 2026)\nАвтор прямо попросил добавить звуки. Восемь событий по три оригинальных синтетических варианта, отдельная группа «Олени» в существующем микшере, позиционные голоса, рёв, фырк и копыта с индивидуальной фазой. AI, генерация, другие ассеты/звуки не меняются. Начальная творческая палитра и уровни ChatGPT, не финальная художественная приёмка. [Технические детали](PALE_DEER_HERDS.md#d-076--пробное-авторское-по-задаче-аудио-19-ноября-2026).\n")
    print("Installed eight deer events, 8 mixer entries and 24 resource references; existing entries preserved.")
if __name__=="__main__": main()
