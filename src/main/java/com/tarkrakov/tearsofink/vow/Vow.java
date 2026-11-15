package com.tarkrakov.tearsofink.vow;

/** Обет игрока: Путь Чернил или Путь Души (NONE — ещё не выбрал). Решает, какие умения доступны. */
public enum Vow {
	NONE("none"), INK("ink"), SOUL("soul");

	public final String id;

	/** Обет своего игрока на клиенте (подсказки книг, ввод прыжков). Ставит клиент по VowSync. */
	public static volatile Vow CLIENT = NONE;

	Vow(String id) {
		this.id = id;
	}

	public static Vow byIndex(int i) {
		Vow[] all = values();
		return i >= 0 && i < all.length ? all[i] : NONE;
	}

	public Vow other() {
		return this == INK ? SOUL : this == SOUL ? INK : NONE;
	}
}
