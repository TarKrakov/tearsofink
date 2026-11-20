package com.tarkrakov.tearsofink.client.armor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tarkrakov.tearsofink.TearsOfInk;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Rigid armor fitted to the actual already-animated humanoid parts. BB coordinates are y-up,
 * player's right is +X; MC model coordinates are y-down, player's right is -X.
 * UV units are the BB logical texture dimensions, NOT the physical PNG pixels. */
public final class AuthoredArmorRenderer implements ArmorRenderer {
    private enum Bone {
        HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG;
        ModelPart part(HumanoidModel<?> model) {
            return switch (this) {
                case HEAD -> model.head;
                case BODY -> model.body;
                case RIGHT_ARM -> model.rightArm;
                case LEFT_ARM -> model.leftArm;
                case RIGHT_LEG -> model.rightLeg;
                case LEFT_LEG -> model.leftLeg;
            };
        }
        static Bone named(String name) {
            return switch (name) {
                case "head" -> HEAD; case "body" -> BODY;
                case "right_arm" -> RIGHT_ARM; case "left_arm" -> LEFT_ARM;
                case "right_leg" -> RIGHT_LEG; case "left_leg" -> LEFT_LEG;
                default -> null;
            };
        }
    }
    private record Binding(EquipmentSlot slot, Bone bone) {}
    private static final Map<String, Binding> GROUPS = Map.of(
            "helmet", new Binding(EquipmentSlot.HEAD, Bone.HEAD),
            "chestplate", new Binding(EquipmentSlot.CHEST, Bone.BODY),
            "chestplate_right_arm", new Binding(EquipmentSlot.CHEST, Bone.RIGHT_ARM),
            "chestplate_left_arm", new Binding(EquipmentSlot.CHEST, Bone.LEFT_ARM),
            "leggings_waist", new Binding(EquipmentSlot.LEGS, Bone.BODY),
            "leggings_right", new Binding(EquipmentSlot.LEGS, Bone.RIGHT_LEG),
            "leggings_left", new Binding(EquipmentSlot.LEGS, Bone.LEFT_LEG),
            "boot_right", new Binding(EquipmentSlot.FEET, Bone.RIGHT_LEG),
            "boot_left", new Binding(EquipmentSlot.FEET, Bone.LEFT_LEG));

    private record Vertex(Vector3f position, float u, float v) {}
    private record Quad(Vertex[] vertices, Vector3f normal) {}
    private record Turn(Vector3f origin, Quaternionf rotation) {
        Vector3f apply(Vector3f point) {
            return rotation.transform(point.sub(origin)).add(origin);
        }
    }
    private static final Map<Item, AuthoredArmorRenderer> RENDERERS = new HashMap<>();
    private final ResourceLocation texture;
    private final Map<EquipmentSlot, Map<Bone, List<Quad>>> mesh = new EnumMap<>(EquipmentSlot.class);
    private final Map<Bone, Vector3f> neutralPivots = new EnumMap<>(Bone.class);
    private static final List<EquipmentSlot> ITEM_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private AuthoredArmorRenderer(ResourceLocation texture) { this.texture = texture; }

    public static void init() {
        registerSet("pale_heavy_armor", "pale_closed_helmet", "pale_cuirass", "pale_plate_leggings", "pale_sabatons");
        registerSet("pale_medium_armor", "pale_chain_hood", "pale_brigandine", "pale_reinforced_leggings", "pale_leather_boots");
        registerSet("pale_light_armor", "pale_headband", "pale_quilted_jacket", "pale_pants", "pale_shoes");
    }

