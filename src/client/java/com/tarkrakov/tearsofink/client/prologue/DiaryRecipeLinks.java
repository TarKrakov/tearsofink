package com.tarkrakov.tearsofink.client.prologue;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.alchemy.AlchemyRecipe;
import com.tarkrakov.tearsofink.weapon.forge.ForgingRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Inline recipe references in diary localization, using the client's server-synced recipes.
 * Markup: [[recipe_id|localized words]]. The actual glyph style survives wrapping and pagination.
 * No JEI, external links, extra recipe data or server-side action is required.
 */
final class DiaryRecipeLinks {
    private static final Pattern LINK = Pattern.compile("\\[\\[([a-z0-9_./-]+)\\|([^\\]\\r\\n]+)\\]\\]");
    private static final String PREFIX = "toi_recipe:";
    private static final int LINK_COLOR = 0x863B20;
    private static final int INK = 0xFF39282A, PAPER = 0xFFF2DEB9, BORDER = 0xFF77513E;
    private static final int SLOT = 18, PANEL_WIDTH = 184;
    private DiaryRecipeLinks() {}

    static Component decorate(Component source) {
        String value = source.getString();
        Matcher matcher = LINK.matcher(value);
        if (!matcher.find()) return source;
        MutableComponent out = Component.empty().withStyle(source.getStyle());
        int end = 0;
        do {
            out.append(Component.literal(value.substring(end, matcher.start())));
            out.append(Component.literal(matcher.group(2)).withStyle(style ->
                    style.withColor(LINK_COLOR).withUnderlined(true).withInsertion(PREFIX + matcher.group(1))));
            end = matcher.end();
        } while (matcher.find());
        out.append(Component.literal(value.substring(end)));
        return out;
    }

    static ResourceLocation hit(Font font, FormattedCharSequence line, int x, int y, double mx, double my) {
        // Coordinates are the same book-local coordinates used to draw this exact wrapped line.
        if (my < y || my >= y + font.lineHeight || mx < x || mx >= x + font.width(line)) return null;
        Style style = font.getSplitter().componentStyleAtWidth(line, (int) (mx - x));
        String mark = style == null ? null : style.getInsertion();
        return mark != null && mark.startsWith(PREFIX)
                ? TearsOfInk.id(mark.substring(PREFIX.length())) : null;
    }

    private record Layout(String station, List<ItemStack> cells, int columns, int rows, boolean alchemy) {}
    private record Count(ItemStack stack, int count) {}
    private record Legend(ItemStack stack, List<FormattedCharSequence> lines) {}

    private static ItemStack sample(Ingredient ingredient, long frame) {
        ItemStack[] options = ingredient.getItems();
        return options.length == 0 ? ItemStack.EMPTY : options[(int) Math.floorMod(frame, options.length)];
    }

    private static Layout layout(Recipe<?> recipe) {
        int w, h;
        String station;
        boolean alchemy = false;
        if (recipe instanceof ShapedRecipe shaped) {
            w = shaped.getWidth(); h = shaped.getHeight(); station = "crafting";
        } else if (recipe instanceof ForgingRecipe forging) {
            w = forging.width(); h = forging.height(); station = "forging";
        } else if (recipe instanceof AlchemyRecipe) {
            w = 3; h = 2; station = "alchemy"; alchemy = true;
        } else if (recipe instanceof ShapelessRecipe) {
            w = 3; h = 3; station = "shapeless";
        } else return null;
        if (w < 1 || w > 3 || h < 1 || h > 3) return null;
        List<Ingredient> ingredients = recipe.getIngredients();
        long frame = System.currentTimeMillis() / 1000;
        List<ItemStack> cells = new ArrayList<>();
        for (int i = 0; i < 9; i++) cells.add(ItemStack.EMPTY);
        if (alchemy) {
            // The last ingredient of AlchemyRecipe is always the mandatory flask.
            if (ingredients.size() < 2 || ingredients.size() > 4) return null;
            for (int i = 0; i < ingredients.size() - 1; i++) cells.set(i, sample(ingredients.get(i), frame));
            cells.set(4, sample(ingredients.getLast(), frame));
        } else {
            if (ingredients.size() > w * h) return null;
            for (int i = 0; i < ingredients.size(); i++) {
                int col = i % w + (3 - w) / 2;
                int row = i / w;
                cells.set(row * 3 + col, sample(ingredients.get(i), frame));
            }
        }
        return new Layout(station, cells, 3, h == 3 || !alchemy ? 3 : 2, alchemy);
    }

