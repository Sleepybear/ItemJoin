package us.sleepybear.minecraft.itemjoin;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class ItemJoinPlugin extends PluginBase implements Listener {

    private boolean clearInvOnLoad = false;
    private boolean firstJoinOnly = false;
    private Map<Integer, Item> items = new IdentityHashMap<>();
    private List<String> bypassPlayers = new ArrayList<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        Config config = getConfig();

        if (!config.exists("firstJoinOnly")) {
            config.set("firstJoinOnly", false);
            config.save();
        }
        clearInvOnLoad = config.getBoolean("clearOnJoin", false);
        firstJoinOnly = config.getBoolean("firstJoinOnly", false);
        List<Map> configItems = config.getMapList("items");
        if (config.exists("bypass") && config.getList("bypass") != null) {
            bypassPlayers.addAll(config.getList("bypass"));
        }
        int currentSlot = 0;
        for (Map i : configItems) {
            Item item = Item.get((int) i.get("itemId"), (int) i.get("meta"), (int) i.get("count"));
            if (i.containsKey("slot")) {
                if (items.putIfAbsent((int) i.get("slot"), item) != null) {
                    getLogger().warning("Not adding item " + item.getName() + " because another item already uses that slot.");
                }
                if ((int) i.get("slot") == currentSlot) {
                    currentSlot++;
                }
                continue;
            }
            while (items.containsKey(currentSlot)) {
                currentSlot++;
            }
            if (currentSlot >= 36) {
                getLogger().warning("Exceeded maximum slots");
                break;
            }
            items.put(currentSlot, item);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();
        if (inv == null) return;

        if (firstJoinOnly && player.hasPlayedBefore()) {
            getLogger().debug("Player has played before, not giving items.");
            return;
        }

        if (clearInvOnLoad) {
            if (!bypassPlayers.contains(player.getName())) {
                getLogger().debug("Clearing inventory of " + player.getName());
                inv.clearAll();
            }
        }
        if (items.size() > 0) {
            for (Map.Entry<Integer, Item> entry : items.entrySet()) {
                inv.setItem(entry.getKey(), entry.getValue(), false);
            }
            inv.sendContents(player);
        }
    }
}
