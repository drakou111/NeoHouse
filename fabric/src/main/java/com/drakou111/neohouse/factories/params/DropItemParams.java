package com.drakou111.neohouse.factories.params;

import com.drakou111.neohouse.constants.Teleport;

public class DropItemParams {
    public String snbtData = "";
    public Teleport teleport = null;
    public String customLocation = null;
    public boolean dropNaturally = true;
    public boolean disableItemMerging = false;
    public long despawnDuration = 6000;
    public long pickupDelay = 10;
    public boolean prioritizePlayer = false;
    public boolean fallbackToInventory = false;
    public String note = null;

    public DropItemParams snbtData(String v)           { this.snbtData = v; return this; }
    public DropItemParams teleport(Teleport v)         { this.teleport = v; return this; }
    public DropItemParams customLocation(String v)     { this.customLocation = v; return this; }
    public DropItemParams dropNaturally(boolean v)     { this.dropNaturally = v; return this; }
    public DropItemParams disableItemMerging(boolean v){ this.disableItemMerging = v; return this; }
    public DropItemParams despawnDuration(long v)      { this.despawnDuration = v; return this; }
    public DropItemParams pickupDelay(long v)          { this.pickupDelay = v; return this; }
    public DropItemParams prioritizePlayer(boolean v)  { this.prioritizePlayer = v; return this; }
    public DropItemParams fallbackToInventory(boolean v){ this.fallbackToInventory = v; return this; }
    public DropItemParams note(String v)               { this.note = v; return this; }
}
