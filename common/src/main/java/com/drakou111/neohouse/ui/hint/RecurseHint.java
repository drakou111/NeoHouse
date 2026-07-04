package com.drakou111.neohouse.ui.hint;

import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class RecurseHint implements HintProvider {
    private final Random random = new Random();
    private final double nestChance;
    private final Supplier<List<HintProvider>> pool;

    public RecurseHint(double nestChance, Supplier<List<HintProvider>> pool) {
        this.nestChance = nestChance;
        this.pool = pool;
    }

    @Override
    public boolean shouldShow(Minecraft mc) {
        return random.nextDouble() < nestChance;
    }

    @Override
    public String getText(Minecraft mc) {
        return build(mc, 0);
    }

    private String build(Minecraft mc, int depth) {
        List<HintProvider> all = pool.get();
        HintProvider next = all.get(random.nextInt(all.size()));
        String inner = (next instanceof RecurseHint r)
                ? r.build(mc, depth + 1)
                : next.getText(mc);
        return "Here's a random tip: [" + inner + "]";
    }
}
