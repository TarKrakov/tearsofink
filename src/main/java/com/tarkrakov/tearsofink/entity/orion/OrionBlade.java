package com.tarkrakov.tearsofink.entity.orion;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Common-side evaluation of the authored sword's four-bone chain in world space. */
final class OrionBlade {
    record Segment(Vec3 base, Vec3 tip) {
        boolean touches(AABB bounds) {
            return contact(bounds, OrionCombat.BLADE_TOLERANCE) != null;
        }

        Vec3 contact(AABB bounds, double margin) {
            AABB padded = bounds.inflate(margin, Math.min(margin, .54), margin);
            return padded.contains(base) ? base : padded.clip(base, tip).orElse(padded.contains(tip) ? tip : null);
        }
    }

    static Segment sample(OrionCombat.Clip clip, float time, Vec3 origin, float yaw) {
        OrionExport.Track[] tracks = switch (clip) {
            case SLASH_COMBO -> OrionExport.SLASH_COMBO;
            case SPIN_DASH -> OrionExport.SPIN_DASH;
            case CLEAVE -> OrionExport.CLEAVE;
            default -> throw new IllegalArgumentException("No blade track for " + clip);
        };
        Matrix4f matrix = new Matrix4f();
        Vector3f position = new Vector3f(), rotation = new Vector3f(), scale = new Vector3f();
        for (int bone = 0; bone < OrionExport.PIVOTS.length; bone++) {
            position.zero();
            rotation.zero();
            scale.set(1);
            for (OrionExport.Track track : tracks) {
                if (track.bone() != bone) continue;
                Vector3f value = switch (track.channel()) {
                    case 0 -> position;
                    case 1 -> rotation;
                    default -> scale;
                };
                interpolate(value, track.keys(), time);
            }
            float[] pivot = OrionExport.PIVOTS[bone];
            matrix.translate(pivot[0] + position.x, pivot[1] - position.y, pivot[2] + position.z)
                    .rotateZYX(rotation.z * Mth.DEG_TO_RAD, rotation.y * Mth.DEG_TO_RAD, rotation.x * Mth.DEG_TO_RAD)
                    .scale(scale);
        }
        // Endpoints are the center of the cutting edge at the guard and the export's last tip cube.
        return new Segment(world(matrix.transformPosition(new Vector3f(0, 1.5f, -6)), origin, yaw),
                world(matrix.transformPosition(new Vector3f(0, -.5f, -45)), origin, yaw));
    }

    private static Vec3 world(Vector3f point, Vec3 origin, float yaw) {
        float unit = OrionCombat.SCALE / 16;
        return origin.add(new Vec3(point.x * unit, 1.501 * OrionCombat.SCALE - point.y * unit,
                -point.z * unit).yRot(-yaw * Mth.DEG_TO_RAD));
    }

    private static void interpolate(Vector3f target, float[][] keys, float time) {
        int after = 0;
        while (after < keys.length - 1 && keys[after][0] < time) after++;
        int before = Math.max(0, after - 1);
        float[] a = keys[before], b = keys[after];
        float fraction = after == before ? 0 : Mth.clamp((time - a[0]) / (b[0] - a[0]), 0, 1);
        float[] p = keys[Math.max(0, before - 1)], n = keys[Math.min(keys.length - 1, after + 1)];
        for (int axis = 1; axis <= 3; axis++)
            target.setComponent(axis - 1, b[4] == 0 ? Mth.lerp(fraction, a[axis], b[axis])
                    : Mth.catmullrom(fraction, p[axis], a[axis], b[axis], n[axis]));
    }

    private OrionBlade() {}
}
