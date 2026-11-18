package com.tarkrakov.tearsofink.mixin;

import com.tarkrakov.tearsofink.world.InkLandsBiomeSource;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.TempleWorldAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mixin(LevelStorageSource.LevelStorageAccess.class)
abstract class TempleWorldStorageMixin implements TempleWorldAccess {
	@Unique
	private int tearsofink$storedLayout = 1;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void tearsofink$readTempleLayout(LevelStorageSource parent, String levelId, Path folder, CallbackInfo ci) {
		var access = (LevelStorageSource.LevelStorageAccess) (Object) this;
		// The dedicated server writes its first level.dat before MinecraftServer.createLevels.
		if (!access.hasWorldData()) {
			try (var contents = Files.list(folder)) {
				if (contents.allMatch(file -> switch (file.getFileName().toString()) {
					case "session.lock", "datapacks" -> true;
					default -> false;
				})) tearsofink$storedLayout = 0;
			} catch (IOException ignored) {
				// A world without readable provenance uses the legacy layout.
			}
			return;
		}
		for (var resource : new LevelResource[]{LevelResource.LEVEL_DATA_FILE, LevelResource.OLD_LEVEL_DATA_FILE}) {
			try {
				var biomeSource = NbtIo.readCompressed(access.getLevelPath(resource), NbtAccounter.unlimitedHeap())
						.getCompound("Data").getCompound("WorldGenSettings").getCompound("dimensions")
						.getCompound(ModDimensions.INK_LANDS.location().toString())
						.getCompound("generator").getCompound("biome_source");
				if (biomeSource.contains("temple_layout", Tag.TAG_INT) && biomeSource.getInt("temple_layout") == 2) {
					tearsofink$storedLayout = 2;
				}
				return;
			} catch (IOException ignored) {
				// Vanilla can restore from level.dat_old if the primary file cannot be read.
			}
		}
	}

	@Inject(method = "saveDataTag(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/level/storage/WorldData;Lnet/minecraft/nbt/CompoundTag;)V",
			at = @At("HEAD"))
	private void tearsofink$preserveTempleLayout(RegistryAccess registryAccess, WorldData data, CompoundTag player, CallbackInfo ci) {
		InkLandsBiomeSource.bindTempleLayout(registryAccess, tearsofink$storedLayout);
	}

	@Override
	public int tearsofink$storedTempleLayout() {
		return tearsofink$storedLayout;
	}
}
