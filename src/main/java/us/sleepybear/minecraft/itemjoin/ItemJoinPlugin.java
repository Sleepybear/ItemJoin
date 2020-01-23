package us.sleepybear.minecraft.itemjoin;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemJoinPlugin extends PluginBase implements Listener {

    private boolean clearInvOnLoad = false;
    private Item[] items;
    private List<String> bypassPlayers;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        Config config = getConfig();

        clearInvOnLoad = config.getBoolean("clearOnJoin", false);
        List<Map> configItems = config.getMapList("items");
        List<Item> list = new ArrayList<>();
        bypassPlayers = config.getList("bypass");
        for (Map i : configItems) {
            list.add(Item.get((int) i.get("itemId"), (int) i.get("meta"), (int) i.get("count")));
        }
        items = list.toArray(new Item[0]);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerInventory inv = event.getPlayer().getInventory();
        if (inv == null) return;

        if (clearInvOnLoad) {
            if (!bypassPlayers.contains(event.getPlayer().getName()))
                inv.clearAll();
        }
        if (items != null && items.length > 0) {
            inv.addItem(items);
        }
    }
}
