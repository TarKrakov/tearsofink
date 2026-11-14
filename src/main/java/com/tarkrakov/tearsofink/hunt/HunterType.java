package com.tarkrakov.tearsofink.hunt;

/** Тип охотника — он же линия его оружия. Цвет — луч Охоты. */
public enum HunterType {
	SPIRE("spire", 0xD8DCE6),
	BLOOD("blood", 0xB0202A),
	INK("ink", 0x4A2266);

	public final String id;
	public final int beamColor;

	HunterType(String id, int beamColor) {
		this.id = id;
		this.beamColor = beamColor;
	}

	public static HunterType byIndex(int i) {
		HunterType[] v = values();
		return v[Math.floorMod(i, v.length)];
	}
}