    /** Additional sets use the same group contract, with no new renderer class. */
    private static void registerSet(String file, String... ids) {
        String path = "/assets/tears-of-ink/armor_models/" + file + ".bbmodel";
        try (var stream = AuthoredArmorRenderer.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing " + path);
            var model = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            var textures = model.getAsJsonArray("textures");
            if (textures.size() != 1) throw new IllegalArgumentException("Expected a single armor texture");
            var tex = textures.get(0).getAsJsonObject();
            String filename = tex.get("name").getAsString();
            if (!filename.matches("[a-z0-9_./-]+\\.png") || filename.contains("..")) throw new IllegalArgumentException("Invalid texture name");
            var result = new AuthoredArmorRenderer(TearsOfInk.id("textures/armor/" + filename));
            var resolution = model.getAsJsonObject("resolution");
            float uWidth = number(tex, "uv_width", number(resolution, "width", 64));
            float vHeight = number(tex, "uv_height", number(resolution, "height", 64));
            if (uWidth <= 0 || vHeight <= 0) throw new IllegalArgumentException("Invalid UV dimensions");
            result.load(model, uWidth, vHeight);
            Item[] items = new Item[ids.length];
            for (int i = 0; i < ids.length; i++) {
                var id = TearsOfInk.id(ids[i]);
                if (!BuiltInRegistries.ITEM.containsKey(id)) throw new IllegalArgumentException("Unknown item " + id);
                items[i] = BuiltInRegistries.ITEM.get(id);
            }
            if (items.length != ITEM_SLOTS.size()) throw new IllegalArgumentException("Expected four armor items");
            // Bake a standalone item mesh from the same geometry, once per slot.
            List<List<Quad>> previews = new ArrayList<>();
            for (EquipmentSlot slot : ITEM_SLOTS) previews.add(result.itemMesh(slot));
            ArmorRenderer.register(result, items);
            for (int i = 0; i < items.length; i++) {
                RENDERERS.put(items[i], result);
                List<Quad> preview = previews.get(i);
                BuiltinItemRendererRegistry.INSTANCE.register(items[i],
                        (stack, mode, pose, buffers, light, overlay) ->
                                result.renderItemPreview(preview, stack, pose, buffers, light, overlay));
            }
        } catch (Exception e) {
            // Leave the vanilla placeholder rather than preventing the client from starting.
            TearsOfInk.LOGGER.error("Unable to load authored armor {}", file, e);
        }
    }

