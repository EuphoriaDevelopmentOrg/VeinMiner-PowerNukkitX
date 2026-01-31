package com.veinminer;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Sound;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VeinMinerPlugin extends PluginBase implements Listener {

    // Constants for magic numbers
    private static final int DEFAULT_MAX_BLOCKS = 64;
    private static final int NEIGHBOR_RANGE = 1; // -1 to 1 for 3x3x3 cube
    private static final int UPDATE_CHECK_TIMEOUT = 5000;
    private static final double DEFAULT_DURABILITY_MULTIPLIER = 1.0;
    
    private int maxBlocks;
    private static final Set<String> VEIN_BLOCKS = new HashSet<>(); // Static cached blocks
    private static final Map<String, Boolean> TOOL_VALIDATION_CACHE = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000; // Prevent unbounded cache growth
    
    private boolean autoPickupEnabled;
    private String fullInventoryAction;
    private String inventoryFullMessage;
    private boolean loggingEnabled;
    private boolean logVeinMining;
    private boolean logConfigLoading;
    private double durabilityMultiplier;
    
    // New features
    private List<String> disabledWorlds;
    private boolean particlesEnabled;
    private boolean soundsEnabled;
    private boolean updateCheckerEnabled;
    private String githubRepo;
    private StatisticsTracker statsTracker;
    private VeinMinerCommand veinMinerCommand;
    
    // Config-based block filtering
    private Map<String, Boolean> configuredBlocks;
    private boolean oresEnabled;
    private boolean logsEnabled;
    private boolean leavesEnabled;

    @Override
    public void onEnable() {
        // Save default config
        this.saveDefaultConfig();
        
        // Load configuration
        loadConfig();
        
        // Load vein blocks once (static cache)
        if (VEIN_BLOCKS.isEmpty()) {
            loadVeinBlocks();
        }
        
        // Initialize statistics tracker
        statsTracker = new StatisticsTracker(this);
        
        // Register command
        veinMinerCommand = new VeinMinerCommand(this);
        this.getServer().getCommandMap().register("veinminer", veinMinerCommand);
        
        // Register events
        this.getServer().getPluginManager().registerEvents(this, this);
        
        // Schedule periodic cache cleanup (every 30 minutes)
        this.getServer().getScheduler().scheduleDelayedRepeatingTask(this, () -> {
            if (TOOL_VALIDATION_CACHE.size() > MAX_CACHE_SIZE) {
                TOOL_VALIDATION_CACHE.clear();
                if (loggingEnabled && logConfigLoading) {
                    this.getLogger().info("[Cache] Tool validation cache cleared (size limit reached)");
                }
            }
        }, 36000, 36000); // 30 minutes in ticks (30*60*20)
        
        // Startup message
        this.getLogger().info("Plugin enabled (v" + getDescription().getVersion() + ") - Max blocks: " + maxBlocks + ", Vein blocks: " + VEIN_BLOCKS.size());
        
        // Check for updates
        if (updateCheckerEnabled) {
            checkForUpdates();
        }
    }
    
    @Override
    public void onDisable() {
        // Save statistics synchronously (async not allowed during shutdown)
        if (statsTracker != null) {
            statsTracker.saveStats(false);
        }
        this.getLogger().info(TextFormat.RED + "VeinMiner plugin disabled!");
    }
    
    /**
     * Load and validate configuration settings
     */
    private void loadConfig() {
        Config config = this.getConfig();
        
        // Validate and load max blocks
        maxBlocks = config.getInt("max-blocks", DEFAULT_MAX_BLOCKS);
        if (maxBlocks < 1) {
            this.getLogger().warning("Invalid max-blocks value (" + maxBlocks + "), using default: " + DEFAULT_MAX_BLOCKS);
            maxBlocks = DEFAULT_MAX_BLOCKS;
        }
        
        // Load auto-pickup settings
        autoPickupEnabled = config.getBoolean("auto-pickup.enabled", true);
        fullInventoryAction = config.getString("auto-pickup.full-inventory-action", "drop").toLowerCase();
        
        // Load durability multiplier
        durabilityMultiplier = config.getDouble("tool-durability.multiplier", DEFAULT_DURABILITY_MULTIPLIER);
        if (durabilityMultiplier < 0) {
            this.getLogger().warning("Invalid durability multiplier, using default: " + DEFAULT_DURABILITY_MULTIPLIER);
            durabilityMultiplier = DEFAULT_DURABILITY_MULTIPLIER;
        }
        
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
        
        // Load block category settings
        oresEnabled = config.getBoolean("enabled-blocks.ores", true);
        logsEnabled = config.getBoolean("enabled-blocks.logs", true);
        leavesEnabled = config.getBoolean("enabled-blocks.leaves", true);
        
        // Load specific block configurations
        configuredBlocks = new HashMap<>();
        if (config.exists("blocks")) {
            Map<String, Object> blocksSection = config.getSection("blocks").getAllMap();
            for (Map.Entry<String, Object> entry : blocksSection.entrySet()) {
                if (entry.getValue() instanceof Boolean) {
                    configuredBlocks.put(entry.getKey().toUpperCase(), (Boolean) entry.getValue());
                }
            }
        }
        
        if (loggingEnabled && logConfigLoading) {
            this.getLogger().info(TextFormat.GREEN + "[Config] Auto-pickup: " + (autoPickupEnabled ? "enabled" : "disabled"));
            this.getLogger().info(TextFormat.GREEN + "[Config] Full inventory action: " + fullInventoryAction);
            this.getLogger().info(TextFormat.GREEN + "[Config] Console logging: enabled");
            this.getLogger().info(TextFormat.GREEN + "[Config] Disabled worlds: " + disabledWorlds.size());
            this.getLogger().info(TextFormat.GREEN + "[Config] Effects: " + (particlesEnabled || soundsEnabled ? "enabled" : "disabled"));
            this.getLogger().info(TextFormat.GREEN + "[Config] Durability multiplier: " + durabilityMultiplier + "x");
        }
    }
    
    public void reloadConfiguration() {
        loadConfig();
        VEIN_BLOCKS.clear();
        TOOL_VALIDATION_CACHE.clear();
        loadVeinBlocks();
    }
    
    public StatisticsTracker getStatsTracker() {
        return statsTracker;
    }
    
    /**
     * Load all vein-mineable block types into static cache
     */
    private void loadVeinBlocks() {
        // Only load blocks that are enabled in config
        
        // Ores
        if (oresEnabled && isBlockEnabled("COAL_ORE")) {
            VEIN_BLOCKS.add("minecraft:coal_ore");
        }
        if (oresEnabled && isBlockEnabled("IRON_ORE")) {
            VEIN_BLOCKS.add("minecraft:iron_ore");
        }
        if (oresEnabled && isBlockEnabled("GOLD_ORE")) {
            VEIN_BLOCKS.add("minecraft:gold_ore");
        }
        if (oresEnabled && isBlockEnabled("DIAMOND_ORE")) {
            VEIN_BLOCKS.add("minecraft:diamond_ore");
        }
        if (oresEnabled && isBlockEnabled("EMERALD_ORE")) {
            VEIN_BLOCKS.add("minecraft:emerald_ore");
        }
        if (oresEnabled && isBlockEnabled("LAPIS_ORE")) {
            VEIN_BLOCKS.add("minecraft:lapis_ore");
        }
        if (oresEnabled && isBlockEnabled("REDSTONE_ORE")) {
            VEIN_BLOCKS.add("minecraft:redstone_ore");
            VEIN_BLOCKS.add("minecraft:lit_redstone_ore"); // Both states
        }
        if (oresEnabled && isBlockEnabled("COPPER_ORE")) {
            VEIN_BLOCKS.add("minecraft:copper_ore");
        }
        if (oresEnabled && isBlockEnabled("DEEPSLATE_COAL_ORE")) {
            VEIN_BLOCKS.add("minecraft:deepslate_coal_ore");
        }
        if (oresEnabled && isBlockEnabled("DEEPSLATE_IRON_ORE")) {
            VEIN_BLOCKS.add("minecraft:deepslate_iron_ore");
        }
        if (oresEnabled && isBlockEnabled("DEEPSLATE_GOLD_ORE")) {
            VEIN_BLOCKS.add("minecraft:deepslate_gold_ore");
        }
        if (oresEnabled && isBlockEnabled("DEEPSLATE_DIAMOND_ORE")) {
            VEIN_BLOCKS.add("minecraft:deepslate_diamond_ore");
        }
        if (oresEnabled && isBlockEnabled("DEEPSLATE_EMERALD_ORE")) {
            VEIN_BLOCKS.add("minecraft:deepslate_emerald_ore");
        }
        if (oresEnabled && isBlockEnabled("DEEPSLATE_LAPIS_ORE")) {
            VEIN_BLOCKS.add("minecraft:deepslate_lapis_ore");
        }
        if (oresEnabled && isBlockEnabled("DEEPSLATE_REDSTONE_ORE")) {
            VEIN_BLOCKS.add("minecraft:deepslate_redstone_ore");
            VEIN_BLOCKS.add("minecraft:lit_deepslate_redstone_ore"); // Both states
        }
        if (oresEnabled && isBlockEnabled("DEEPSLATE_COPPER_ORE")) {
            VEIN_BLOCKS.add("minecraft:deepslate_copper_ore");
        }
        if (oresEnabled && isBlockEnabled("QUARTZ_ORE")) {
            VEIN_BLOCKS.add("minecraft:quartz_ore");
        }
        if (oresEnabled && isBlockEnabled("NETHER_GOLD_ORE")) {
            VEIN_BLOCKS.add("minecraft:nether_gold_ore");
        }
        // Ancient debris - special case (doesn't contain "_ore")
        if (oresEnabled) {
            VEIN_BLOCKS.add("minecraft:ancient_debris");
        }
        // Amethyst clusters
        if (oresEnabled && isBlockEnabled("AMETHYST_CLUSTER")) {
            VEIN_BLOCKS.add("minecraft:amethyst_cluster");
            VEIN_BLOCKS.add("minecraft:large_amethyst_bud");
            VEIN_BLOCKS.add("minecraft:medium_amethyst_bud");
            VEIN_BLOCKS.add("minecraft:small_amethyst_bud");
        }
        
        // Logs (natural tree logs only)
        if (logsEnabled && isBlockEnabled("LOG")) {
            VEIN_BLOCKS.add("minecraft:oak_log");
            VEIN_BLOCKS.add("minecraft:spruce_log");
            VEIN_BLOCKS.add("minecraft:birch_log");
            VEIN_BLOCKS.add("minecraft:jungle_log");
        }
        if (logsEnabled && isBlockEnabled("LOG2")) {
            VEIN_BLOCKS.add("minecraft:acacia_log");
            VEIN_BLOCKS.add("minecraft:dark_oak_log");
        }
        if (logsEnabled && isBlockEnabled("MANGROVE_LOG")) {
            VEIN_BLOCKS.add("minecraft:mangrove_log");
        }
        if (logsEnabled && isBlockEnabled("CHERRY_LOG")) {
            VEIN_BLOCKS.add("minecraft:cherry_log");
        }
        if (logsEnabled && isBlockEnabled("CRIMSON_STEM")) {
            VEIN_BLOCKS.add("minecraft:crimson_stem");
        }
        if (logsEnabled && isBlockEnabled("WARPED_STEM")) {
            VEIN_BLOCKS.add("minecraft:warped_stem");
        }
        
        // Leaves
        if (leavesEnabled && isBlockEnabled("LEAVES")) {
            VEIN_BLOCKS.add("minecraft:oak_leaves");
            VEIN_BLOCKS.add("minecraft:spruce_leaves");
            VEIN_BLOCKS.add("minecraft:birch_leaves");
            VEIN_BLOCKS.add("minecraft:jungle_leaves");
        }
        if (leavesEnabled && isBlockEnabled("LEAVES2")) {
            VEIN_BLOCKS.add("minecraft:acacia_leaves");
            VEIN_BLOCKS.add("minecraft:dark_oak_leaves");
        }
        if (leavesEnabled && isBlockEnabled("MANGROVE_LEAVES")) {
            VEIN_BLOCKS.add("minecraft:mangrove_leaves");
        }
        if (leavesEnabled && isBlockEnabled("CHERRY_LEAVES")) {
            VEIN_BLOCKS.add("minecraft:cherry_leaves");
        }

        if (loggingEnabled && logConfigLoading) {
            this.getLogger().info(TextFormat.GREEN + "[Config] Loaded " + VEIN_BLOCKS.size() + " vein-mineable block types");
        }
    }
    
    /**
     * Check if a block is enabled in config
     * @param blockName The block name from config
     * @return true if enabled or not specified, false if explicitly disabled
     */
    private boolean isBlockEnabled(String blockName) {
        return configuredBlocks.getOrDefault(blockName.toUpperCase(), true);
    }

    /**
     * Handle block break events for vein mining
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Null safety checks
        if (player == null || block == null) {
            return;
        }
        
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
        if (!VEIN_BLOCKS.contains(blockId)) {
            return;
        }
        
        // Check if player has the proper tool
        Item tool = player.getInventory().getItemInHand();
        if (!isProperTool(blockId, tool)) {
            return;
        }
        
        // SECURITY: Verify tool has enough durability BEFORE processing
        int toolDurability = tool.getMaxDurability();
        if (toolDurability > 0) {
            int remainingDurability = toolDurability - tool.getDamage();
            if (remainingDurability <= 0) {
                return; // Tool is already broken
            }
        }
        
        try {
            // Find all connected blocks of the same type
            Set<Block> vein = findVein(block);
            
            // SECURITY: Strictly enforce maxBlocks limit
            if (vein != null && vein.size() > 1 && vein.size() <= maxBlocks) {
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
                
                // Process the vein mining
                processVeinMining(player, vein, tool);
            }
        } catch (Exception e) {
            this.getLogger().error("Error during vein mining: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(TextFormat.RED + "An error occurred during vein mining.");
        }
    }
    
    /**
     * Process the mining of all blocks in a vein
     * @param player The player mining the vein
     * @param vein Set of blocks in the vein
     * @param tool The tool being used
     */
    private void processVeinMining(Player player, Set<Block> vein, Item tool) {
        int itemsNotPickedUp = 0;
        int totalXP = 0;
        boolean toolBroken = false;
        int blocksMined = 0;
        
        // Calculate max blocks we can mine with remaining durability
        int maxMinableBlocks = vein.size();
        if (tool.getMaxDurability() > 0) {
            int remainingDurability = tool.getMaxDurability() - tool.getDamage();
            int durabilityPerBlock = Math.max(1, (int) Math.round(durabilityMultiplier));
            maxMinableBlocks = Math.min(vein.size(), remainingDurability / durabilityPerBlock);
        }
        
        // Break all blocks in the vein (including the original)
        for (Block veinBlock : vein) {
            // SECURITY: Stop if we've reached durability limit
            if (blocksMined >= maxMinableBlocks) {
                break;
            }
            
            // Check if tool is broken before processing each block
            if (tool.getMaxDurability() > 0 && tool.getDamage() >= tool.getMaxDurability()) {
                toolBroken = true;
                break;
            }
            
            // Process block drops and effects
            Map<String, Object> result = processBlockBreak(player, veinBlock, tool);
            boolean blockActuallyBroken = (boolean) result.get("success");
            
            // SECURITY: Only count XP and items if block was actually broken
            if (blockActuallyBroken) {
                itemsNotPickedUp += (int) result.get("itemsNotPickedUp");
                totalXP += (int) result.get("xp");
                blocksMined++;
            }
            
            // Apply tool durability with multiplier
            if (tool.getMaxDurability() > 0) {
                int durabilityDamage = (int) Math.max(1, Math.round(durabilityMultiplier));
                tool.setDamage(tool.getDamage() + durabilityDamage);
                
                // Check if tool broke
                if (tool.getDamage() >= tool.getMaxDurability()) {
                    player.getInventory().setItemInHand(Item.get("minecraft:air"));
                    // Send slot update to prevent network issues
                    player.getInventory().sendSlot(player.getInventory().getHeldItemIndex(), player);
                    player.getLevel().addSound(player, Sound.RANDOM_BREAK);
                    toolBroken = true;
                    break;
                }
            }
        }
        
        // Spawn experience orbs
        if (totalXP > 0 && blocksMined > 0) {
            player.getLevel().dropExpOrb(player.getLocation(), totalXP);
        }
        
        // Play sound effect
        if (soundsEnabled && !toolBroken && blocksMined > 0) {
            player.getLevel().addSound(player, Sound.RANDOM_LEVELUP, 1.0f, 1.5f);
        }
        
        // Send inventory full message if needed
        if (itemsNotPickedUp > 0) {
            sendInventoryFullMessage(player, itemsNotPickedUp);
        }
        
        // Log tool break
        if (toolBroken && loggingEnabled && logVeinMining) {
            this.getLogger().info(TextFormat.RED + "[VeinMine] Player " + player.getName() + 
                "'s tool broke during vein mining (mined " + blocksMined + " blocks)");
        }
        
        // Update tool in inventory if not broken
        if (!toolBroken && tool.getMaxDurability() > 0) {
            player.getInventory().setItemInHand(tool);
            // Send slot update to prevent network ID mismatch
            player.getInventory().sendSlot(player.getInventory().getHeldItemIndex(), player);
        }
    }
    
    /**
     * Process a single block break within vein mining
     * @param player The player mining
     * @param veinBlock The block to break
     * @param tool The tool being used
     * @return Map containing itemsNotPickedUp and xp
     */
    private Map<String, Object> processBlockBreak(Player player, Block veinBlock, Item tool) {
        int itemsNotPickedUp = 0;
        int xp = 0;
        boolean success = false;
        
        try {
            // SECURITY: Verify block still exists and hasn't been modified
            if (veinBlock == null || veinBlock.getLevel() == null) {
                Map<String, Object> result = new HashMap<>(3);
                result.put("itemsNotPickedUp", 0);
                result.put("xp", 0);
                result.put("success", false);
                return result;
            }
            
            // Get drops (this respects Fortune/Silk Touch enchantments)
            Item[] drops = veinBlock.getDrops(tool);
            
            // Show particle effect
            if (particlesEnabled) {
                veinBlock.getLevel().addParticle(new cn.nukkit.level.particle.DestroyBlockParticle(
                    veinBlock.add(0.5, 0.5, 0.5), veinBlock));
            }
            
            // Handle item drops
            itemsNotPickedUp = handleItemDrops(player, veinBlock, drops);
            
            // Break the block
            boolean blockBroken = veinBlock.getLevel().setBlock(veinBlock, Block.get("minecraft:air"), true, true);
            
            // SECURITY: Only grant XP if block was actually broken
            if (blockBroken) {
                xp = veinBlock.getDropExp();
                success = true;
            }
            
        } catch (Exception e) {
            this.getLogger().warning("Error processing block break at " + veinBlock.getLocation() + ": " + e.getMessage());
        }
        
        // Use initial capacity for better performance
        Map<String, Object> result = new HashMap<>(3);
        result.put("itemsNotPickedUp", itemsNotPickedUp);
        result.put("xp", xp);
        result.put("success", success);
        return result;
    }
    
    /**
     * Handle item drops from broken block
     * @param player The player
     * @param block The block location
     * @param drops Array of dropped items
     * @return Number of items not picked up
     */
    private int handleItemDrops(Player player, Block block, Item[] drops) {
        int itemsNotPickedUp = 0;
        boolean inventoryChanged = false;
        
        // Check if drops array is null or empty
        if (drops == null || drops.length == 0) {
            return 0;
        }
        
        for (Item drop : drops) {
            // Skip null items
            if (drop == null || drop.isNull()) {
                continue;
            }
            
            if (autoPickupEnabled) {
                // Try to add to inventory
                if (!player.getInventory().canAddItem(drop)) {
                    // Inventory is full
                    if (fullInventoryAction.equals("drop")) {
                        block.getLevel().dropItem(block, drop);
                    }
                    // If "delete", just don't add it anywhere
                    itemsNotPickedUp++;
                } else {
                    // Add item and track if inventory changed
                    Item[] notAdded = player.getInventory().addItem(drop);
                    if (notAdded.length > 0) {
                        // Some items couldn't be added, drop them
                        for (Item leftover : notAdded) {
                            if (fullInventoryAction.equals("drop")) {
                                block.getLevel().dropItem(block, leftover);
                            }
                            itemsNotPickedUp++;
                        }
                    } else {
                        inventoryChanged = true;
                    }
                }
            } else {
                // Drop to ground if auto-pickup is disabled
                block.getLevel().dropItem(block, drop);
            }
        }
        
        // Send inventory update once at the end, not for every item
        if (inventoryChanged) {
            player.getInventory().sendContents(player);
        }
        
        return itemsNotPickedUp;
    }
    
    /**
     * Send inventory full message to player
     * @param player The player
     * @param count Number of items not picked up
     */
    private void sendInventoryFullMessage(Player player, int count) {
        if (!inventoryFullMessage.isEmpty()) {
            String action = fullInventoryAction.equals("drop") ? "dropped" : "deleted";
            String message = TextFormat.colorize('&', inventoryFullMessage
                .replace("{count}", String.valueOf(count))
                .replace("{action}", action));
            player.sendMessage(message);
        }
        
        if (loggingEnabled && logVeinMining) {
            String action = fullInventoryAction.equals("drop") ? "dropped" : "deleted";
            this.getLogger().info(TextFormat.YELLOW + "[VeinMine] Player " + player.getName() + 
                " had full inventory: " + count + " items " + action);
        }
    }

    /**
     * Find all connected blocks of the same type using BFS (Breadth-First Search)
     * Optimized to cache block positions as strings instead of Block objects
     * @param startBlock The initial block to start from
     * @return Set of blocks in the vein
     */
    private Set<Block> findVein(Block startBlock) {
        if (startBlock == null || startBlock.getLevel() == null) {
            return Collections.emptySet();
        }
        
        Set<Block> vein = new HashSet<>();
        Set<String> visitedPositions = new HashSet<>(); // Cache positions as "x,y,z"
        Queue<Block> queue = new ArrayDeque<>(); // Faster than LinkedList
        queue.add(startBlock);
        
        String blockId = startBlock.getId();
        cn.nukkit.level.Level level = startBlock.getLevel();
        
        // PERFORMANCE: Pre-allocate StringBuilder for position keys
        StringBuilder posKeyBuilder = new StringBuilder(16);
        
        while (!queue.isEmpty() && vein.size() < maxBlocks) {
            Block current = queue.poll();
            
            if (current == null) {
                continue;
            }
            
            int cx = current.getFloorX();
            int cy = current.getFloorY();
            int cz = current.getFloorZ();
            
            // PERFORMANCE: Reuse StringBuilder for position keys
            posKeyBuilder.setLength(0);
            String posKey = posKeyBuilder.append(cx).append(',').append(cy).append(',').append(cz).toString();
            
            if (visitedPositions.contains(posKey)) {
                continue;
            }
            
            // Check if this block is the same type
            if (!current.getId().equals(blockId)) {
                continue;
            }
            
            vein.add(current);
            visitedPositions.add(posKey);
            
            // SECURITY: Hard limit check to prevent infinite loops
            if (vein.size() >= maxBlocks) {
                break;
            }
            
            // Check all 26 surrounding blocks (including diagonals) in a 3x3x3 cube
            for (int dx = -NEIGHBOR_RANGE; dx <= NEIGHBOR_RANGE; dx++) {
                for (int dy = -NEIGHBOR_RANGE; dy <= NEIGHBOR_RANGE; dy++) {
                    for (int dz = -NEIGHBOR_RANGE; dz <= NEIGHBOR_RANGE; dz++) {
                        // Skip center block
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        
                        int nx = cx + dx;
                        int ny = cy + dy;
                        int nz = cz + dz;
                        
                        // PERFORMANCE: Reuse StringBuilder
                        posKeyBuilder.setLength(0);
                        String neighborPosKey = posKeyBuilder.append(nx).append(',').append(ny).append(',').append(nz).toString();
                        
                        if (visitedPositions.contains(neighborPosKey)) {
                            continue;
                        }
                        
                        // SECURITY: Prevent queue from growing too large
                        if (queue.size() > maxBlocks * 2) {
                            continue;
                        }
                        
                        try {
                            // PERFORMANCE: Direct block ID check without creating full Block object when possible
                            Block neighbor = level.getBlock(nx, ny, nz);
                            if (neighbor != null && blockId.equals(neighbor.getId())) {
                                queue.add(neighbor);
                            }
                        } catch (Exception e) {
                            // Skip invalid blocks
                        }
                    }
                }
            }
        }
        
        return vein;
    }

    /**
     * Check if the tool is appropriate for mining this block
     * Uses caching to improve performance
     * @param blockId The block identifier
     * @param tool The tool item
     * @return true if tool is valid for this block
     */
    private boolean isProperTool(String blockId, Item tool) {
        if (tool == null || blockId == null) {
            return false;
        }
        
        String toolId = tool.getId();
        // Use StringBuilder for better performance with concatenation
        String cacheKey = new StringBuilder(blockId.length() + toolId.length() + 1)
            .append(blockId).append(':').append(toolId).toString();
        
        // Check cache first
        Boolean cached = TOOL_VALIDATION_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // Calculate result
        boolean result = false;
        
        // Ores and Ancient Debris require pickaxes
        if (isOre(blockId) || blockId.equals("minecraft:ancient_debris")) {
            result = isPickaxe(toolId);
        }
        // Logs require axes
        else if (isLog(blockId)) {
            result = isAxe(toolId);
        }
        // Leaves can be broken with any tool or hand
        else if (isLeaves(blockId)) {
            result = true;
        }
        
        // Cache the result
        TOOL_VALIDATION_CACHE.put(cacheKey, result);
        return result;
    }

    /**
     * Check if block is an ore
     * @param blockId Block identifier
     * @return true if block is an ore
     */
    private boolean isOre(String blockId) {
        return blockId.contains("_ore");
    }

    /**
     * Check if block is a log or stem
     * @param blockId Block identifier
     * @return true if block is a log
     */
    private boolean isLog(String blockId) {
        return blockId.contains("_log") || blockId.contains("_stem");
    }

    /**
     * Check if block is leaves
     * @param blockId Block identifier
     * @return true if block is leaves
     */
    private boolean isLeaves(String blockId) {
        return blockId.contains("_leaves");
    }

    /**
     * Check if tool is a pickaxe
     * @param toolId Tool identifier
     * @return true if tool is a pickaxe
     */
    private boolean isPickaxe(String toolId) {
        return toolId.contains("_pickaxe");
    }

    /**
     * Check if tool is an axe
     * @param toolId Tool identifier
     * @return true if tool is an axe
     */
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
    
    /**
     * Check for plugin updates from GitHub
     */
    private void checkForUpdates() {
        this.getServer().getScheduler().scheduleAsyncTask(this, new cn.nukkit.scheduler.AsyncTask() {
            @Override
            public void onRun() {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try {
                    String currentVersion = getDescription().getVersion();
                    String url = "https://api.github.com/repos/" + githubRepo + "/releases/latest";
                    
                    @SuppressWarnings("deprecation")
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    connection = conn;
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("User-Agent", "VeinMiner-UpdateChecker");
                    connection.setConnectTimeout(UPDATE_CHECK_TIMEOUT);
                    connection.setReadTimeout(UPDATE_CHECK_TIMEOUT);
                    
                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        
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
                } finally {
                    // Properly close resources
                    try {
                        if (reader != null) {
                            reader.close();
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                    try {
                        if (connection != null) {
                            connection.disconnect();
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        });
    }
}
