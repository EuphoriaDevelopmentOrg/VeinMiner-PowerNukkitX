package com.veinminer;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Sound;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
    
    // New features
    private List<String> disabledWorlds;
    private boolean particlesEnabled;
    private boolean soundsEnabled;
    private boolean updateCheckerEnabled;
    private String githubRepo;
    private StatisticsTracker statsTracker;
    private VeinMinerCommand veinMinerCommand;

    @Override
    public void onEnable() {
        // Save default config
        this.saveDefaultConfig();
        
        // Load configuration
        loadConfig();
        
        // Load vein blocks
        loadVeinBlocks();
        
        // Initialize statistics tracker
        statsTracker = new StatisticsTracker(this);
        
        // Register command
        veinMinerCommand = new VeinMinerCommand(this);
        this.getServer().getCommandMap().register("veinminer", veinMinerCommand);
        
        // Register events
        this.getServer().getPluginManager().registerEvents(this, this);
        
        // Fancy startup message
        this.getLogger().info(TextFormat.AQUA + "═══════════════════════════════════════");
        this.getLogger().info(TextFormat.GOLD + "  ⚯ " + TextFormat.BOLD + " VeinMiner" + TextFormat.RESET + TextFormat.GOLD + " v1.0.4 ⚯");
        this.getLogger().info(TextFormat.GREEN + "  ✓ Plugin Enabled Successfully!");
        this.getLogger().info(TextFormat.YELLOW + "  » Max blocks per vein: " + TextFormat.WHITE + MAX_BLOCKS);
        this.getLogger().info(TextFormat.YELLOW + "  » Auto-pickup: " + TextFormat.WHITE + (autoPickupEnabled ? "Enabled" : "Disabled"));
        this.getLogger().info(TextFormat.YELLOW + "  » Sneak required: " + TextFormat.WHITE + "Yes");
        this.getLogger().info(TextFormat.YELLOW + "  » Vein blocks loaded: " + TextFormat.WHITE + veinBlocks.size());
        this.getLogger().info(TextFormat.AQUA + "═══════════════════════════════════════");
        
        // Check for updates
        if (updateCheckerEnabled) {
            checkForUpdates();
        }
    }
    
    @Override
    public void onDisable() {
        // Save statistics
        if (statsTracker != null) {
            statsTracker.saveStats();
        }
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
        
        // Load world restrictions
        disabledWorlds = config.getStringList("disabled-worlds");
        
        // Load effects
        particlesEnabled = config.getBoolean("effects.particles", true);
        soundsEnabled = config.getBoolean("effects.sounds", true);
        
        // Load update checker
        updateCheckerEnabled = config.getBoolean("update-checker.enabled", true);
        githubRepo = config.getString("update-checker.repository", "EuphoriaDevelopmentOrg/VeinMiner-PowerNukkitX");
        
        // Load messages
        inventoryFullMessage = config.getString("messages.inventory-full", "&eInventory full! {count} items were {action}.");
        
        if (loggingEnabled && logConfigLoading) {
            this.getLogger().info(TextFormat.GREEN + "[Config] Auto-pickup: " + (autoPickupEnabled ? "enabled" : "disabled"));
            this.getLogger().info(TextFormat.GREEN + "[Config] Full inventory action: " + fullInventoryAction);
            this.getLogger().info(TextFormat.GREEN + "[Config] Console logging: enabled");
            this.getLogger().info(TextFormat.GREEN + "[Config] Disabled worlds: " + disabledWorlds.size());
            this.getLogger().info(TextFormat.GREEN + "[Config] Effects: " + (particlesEnabled || soundsEnabled ? "enabled" : "disabled"));
        }
    }
    
    public void reloadConfiguration() {
        loadConfig();
        veinBlocks.clear();
        loadVeinBlocks();
    }
    
    public StatisticsTracker getStatsTracker() {
        return statsTracker;
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
        
        // Check permission
        if (!player.hasPermission("veinminer.use")) {
            return;
        }
        
        // Check if player has toggled vein mining off
        if (veinMinerCommand != null && veinMinerCommand.isDisabled(player)) {
            return;
        }
        
        // Check if world is disabled
        if (disabledWorlds.contains(player.getLevel().getName())) {
            return;
        }
        
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
            
            // Record statistics
            statsTracker.recordVeinMine(player, vein.size());
            
            // Send message
            player.sendTip(TextFormat.GOLD + "Vein Mining: " + TextFormat.WHITE + vein.size() + " blocks");
            
            int itemsNotPickedUp = 0;
            int totalXP = 0;
            boolean toolBroken = false;
            
            // Break all blocks in the vein (including the original)
            for (Block veinBlock : vein) {
                
                // Get drops (this respects Fortune/Silk Touch enchantments)
                Item[] drops = veinBlock.getDrops(tool);
                
                // Get experience from block
                int xp = veinBlock.getDropExp();
                if (xp > 0) {
                    totalXP += xp;
                }
                
                // Show particle effect
                if (particlesEnabled) {
                    veinBlock.getLevel().addParticle(new cn.nukkit.level.particle.DestroyBlockParticle(veinBlock.add(0.5, 0.5, 0.5), veinBlock));
                }
                
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
            
            // Spawn experience orbs
            if (totalXP > 0) {
                player.getLevel().dropExpOrb(block, totalXP);
            }
            
            // Play sound effect
            if (soundsEnabled && !toolBroken) {
                player.getLevel().addSound(player, Sound.RANDOM_LEVELUP, 1.0f, 1.5f);
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
    
    private boolean isNewerVersion(String current, String latest) {
        try {
            String[] currentParts = current.split("\\.");
            String[] latestParts = latest.split("\\.");
            
            int maxLength = Math.max(currentParts.length, latestParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                
                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void checkForUpdates() {
        this.getServer().getScheduler().scheduleAsyncTask(this, new cn.nukkit.scheduler.AsyncTask() {
            @Override
            public void onRun() {
                try {
                    String currentVersion = getDescription().getVersion();
                    String url = "https://api.github.com/repos/" + githubRepo + "/releases/latest";
                    
                    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("User-Agent", "VeinMiner-UpdateChecker");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    
                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        
                        // Simple JSON parsing for tag_name
                        String jsonResponse = response.toString();
                        int tagIndex = jsonResponse.indexOf("\"tag_name\"");
                        if (tagIndex != -1) {
                            int startQuote = jsonResponse.indexOf("\"", tagIndex + 11);
                            int endQuote = jsonResponse.indexOf("\"", startQuote + 1);
                            String latestVersion = jsonResponse.substring(startQuote + 1, endQuote).replace("v", "").trim();
                            String cleanCurrentVersion = currentVersion.trim();
                            
                            if (isNewerVersion(cleanCurrentVersion, latestVersion)) {
                                getServer().getScheduler().scheduleTask(VeinMinerPlugin.this, () -> {
                                    getLogger().warning(TextFormat.YELLOW + "===========================================");
                                    getLogger().warning(TextFormat.YELLOW + "A new update for VeinMiner is available!");
                                    getLogger().warning(TextFormat.YELLOW + "Current: " + TextFormat.RED + cleanCurrentVersion + TextFormat.YELLOW + " | Latest: " + TextFormat.GREEN + latestVersion);
                                    getLogger().warning(TextFormat.YELLOW + "Download: " + TextFormat.AQUA + "https://github.com/" + githubRepo + "/releases");
                                    getLogger().warning(TextFormat.YELLOW + "===========================================");
                                    
                                    // Notify online ops/admins
                                    for (Player onlinePlayer : getServer().getOnlinePlayers().values()) {
                                        if (onlinePlayer.isOp()) {
                                            onlinePlayer.sendMessage(TextFormat.YELLOW + "[VeinMiner] " + TextFormat.GOLD + "A new update is available!");
                                            onlinePlayer.sendMessage(TextFormat.YELLOW + "Current: " + TextFormat.RED + cleanCurrentVersion + TextFormat.YELLOW + " | Latest: " + TextFormat.GREEN + latestVersion);
                                            onlinePlayer.sendMessage(TextFormat.GRAY + "Download: " + TextFormat.AQUA + "https://github.com/" + githubRepo + "/releases");
                                        }
                                    }
                                });
                            } else {
                                if (loggingEnabled) {
                                    getServer().getScheduler().scheduleTask(VeinMinerPlugin.this, () -> {
                                        getLogger().info(TextFormat.GREEN + "[Update] You are running the latest version!");
                                    });
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    if (loggingEnabled) {
                        getLogger().warning("Failed to check for updates: " + e.getMessage());
                    }
                }
            }
        });
    }
}
