package com.drakou111.neohouse.utils;

import com.drakou111.neohouse.compat.ClientUi;
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
            var tabList = ClientUi.tabList(mc);
            for (var f : tabList.getClass().getDeclaredFields()) {
                if (!Component.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Component c = (Component) f.get(tabList);
                if (c != null && c.getString().toLowerCase().contains("you are in")) return true;
            }
            return false;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
