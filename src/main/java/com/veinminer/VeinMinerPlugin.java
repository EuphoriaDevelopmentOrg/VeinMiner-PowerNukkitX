package com.veinminer;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.item.Item;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;

import java.util.*;

public class VeinMinerPlugin extends PluginBase implements Listener {

    private static final int MAX_BLOCKS = 64;
    private Set<String> veinBlocks = new HashSet<>();
    private boolean autoPickupEnabled;
    private String fullInventoryAction;
    private String inventoryFullMessage;
    private boolean loggingEnabled;
    private boolean logVeinMining;
    private boolean logConfigLoading;

    @Override
    public void onEnable() {
        // Save default config
        this.saveDefaultConfig();
        
        // Load configuration
        loadConfig();
        
        // Load vein blocks
        loadVeinBlocks();
        
        // Register events
        this.getServer().getPluginManager().registerEvents(this, this);
        
        // Fancy startup message
        this.getLogger().info(TextFormat.AQUA + "═══════════════════════════════════════");
        this.getLogger().info(TextFormat.GOLD + "  ⚯ " + TextFormat.BOLD + "VeinMiner" + TextFormat.RESET + TextFormat.GOLD + " v1.0.1 ⚯");
        this.getLogger().info(TextFormat.GREEN + "  ✓ Plugin Enabled Successfully!");
        this.getLogger().info(TextFormat.YELLOW + "  » Max blocks per vein: " + TextFormat.WHITE + MAX_BLOCKS);
        this.getLogger().info(TextFormat.YELLOW + "  » Auto-pickup: " + TextFormat.WHITE + (autoPickupEnabled ? "Enabled" : "Disabled"));
        this.getLogger().info(TextFormat.YELLOW + "  » Sneak required: " + TextFormat.WHITE + "Yes");
        this.getLogger().info(TextFormat.YELLOW + "  » Vein blocks loaded: " + TextFormat.WHITE + veinBlocks.size());
        this.getLogger().info(TextFormat.AQUA + "═══════════════════════════════════════");
    }
    
    @Override
    public void onDisable() {
        this.getLogger().info(TextFormat.RED + "VeinMiner plugin disabled!");
    }
    
    private void loadConfig() {
        Config config = this.getConfig();
        
        // Load auto-pickup settings
        autoPickupEnabled = config.getBoolean("auto-pickup.enabled", true);
        fullInventoryAction = config.getString("auto-pickup.full-inventory-action", "drop").toLowerCase();
        
        // Load logging settings
        loggingEnabled = config.getBoolean("logging.enabled", true);
        logVeinMining = config.getBoolean("logging.log-vein-mining", true);
        logConfigLoading = config.getBoolean("logging.log-config-loading", true);
        
        // Load messages
        inventoryFullMessage = config.getString("messages.inventory-full", "&eInventory full! {count} items were {action}.");
        
        if (loggingEnabled && logConfigLoading) {
            this.getLogger().info(TextFormat.GREEN + "[Config] Auto-pickup: " + (autoPickupEnabled ? "enabled" : "disabled"));
            this.getLogger().info(TextFormat.GREEN + "[Config] Full inventory action: " + fullInventoryAction);
            this.getLogger().info(TextFormat.GREEN + "[Config] Console logging: enabled");
        }
    }
    
