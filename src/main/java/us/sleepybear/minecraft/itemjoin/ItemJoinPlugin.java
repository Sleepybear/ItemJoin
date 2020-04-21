package us.sleepybear.minecraft.itemjoin;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
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
            if (i.containsKey("ench")) {
                List<Map> enchants = (List<Map>) i.get("ench");
                for (Map e : enchants) {
                    item.addEnchantment(Enchantment.get((int) e.get("id")).setLevel((int) e.get("level")));
                }
            }
            if (i.containsKey("slot")) {
                int slot = (int) i.get("slot");
                if (slot < 0 || slot > 39) {
                    getLogger().warning("Invalid slot (" + slot + ") for item: " + item.getName());
                    slot = currentSlot;
                }

                if (items.putIfAbsent(slot, item) != null) {
                    getLogger().warning("Not adding item " + item.getName() + " because another item already uses that slot.");
                }
                if (slot == currentSlot) {
                    currentSlot++;
                }
                continue;
            }
            while (items.containsKey(currentSlot)) {
                currentSlot++;
            }
            if (currentSlot >= 36) { // Leaving this as 36 so auto assigned slots don't roll over onto armor
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

        if (!firstJoinOnly && clearInvOnLoad) { // No need to clear on first join anyway
            if (!bypassPlayers.contains(player.getName())) {
                getLogger().debug("Clearing inventory of " + player.getName());
                inv.clearAll();
            }
        }
        if (items.size() > 0) {
            for (Map.Entry<Integer, Item> entry : items.entrySet()) {
                if (entry.getKey().intValue() >= 36) { // Should probably do more sanity checks of armor types
                    if (!entry.getValue().isArmor()) {
                        getLogger().warning("Unable to equip non-armor item in armor slot: " + entry.getValue().getName());
                        continue;
                    }
                }
                inv.setItem(entry.getKey(), entry.getValue(), false);
            }
            inv.sendContents(player);
        }
    }
}
