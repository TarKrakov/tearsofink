package com.tarkrakov.tearsofink.client.compat;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;

/**
 * Узнаём, включён ли сейчас шейдерпак Iris. Без жёсткой зависимости от Iris:
 * если Iris не установлен — просто считаем, что шейдеров нет.
 */
public final class ShaderCompat {
	private static final boolean IRIS_LOADED = FabricLoader.getInstance().isModLoaded("iris");
	private static Object irisApi;
	private static Method isShaderPackInUse;
	private static boolean broken;

	private ShaderCompat() {}

	public static boolean shadersActive() {
		if (!IRIS_LOADED || broken) return false;
		try {
			if (isShaderPackInUse == null) {
				Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
				irisApi = apiClass.getMethod("getInstance").invoke(null);
				isShaderPackInUse = apiClass.getMethod("isShaderPackInUse");
			}
			return (boolean) isShaderPackInUse.invoke(irisApi);
		} catch (Throwable t) {
			broken = true;
			return false;
		}
	}
}