    private void loadVeinBlocks() {
        // Ores
        veinBlocks.add("minecraft:coal_ore");
        veinBlocks.add("minecraft:iron_ore");
        veinBlocks.add("minecraft:gold_ore");
        veinBlocks.add("minecraft:diamond_ore");
        veinBlocks.add("minecraft:emerald_ore");
        veinBlocks.add("minecraft:lapis_ore");
        veinBlocks.add("minecraft:redstone_ore");
        veinBlocks.add("minecraft:lit_redstone_ore");
        veinBlocks.add("minecraft:copper_ore");
        veinBlocks.add("minecraft:deepslate_coal_ore");
        veinBlocks.add("minecraft:deepslate_iron_ore");
        veinBlocks.add("minecraft:deepslate_gold_ore");
        veinBlocks.add("minecraft:deepslate_diamond_ore");
        veinBlocks.add("minecraft:deepslate_emerald_ore");
        veinBlocks.add("minecraft:deepslate_lapis_ore");
        veinBlocks.add("minecraft:deepslate_redstone_ore");
        veinBlocks.add("minecraft:lit_deepslate_redstone_ore");
        veinBlocks.add("minecraft:deepslate_copper_ore");
        veinBlocks.add("minecraft:quartz_ore");
        veinBlocks.add("minecraft:nether_gold_ore");
        veinBlocks.add("minecraft:ancient_debris");
        
        // Logs (natural tree logs only)
        veinBlocks.add("minecraft:oak_log");
        veinBlocks.add("minecraft:spruce_log");
        veinBlocks.add("minecraft:birch_log");
        veinBlocks.add("minecraft:jungle_log");
        veinBlocks.add("minecraft:acacia_log");
        veinBlocks.add("minecraft:dark_oak_log");
        veinBlocks.add("minecraft:mangrove_log");
        veinBlocks.add("minecraft:cherry_log");
        veinBlocks.add("minecraft:crimson_stem");
        veinBlocks.add("minecraft:warped_stem");
        
        // Leaves
        veinBlocks.add("minecraft:oak_leaves");
        veinBlocks.add("minecraft:spruce_leaves");
        veinBlocks.add("minecraft:birch_leaves");
        veinBlocks.add("minecraft:jungle_leaves");
        veinBlocks.add("minecraft:acacia_leaves");
        veinBlocks.add("minecraft:dark_oak_leaves");
        veinBlocks.add("minecraft:mangrove_leaves");
        veinBlocks.add("minecraft:cherry_leaves");
        
        if (loggingEnabled && logConfigLoading) {
            this.getLogger().info(TextFormat.GREEN + "[Config] Loaded " + veinBlocks.size() + " vein-mineable block types");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Check if player is sneaking (crouching)
        if (!player.isSneaking()) {
            return;
        }
        
        // Check if the block is vein-mineable
        String blockId = block.getId();
        if (!veinBlocks.contains(blockId)) {
            return;
        }
        
        // Check if player has the proper tool
        Item tool = player.getInventory().getItemInHand();
        if (!isProperTool(blockId, tool)) {
            return;
        }
        
        // Find and break all connected blocks of the same type
        Set<Block> vein = findVein(block, new HashSet<>());
        
        if (vein.size() > 1) {
            // Cancel the event to prevent normal drop behavior
            event.setCancelled(true);
            
            // Log vein mining activation
            if (loggingEnabled && logVeinMining) {
                this.getLogger().info(TextFormat.YELLOW + "[VeinMine] Player: " + player.getName() + 
                    " | Block: " + blockId + " | Vein size: " + vein.size());
            }
            
            // Send message
            player.sendTip(TextFormat.GOLD + "Vein Mining: " + TextFormat.WHITE + vein.size() + " blocks");
            
            int itemsNotPickedUp = 0;
            boolean toolBroken = false;
            
            // Break all blocks in the vein (including the original)
            for (Block veinBlock : vein) {
                
                // Get drops
                Item[] drops = veinBlock.getDrops(tool);
                
                // Handle item pickup
                for (Item drop : drops) {
                    if (autoPickupEnabled) {
                        // Try to add to inventory
                        if (!player.getInventory().canAddItem(drop)) {
                            // Inventory is full
                            if (fullInventoryAction.equals("drop")) {
                                veinBlock.getLevel().dropItem(veinBlock, drop);
                            }
                            // If "delete", just don't add it anywhere
                            itemsNotPickedUp++;
                        } else {
                            player.getInventory().addItem(drop);
                        }
                    } else {
                        // Drop to ground if auto-pickup is disabled
                        veinBlock.getLevel().dropItem(veinBlock, drop);
                    }
                }
                
                // Apply tool durability
                if (tool.getMaxDurability() > 0) {
                    tool.setDamage(tool.getDamage() + 1);
                    if (tool.getDamage() >= tool.getMaxDurability()) {
                        player.getInventory().setItemInHand(Item.get("minecraft:air")); // Air
                        player.getLevel().addSound(player, cn.nukkit.level.Sound.RANDOM_BREAK);
                        toolBroken = true;
                        break;
                    }
                }
                
                // Break the block
                veinBlock.getLevel().setBlock(veinBlock, Block.get("minecraft:air"), true, true);
            }
            
            // Send inventory full message if needed
            if (itemsNotPickedUp > 0) {
                if (!inventoryFullMessage.isEmpty()) {
                    String action = fullInventoryAction.equals("drop") ? "dropped" : "deleted";
                    String message = TextFormat.colorize('&', inventoryFullMessage
                        .replace("{count}", String.valueOf(itemsNotPickedUp))
                        .replace("{action}", action));
                    player.sendMessage(message);
                }
                
                if (loggingEnabled && logVeinMining) {
                    String action = fullInventoryAction.equals("drop") ? "dropped" : "deleted";
                    this.getLogger().info(TextFormat.YELLOW + "[VeinMine] Player " + player.getName() + 
                        " had full inventory: " + itemsNotPickedUp + " items " + action);
                }
            }
            
            // Log tool break
            if (toolBroken && loggingEnabled && logVeinMining) {
                this.getLogger().info(TextFormat.RED + "[VeinMine] Player " + player.getName() + 
                    "'s tool broke during vein mining");
            }
            
            // Update tool in inventory if not broken
            if (!toolBroken && tool.getMaxDurability() > 0) {
                player.getInventory().setItemInHand(tool);
            }
        }
    }

    /**
     * Find all connected blocks of the same type using BFS (Breadth-First Search)
     */
    private Set<Block> findVein(Block startBlock, Set<Block> visited) {
        Set<Block> vein = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(startBlock);
        
        String blockId = startBlock.getId();
        
        while (!queue.isEmpty() && vein.size() < MAX_BLOCKS) {
            Block current = queue.poll();
            
            if (visited.contains(current) || vein.contains(current)) {
                continue;
            }
            
            // Check if this block is the same type
            if (!current.getId().equals(blockId)) {
                continue;
            }
            
            vein.add(current);
            visited.add(current);
            
            // Check all 26 surrounding blocks (including diagonals)
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) {
                            continue;
                        }
                        
                        Block neighbor = current.getLevel().getBlock(
                                current.getFloorX() + x,
                                current.getFloorY() + y,
                                current.getFloorZ() + z
                        );
                        
                        if (!visited.contains(neighbor) && neighbor.getId().equals(blockId)) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
        
        return vein;
    }

    /**
     * Check if the tool is appropriate for mining this block
     */
    private boolean isProperTool(String blockId, Item tool) {
        String toolId = tool.getId();
        
        // Ores require pickaxes
        if (isOre(blockId)) {
            return isPickaxe(toolId);
        }
        
        // Logs require axes
        if (isLog(blockId)) {
            return isAxe(toolId);
        }
        
        // Leaves can be broken with any tool or hand
        if (isLeaves(blockId)) {
            return true;
        }
        
        return false;
    }

    private boolean isOre(String blockId) {
        return blockId.contains("_ore");
    }

    private boolean isLog(String blockId) {
        return blockId.contains("_log") || blockId.contains("_stem");
    }

    private boolean isLeaves(String blockId) {
        return blockId.contains("_leaves");
    }

    private boolean isPickaxe(String toolId) {
        return toolId.contains("_pickaxe");
    }

    private boolean isAxe(String toolId) {
        return toolId.contains("_axe");
    }
}
