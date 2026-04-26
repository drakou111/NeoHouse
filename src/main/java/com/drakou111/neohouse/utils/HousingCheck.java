package com.drakou111.neohouse.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Scoreboard;

public class HousingCheck {

    public static boolean isOnHypixel() {
        ServerData srv = Minecraft.getInstance().getCurrentServer();
        if (srv == null) return false;
        String ip = srv.ip.toLowerCase();
        return ip.contains("hypixel.net") || ip.contains("hypixel.io");
    }

    public static boolean isInHousing() {
        if (!isOnHypixel()) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;

        return hasHousingScoreboard(mc) && hasHousingFooter(mc);
    }

    private static boolean hasHousingScoreboard(Minecraft mc) {
        Scoreboard sb = mc.level.getScoreboard();

        return sb.getObjectives().stream().anyMatch(obj -> {
            String name = obj.getName().toLowerCase();
            String display = obj.getDisplayName().getString().toLowerCase();
            return name.contains("housing") || display.contains("housing");
        });
    }

    private static boolean hasHousingFooter(Minecraft mc) {
        try {
            var tabList = mc.gui.getTabList();
            var footerField = tabList.getClass().getDeclaredField("footer");
            footerField.setAccessible(true);

            Component footer = (Component) footerField.get(tabList);
            return footer != null && footer.getString().toLowerCase().contains("you are in");
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