    static void render(GuiGraphics g, Font font, Minecraft mc, ResourceLocation id,
                       int mouseX, int mouseY, int screenW, int screenH) {
        if (mc.level == null) return;
        var holder = mc.level.getRecipeManager().byKey(id);
        if (holder.isEmpty()) {
            unavailable(g, font, mouseX, mouseY);
            return;
        }
        Recipe<?> recipe = holder.get().value();
        Layout layout = layout(recipe);
        ItemStack result = recipe.getResultItem(mc.level.registryAccess());
        if (layout == null || result.isEmpty()) {
            unavailable(g, font, mouseX, mouseY);
            return;
        }

        List<FormattedCharSequence> title = font.split(result.getHoverName(), PANEL_WIDTH - 16);
        List<FormattedCharSequence> station = font.split(Component.translatable("diary.tears-of-ink.recipe." + layout.station()), PANEL_WIDTH - 16);
        Map<net.minecraft.world.item.Item, Count> counts = new LinkedHashMap<>();
        for (ItemStack stack : layout.cells()) if (!stack.isEmpty()) {
            counts.compute(stack.getItem(), (key, old) -> new Count(stack, old == null ? 1 : old.count() + 1));
        }
        List<Legend> legend = new ArrayList<>();
        int legendHeight = 0;
        for (Count count : counts.values()) {
            var lines = font.split(Component.literal(count.count() + " × ").append(count.stack().getHoverName()), PANEL_WIDTH - 38);
            legend.add(new Legend(count.stack(), lines));
            legendHeight += Math.max(18, lines.size() * 10);
        }
        int gridY = 10 + title.size() * 10 + station.size() * 10 + 4;
        int gridHeight = layout.rows() * SLOT;
        int height = gridY + gridHeight + 8 + legendHeight + 5;
        float scale = Math.min(1f, Math.min((screenW - 8f) / PANEL_WIDTH, (screenH - 8f) / height));
        if (scale <= 0) return;
        int actualW = (int) Math.ceil(PANEL_WIDTH * scale), actualH = (int) Math.ceil(height * scale);
        int x = mouseX + 12 + actualW <= screenW - 4 ? mouseX + 12 : mouseX - actualW - 12;
        x = Math.max(4, Math.min(x, screenW - actualW - 4));
        int y = Math.max(4, Math.min(mouseY + 9, screenH - actualH - 4));
        g.pose().pushPose();
        g.pose().translate(x, y, 400);
        g.pose().scale(scale, scale, 1f);
        g.fill(0, 0, PANEL_WIDTH, height, BORDER);
        g.fill(2, 2, PANEL_WIDTH - 2, height - 2, PAPER);
        int yy = 7;
        for (var line : title) {
            g.drawString(font, line, (PANEL_WIDTH - font.width(line)) / 2, yy, INK, false);
            yy += 10;
        }
        for (var line : station) {
            g.drawString(font, line, (PANEL_WIDTH - font.width(line)) / 2, yy, 0xFF79503C, false);
            yy += 10;
        }
        final int gx = 24;
        for (int i = 0; i < layout.rows() * 3; i++) {
            if (layout.alchemy() && i >= 3 && i != 4) continue;
            int sx = gx + (i % 3) * SLOT, sy = gridY + (i / 3) * SLOT;
            slot(g, font, layout.cells().get(i), sx, sy);
        }
        int midY = gridY + gridHeight / 2;
        g.fill(90, midY - 1, 112, midY + 2, BORDER);
        for (int i = 0; i < 5; i++) g.fill(108 + i, midY - 5 + i, 109 + i, midY + 6 - i, BORDER);
        slot(g, font, result, 128, midY - 9);
        yy = gridY + gridHeight + 6;
        g.fill(8, yy - 3, PANEL_WIDTH - 8, yy - 2, BORDER);
        for (Legend item : legend) {
            g.renderItem(item.stack(), 8, yy);
            int textY = yy + 3;
            for (var line : item.lines()) {
                g.drawString(font, line, 30, textY, INK, false);
                textY += 10;
            }
            yy += Math.max(18, item.lines().size() * 10);
        }
        g.pose().popPose();
    }

    private static void slot(GuiGraphics g, Font font, ItemStack stack, int x, int y) {
        g.fill(x, y, x + SLOT, y + SLOT, BORDER);
        g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0xFFDBC49E);
        if (!stack.isEmpty()) {
            g.renderItem(stack, x + 1, y + 1);
            g.renderItemDecorations(font, stack, x + 1, y + 1);
        }
    }

    private static void unavailable(GuiGraphics g, Font font, int x, int y) {
        g.renderTooltip(font, Component.translatable("diary.tears-of-ink.recipe.unavailable"), x, y);
    }
}
