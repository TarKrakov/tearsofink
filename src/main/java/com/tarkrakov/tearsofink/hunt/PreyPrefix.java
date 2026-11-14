package com.tarkrakov.tearsofink.hunt;

/** Особые цели Охоты: резвая (быстрее ходит и бьёт), гигант (больше и крепче), разделённая (две маленьких). */
public enum PreyPrefix {
	NONE("none"),
	SWIFT("swift"),
	GIANT("giant"),
	SPLIT("split");

	public final String id;

	PreyPrefix(String id) {
		this.id = id;
	}
}