    private void load(JsonObject model, float uWidth, float vHeight) {
        Map<String, JsonObject> groups = index(model.getAsJsonArray("groups"));
        Map<String, JsonObject> cubes = index(model.getAsJsonArray("elements"));
        walk(model.getAsJsonArray("outliner"), groups, cubes, null, null, null, List.of(), uWidth, vHeight);
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            if (!mesh.containsKey(slot)) throw new IllegalArgumentException("No geometry for " + slot);
        }
    }
    private static Map<String, JsonObject> index(JsonArray array) {
        Map<String, JsonObject> map = new HashMap<>();
        for (JsonElement value : array) {
            JsonObject obj = value.getAsJsonObject();
            map.put(obj.get("uuid").getAsString(), obj);
        }
        return map;
    }
    private void walk(JsonArray nodes, Map<String, JsonObject> groups, Map<String, JsonObject> cubes,
                      Bone bone, Vector3f pivot, Binding binding, List<Turn> turns, float uWidth, float vHeight) {
        for (JsonElement node : nodes) {
            if (node.isJsonPrimitive()) {
                JsonObject cube = cubes.get(node.getAsString());
                if (cube == null) throw new IllegalArgumentException("Unknown element");
                if (binding != null && enabled(cube, "export")) {
                    if (bone != binding.bone()) throw new IllegalArgumentException("Incorrect armor bone");
                    bake(cube, pivot, binding, turns, uWidth, vHeight);
                }
                continue; // reference/mannequin outside the slot groups is not exported
            }
            JsonObject branch = node.getAsJsonObject();
            JsonObject group = groups.get(branch.get("uuid").getAsString());
            if (group == null) throw new IllegalArgumentException("Unknown group");
            if (!enabled(group, "export")) continue;
            String name = group.get("name").getAsString();
            Bone nextBone = Bone.named(name);
            Vector3f origin = vector(group, "origin");
            Vector3f rotation = vector(group, "rotation");
            if (nextBone != null || name.equals("root")) {
                if (rotation.lengthSquared() != 0) throw new IllegalArgumentException("Non-neutral player bone " + name);
            }
            Binding nextBinding = GROUPS.getOrDefault(name, binding);
            List<Turn> nextTurns = turns;
            if (nextBone != null) {
                neutralPivots.put(nextBone, new Vector3f(origin));
                nextTurns = List.of();
                nextBinding = GROUPS.get(name);
            } else if (rotation.lengthSquared() != 0) {
                nextTurns = new ArrayList<>(turns);
                nextTurns.add(turn(origin, rotation));
            }
            walk(branch.getAsJsonArray("children"), groups, cubes,
                    nextBone == null ? bone : nextBone, nextBone == null ? pivot : origin,
                    nextBinding, nextTurns, uWidth, vHeight);
        }
    }

    private void bake(JsonObject cube, Vector3f pivot, Binding binding, List<Turn> turns, float uWidth, float vHeight) {
        if (cube.has("type") && !cube.get("type").getAsString().equals("cube")) throw new IllegalArgumentException("Only cubes are supported");
        float inflate = number(cube, "inflate", 0);
        Vector3f from = vector(cube, "from").sub(inflate, inflate, inflate);
        Vector3f to = vector(cube, "to").add(inflate, inflate, inflate);
        Turn local = turn(vector(cube, "origin"), vector(cube, "rotation"));
        var quads = mesh.computeIfAbsent(binding.slot(), key -> new EnumMap<>(Bone.class))
                .computeIfAbsent(binding.bone(), key -> new ArrayList<>());
        for (var entry : cube.getAsJsonObject("faces").entrySet()) {
            JsonObject face = entry.getValue().getAsJsonObject();
            if (!enabled(face, "enabled") || !face.has("texture") || face.get("texture").isJsonNull()) continue;
            if (face.get("texture").isJsonPrimitive() && face.get("texture").getAsJsonPrimitive().isBoolean()) continue;
            if (face.get("texture").getAsInt() != 0) throw new IllegalArgumentException("Invalid face texture index");
            var uv = face.getAsJsonArray("uv");
            float u0 = uv.get(0).getAsFloat(), v0 = uv.get(1).getAsFloat();
            float u1 = uv.get(2).getAsFloat(), v1 = uv.get(3).getAsFloat();
            int rotation = Math.floorMod((int) number(face, "rotation", 0), 360);
            if (rotation % 90 != 0) throw new IllegalArgumentException("Unsupported face UV rotation");
            // UV corners in outward winding. BB face mapping follows its CubeFace.UVToLocal.
            Vertex[] vertices = new Vertex[4];
            for (int i = 0; i < 4; i++) {
                float u = i >= 2 ? 1 : 0;
                float v = i == 1 || i == 2 ? 1 : 0;
                float x = u, y = v;
                for (int a = 0; a < rotation; a += 90) { float oldX = x; x = 1 - y; y = oldX; }
                Vector3f point = local.apply(facePoint(entry.getKey(), from, to, x, y));
                for (int j = turns.size() - 1; j >= 0; j--) point = turns.get(j).apply(point);
                point.sub(pivot).mul(-1f / 16, -1f / 16, 1f / 16);
                vertices[i] = new Vertex(point, lerp(u0, u1, u) / uWidth, lerp(v0, v1, v) / vHeight);
            }
            Vector3f normal = new Vector3f(vertices[1].position()).sub(vertices[0].position())
                    .cross(new Vector3f(vertices[2].position()).sub(vertices[0].position()));
            if (normal.lengthSquared() < 1e-12f) continue;
            quads.add(new Quad(vertices, normal.normalize()));
        }
    }
    private static Vector3f facePoint(String face, Vector3f a, Vector3f b, float u, float v) {
        return switch (face) {
            case "north" -> new Vector3f(lerp(b.x, a.x, u), lerp(b.y, a.y, v), a.z);
            case "south" -> new Vector3f(lerp(a.x, b.x, u), lerp(b.y, a.y, v), b.z);
            case "east" -> new Vector3f(b.x, lerp(b.y, a.y, v), lerp(b.z, a.z, u));
            case "west" -> new Vector3f(a.x, lerp(b.y, a.y, v), lerp(a.z, b.z, u));
            case "up" -> new Vector3f(lerp(a.x, b.x, u), b.y, lerp(a.z, b.z, v));
            case "down" -> new Vector3f(lerp(a.x, b.x, u), a.y, lerp(b.z, a.z, v));
            default -> throw new IllegalArgumentException("Unknown face " + face);
        };
    }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static Turn turn(Vector3f origin, Vector3f rotation) {
        float d = (float) (Math.PI / 180);
        return new Turn(origin, new Quaternionf().rotationZYX(rotation.z * d, rotation.y * d, rotation.x * d));
    }
    private static float number(JsonObject obj, String key, float fallback) {
        return obj.has(key) ? obj.get(key).getAsFloat() : fallback;
    }
    private static Vector3f vector(JsonObject obj, String key) {
        if (!obj.has(key)) return new Vector3f();
        JsonArray arr = obj.getAsJsonArray(key);
        return new Vector3f(arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat());
    }
    private static boolean enabled(JsonObject obj, String key) { return !obj.has(key) || obj.get(key).getAsBoolean(); }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, ItemStack stack, LivingEntity entity,
                       EquipmentSlot slot, int light, HumanoidModel<LivingEntity> contextModel) {
        var parts = mesh.get(slot);
        if (parts == null) return;
        VertexConsumer consumer = buffer(buffers, stack);
        boolean slimArms = slimArms(entity);
        for (var entry : parts.entrySet()) {
            Bone bone = entry.getKey();
            renderPart(pose, consumer, bone.part(contextModel), entry.getValue(), light, bone, slimArms);
        }
    }

    private static boolean slimArms(LivingEntity entity) {
        return entity instanceof AbstractClientPlayer player
                && player.getSkin().model() == PlayerSkin.Model.SLIM;
    }

    private VertexConsumer buffer(MultiBufferSource buffers, ItemStack stack) {
        return ItemRenderer.getArmorFoilBuffer(buffers, RenderType.entityCutout(texture), stack.hasFoil());
    }
    private static void renderPart(PoseStack pose, VertexConsumer consumer, ModelPart part, List<Quad> quads,
                                   int light, Bone bone, boolean slimArms) {
        pose.pushPose();
        // Keep the game's animated arm pivot, even when crouching or holding a shield.
        part.translateAndRotate(pose);
        if (slimArms && (bone == Bone.RIGHT_ARM || bone == Bone.LEFT_ARM)) {
            boolean right = bone == Bone.RIGHT_ARM;
            pose.translate(ArmorArmFit.slimOffsetX(right), 0, 0);
            pose.scale(ArmorArmFit.SLIM_X_SCALE, 1, 1);
        }
        var matrix = pose.last();
        for (Quad quad : quads) {
            Vector3f n = quad.normal();
            for (Vertex vertex : quad.vertices()) {
                Vector3f p = vertex.position();
                consumer.addVertex(matrix, p.x, p.y, p.z).setColor(0xFFFFFFFF)
                        .setUv(vertex.u(), vertex.v()).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light).setNormal(matrix, n.x, n.y, n.z);
            }
        }
        pose.popPose();
    }

    /** Undo the player bone-local transform to recover neutral BB geometry, then center
     * each equipped item's own mesh in a unit item cube. No mannequin or other slots.
     * Fit the bounding sphere (diameter 0.90) so every display rotation stays inside
     * the icon without clipping; both boots/legs and both sleeves retain their spacing. */
    private List<Quad> itemMesh(EquipmentSlot slot) {
        List<Quad> result = new ArrayList<>();
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
        for (var entry : mesh.get(slot).entrySet()) {
            Vector3f pivot = new Vector3f(neutralPivots.get(entry.getKey())).div(16f);
            for (Quad quad : entry.getValue()) {
                Vertex[] vertices = new Vertex[4];
                for (int i = 0; i < 4; i++) {
                    Vertex vertex = quad.vertices()[i];
                    Vector3f point = new Vector3f(vertex.position()).mul(-1f, -1f, 1f).add(pivot);
                    min.min(point);
                    max.max(point);
                    vertices[i] = new Vertex(point, vertex.u(), vertex.v());
                }
                result.add(new Quad(vertices, new Vector3f(quad.normal()).mul(-1f, -1f, 1f)));
            }
        }
        if (result.isEmpty()) throw new IllegalArgumentException("Empty item mesh " + slot);
        Vector3f center = new Vector3f(min).add(max).mul(0.5f);
        float radius = 0;
        for (Quad quad : result) {
            for (Vertex vertex : quad.vertices()) {
                radius = Math.max(radius, vertex.position().sub(center).length());
            }
        }
        if (!(radius > 0) || !Float.isFinite(radius)) throw new IllegalArgumentException("Invalid item bounds " + slot);
        for (Quad quad : result) {
            for (Vertex vertex : quad.vertices()) vertex.position().mul(0.45f / radius);
        }
        return List.copyOf(result);
    }

    private void renderItemPreview(List<Quad> quads, ItemStack stack, PoseStack pose,
                                   MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        // ItemRenderer already applies the JSON display transform and translates by -0.5.
        pose.translate(0.5, 0.5, 0.5);
        VertexConsumer consumer = buffer(buffers, stack);
        var matrix = pose.last();
        for (Quad quad : quads) {
            Vector3f normal = quad.normal();
            for (Vertex vertex : quad.vertices()) {
                Vector3f p = vertex.position();
                consumer.addVertex(matrix, p.x, p.y, p.z).setColor(0xFFFFFFFF)
                        .setUv(vertex.u(), vertex.v()).setOverlay(overlay)
                        .setLight(light).setNormal(matrix, normal.x, normal.y, normal.z);
            }
        }
        pose.popPose();
    }

    /** Vanilla first-person visible hands (empty hand/map). Weapon-only FP paths do not render a player arm. */
    public static void renderHand(PoseStack pose, MultiBufferSource buffers, ItemStack chest,
                                  ModelPart arm, boolean right, int light, AbstractClientPlayer player) {
        var renderer = RENDERERS.get(chest.getItem());
        if (renderer == null) return;
        var parts = renderer.mesh.get(EquipmentSlot.CHEST);
        if (parts == null) return;
        Bone bone = right ? Bone.RIGHT_ARM : Bone.LEFT_ARM;
        var quads = parts.get(bone);
        if (quads != null) {
            renderPart(pose, renderer.buffer(buffers, chest), arm, quads, light, bone, slimArms(player));
        }
    }
}
