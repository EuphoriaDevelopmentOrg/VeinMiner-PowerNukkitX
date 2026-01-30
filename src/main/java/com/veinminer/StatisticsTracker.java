package com.veinminer;

import cn.nukkit.Player;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks vein mining statistics for players
 * Thread-safe implementation with atomic operations and async saving
 */
public class StatisticsTracker {
    
    // Logging tag
    private static final String LOG_TAG = "[VeinMiner] ";
    
    private final VeinMinerPlugin plugin;
    private final Map<UUID, PlayerStats> playerStats;
    private final boolean enabled;
    private final boolean saveToFile;
    private volatile boolean saving = false;
    
    // Milestone tracking
    private final Map<UUID, Set<Integer>> achievedMilestones;
    private final boolean milestonesEnabled;
    private final List<Integer> milestoneThresholds;
    
    public StatisticsTracker(VeinMinerPlugin plugin) {
        this.plugin = plugin;
        this.playerStats = new ConcurrentHashMap<>();
        this.achievedMilestones = new ConcurrentHashMap<>();
        
        Config config = plugin.getConfig();
        this.enabled = config.getBoolean("statistics.enabled", true);
        this.saveToFile = config.getBoolean("statistics.save-to-file", true);
        this.milestonesEnabled = config.getBoolean("statistics.milestones.enabled", true);
        this.milestoneThresholds = config.getIntegerList("statistics.milestones.thresholds");
        
        // Default milestones if not configured
        if (milestoneThresholds.isEmpty()) {
            milestoneThresholds.addAll(Arrays.asList(100, 500, 1000, 5000, 10000));
        }
        
        if (enabled && saveToFile) {
            loadStats();
        }
    }
    
    /**
     * Record a vein mining event
     * @param player The player who mined the vein
     * @param blockCount Number of blocks in the vein
     */
    public void recordVeinMine(Player player, int blockCount) {
        if (!enabled) return;
        
        UUID uuid = player.getUniqueId();
        PlayerStats stats = playerStats.computeIfAbsent(uuid, k -> new PlayerStats(player.getName()));
        
        // Ensure milestones are loaded for this player (lazy loading)
        ensureMilestonesLoaded(uuid);
        
        int previousBlocks = stats.getTotalBlocks();
        stats.incrementVeins();
        stats.addBlocks(blockCount);
        stats.updateLastMined();
        stats.updateLargestVein(blockCount);
        
        // Check milestones (only check once per player session to prevent resending)
        if (milestonesEnabled) {
            checkMilestones(player, previousBlocks, stats.getTotalBlocks());
        }
    }
    
    /**
     * Sanitize player name to prevent command injection
     * @param playerName The player name to sanitize
     * @return Sanitized player name
     */
    private String sanitizePlayerName(String playerName) {
        if (playerName == null) {
            return "Unknown";
        }
        // SECURITY: Remove potentially dangerous characters that could be used for command injection
        // Allow only alphanumeric, underscore, and hyphen (standard Minecraft username characters)
        return playerName.replaceAll("[^a-zA-Z0-9_-]", "");
    }
    
    /**
     * Check if player has reached any milestones
     * @param player The player
     * @param previousTotal Previous total blocks
     * @param currentTotal Current total blocks
     */
    private void checkMilestones(Player player, int previousTotal, int currentTotal) {
        UUID uuid = player.getUniqueId();
        Set<Integer> achieved = achievedMilestones.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());
        
        for (Integer threshold : milestoneThresholds) {
            // If milestone crossed and not already achieved
            if (currentTotal >= threshold && previousTotal < threshold) {
                // Use add() with thread-safe set - returns false if already present
                if (achieved.add(threshold)) {
                    grantMilestoneReward(player, threshold);
                }
            }
        }
    }
    
    /**
     * Ensure player milestones are loaded from file (lazy loading)
     * @param uuid Player UUID
     */
    private void ensureMilestonesLoaded(UUID uuid) {
        // If milestones already loaded for this player, skip
        if (achievedMilestones.containsKey(uuid)) {
            return;
        }
        
        // Try to load from file
        if (!saveToFile) {
            return;
        }
        
        try {
            File statsFile = new File(plugin.getDataFolder(), "stats.yml");
            if (!statsFile.exists()) {
                return;
            }
            
            Config statsConfig = new Config(statsFile, Config.YAML);
            String uuidStr = uuid.toString();
            
            if (statsConfig.exists(uuidStr)) {
                Map<String, Object> data = statsConfig.getSection(uuidStr).getAllMap();
                
                if (data.containsKey("milestones")) {
                    List<?> milestonesData = (List<?>) data.get("milestones");
                    Set<Integer> milestones = ConcurrentHashMap.newKeySet();
                    for (Object obj : milestonesData) {
                        if (obj instanceof Number) {
                            milestones.add(((Number) obj).intValue());
                        }
                    }
                    if (!milestones.isEmpty()) {
                        achievedMilestones.put(uuid, milestones);
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail - player will start with no milestones
        }
    }
    
    /**
     * Grant milestone reward to player
     * @param player The player
     * @param milestone The milestone threshold reached
     */
    private void grantMilestoneReward(Player player, int milestone) {
        // SECURITY: Enhanced null and online checks
        if (player == null) {
            return;
        }
        
        // Double-check player is still online before granting rewards
        if (!player.isOnline() || !player.isConnected()) {
            return;
        }
        
        Config config = plugin.getConfig();
        
        // Send congratulatory message
        player.sendMessage(TextFormat.GOLD + "" + TextFormat.BOLD + "═══════════════════════════════");
        player.sendMessage(TextFormat.GREEN + "" + TextFormat.BOLD + "⚡ MILESTONE REACHED! ⚡");
        player.sendMessage(TextFormat.YELLOW + "You've mined " + TextFormat.WHITE + milestone + TextFormat.YELLOW + " blocks with VeinMiner!");
        
        // Grant rewards if configured
        if (config.exists("statistics.milestones.rewards." + milestone)) {
            List<String> commands = config.getStringList("statistics.milestones.rewards." + milestone);
            int successfulCommands = 0;
            for (String command : commands) {
                try {
                    // SECURITY: Sanitize player name to prevent command injection
                    String sanitizedPlayerName = sanitizePlayerName(player.getName());
                    
                    // Replace placeholders with sanitized values
                    String processedCommand = command.replace("{player}", sanitizedPlayerName);
                    
                    if (processedCommand != null && !processedCommand.trim().isEmpty()) {
                        // Verify player is still online before executing
                        if (player.isOnline() && player.isConnected()) {
                            plugin.getServer().executeCommand(plugin.getServer().getConsoleSender(), processedCommand);
                            successfulCommands++;
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning(LOG_TAG + "Failed to execute milestone reward command: " + command + " - " + e.getMessage());
                }
            }
            if (successfulCommands > 0 && player.isOnline()) {
                player.sendMessage(TextFormat.AQUA + "✓ Milestone rewards granted!");
            }
        }
        
        player.sendMessage(TextFormat.GOLD + "" + TextFormat.BOLD + "═══════════════════════════════");
        
        // Log milestone
        if (plugin.getConfig().getBoolean("logging.enabled", true)) {
            plugin.getLogger().info(LOG_TAG + TextFormat.GREEN + "[Milestone] Player " + player.getName() + " reached " + milestone + " blocks mined!");
        }
    }
    
    /**
     * Get statistics for a player
     * @param player The player
     * @return Player statistics
     */
    public PlayerStats getStats(Player player) {
        return playerStats.getOrDefault(player.getUniqueId(), new PlayerStats(player.getName()));
    }
    
    /**
     * Save statistics to file asynchronously
     */
    public void saveStats() {
        saveStats(true);
    }
    
    /**
     * Save statistics to file
     * @param async Whether to save asynchronously or synchronously
     */
    public void saveStats(boolean async) {
        if (!enabled || !saveToFile || saving) return;
        
        saving = true;
        
        // Create a snapshot of current stats
        final Map<UUID, PlayerStats> statsSnapshot = new HashMap<>(playerStats);
        
        if (async) {
            // Save asynchronously (normal operation)
            plugin.getServer().getScheduler().scheduleAsyncTask(plugin, new cn.nukkit.scheduler.AsyncTask() {
                @Override
                public void onRun() {
                    performSave(statsSnapshot);
                }
            });
        } else {
            // Save synchronously (plugin shutdown)
            performSave(statsSnapshot);
        }
    }
    
    /**
     * Perform the actual save operation
     * @param statsSnapshot Snapshot of stats to save
     */
    private void performSave(Map<UUID, PlayerStats> statsSnapshot) {
        try {
            File statsFile = new File(plugin.getDataFolder(), "stats.yml");
            Config statsConfig = new Config(statsFile, Config.YAML);
            
            for (Map.Entry<UUID, PlayerStats> entry : statsSnapshot.entrySet()) {
                String uuidStr = entry.getKey().toString();
                PlayerStats stats = entry.getValue();
                
                Map<String, Object> data = new HashMap<>();
                data.put("name", stats.getPlayerName());
                data.put("totalVeins", stats.getTotalVeins());
                data.put("totalBlocks", stats.getTotalBlocks());
                data.put("largestVein", stats.getLargestVein());
                data.put("lastMined", stats.getLastMined());
                
                // STABILITY: Save achieved milestones with proper synchronization
                Set<Integer> milestones = achievedMilestones.get(entry.getKey());
                if (milestones != null && !milestones.isEmpty()) {
                    // Create defensive copy in synchronized block to prevent concurrent modification
                    List<Integer> milestonesCopy;
                    synchronized (achievedMilestones) {
                        milestonesCopy = new ArrayList<>(milestones);
                    }
                    data.put("milestones", milestonesCopy);
                }
                
                statsConfig.set(uuidStr, data);
            }
            
            statsConfig.save();
        } catch (Exception e) {
            plugin.getLogger().warning(LOG_TAG + "Failed to save statistics: " + e.getMessage());
        } finally {
            saving = false;
        }
    }
    
    /**
     * Load statistics from file
     */
    private void loadStats() {
        File statsFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!statsFile.exists()) return;
        
        try {
            Config statsConfig = new Config(statsFile, Config.YAML);
            
            for (String uuidStr : statsConfig.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    Map<String, Object> data = statsConfig.getSection(uuidStr).getAllMap();
                    
                    PlayerStats stats = new PlayerStats((String) data.get("name"));
                    stats.setTotalVeins(((Number) data.getOrDefault("totalVeins", 0)).intValue());
                    stats.setTotalBlocks(((Number) data.getOrDefault("totalBlocks", 0)).intValue());
                    stats.setLargestVein(((Number) data.getOrDefault("largestVein", 0)).intValue());
                    stats.setLastMined(((Number) data.getOrDefault("lastMined", 0L)).longValue());
                    
                    playerStats.put(uuid, stats);
                    
                    // Load achieved milestones
                    if (data.containsKey("milestones")) {
                        List<?> milestonesData = (List<?>) data.get("milestones");
                        Set<Integer> milestones = ConcurrentHashMap.newKeySet();
                        for (Object obj : milestonesData) {
                            if (obj instanceof Number) {
                                milestones.add(((Number) obj).intValue());
                            }
                        }
                        if (!milestones.isEmpty()) {
                            achievedMilestones.put(uuid, milestones);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning(LOG_TAG + "Failed to load stats for " + uuidStr);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning(LOG_TAG + "Failed to load statistics file: " + e.getMessage());
        }
    }
    
    /**
     * Thread-safe player statistics using atomic operations
     */
    public static class PlayerStats {
        private final String playerName;
        private final AtomicInteger totalVeins;
        private final AtomicInteger totalBlocks;
        private final AtomicInteger largestVein;
        private final AtomicLong lastMined;
        
        public PlayerStats(String playerName) {
            this.playerName = playerName;
            this.totalVeins = new AtomicInteger(0);
            this.totalBlocks = new AtomicInteger(0);
            this.largestVein = new AtomicInteger(0);
            this.lastMined = new AtomicLong(0);
        }
        
        // Atomic operations
        public void incrementVeins() {
            totalVeins.incrementAndGet();
        }
        
        public void addBlocks(int count) {
            totalBlocks.addAndGet(count);
        }
        
        public void updateLastMined() {
            lastMined.set(System.currentTimeMillis());
        }
        
        public void updateLargestVein(int size) {
            largestVein.updateAndGet(current -> Math.max(current, size));
        }
        
        // Getters
        public String getPlayerName() {
            return playerName;
        }
        
        public int getTotalVeins() {
            return totalVeins.get();
        }
        
        public int getTotalBlocks() {
            return totalBlocks.get();
        }
        
        public int getLargestVein() {
            return largestVein.get();
        }
        
        public long getLastMined() {
            return lastMined.get();
        }
        
        // Setters (for loading from file)
        public void setTotalVeins(int value) {
            totalVeins.set(value);
        }
        
        public void setTotalBlocks(int value) {
            totalBlocks.set(value);
        }
        
        public void setLargestVein(int value) {
            largestVein.set(value);
        }
        
        public void setLastMined(long value) {
            lastMined.set(value);
        }
        
        /**
         * Get formatted statistics string
         * @return Formatted statistics
         */
        public String getFormattedStats() {
            StringBuilder sb = new StringBuilder();
            sb.append(TextFormat.GOLD).append(TextFormat.BOLD).append("=== VeinMiner Statistics ===\n");
            sb.append(TextFormat.YELLOW).append("Total Veins Mined: ").append(TextFormat.WHITE).append(getTotalVeins()).append("\n");
            sb.append(TextFormat.YELLOW).append("Total Blocks Mined: ").append(TextFormat.WHITE).append(getTotalBlocks()).append("\n");
            sb.append(TextFormat.YELLOW).append("Largest Vein: ").append(TextFormat.WHITE).append(getLargestVein()).append(" blocks\n");
            
            long lastMinedTime = getLastMined();
            if (lastMinedTime > 0) {
                long hoursSince = (System.currentTimeMillis() - lastMinedTime) / (1000 * 60 * 60);
                sb.append(TextFormat.YELLOW).append("Last Mined: ").append(TextFormat.WHITE);
                if (hoursSince < 1) {
                    sb.append("Less than an hour ago");
                } else if (hoursSince < 24) {
                    sb.append(hoursSince).append(" hour").append(hoursSince > 1 ? "s" : "").append(" ago");
                } else {
                    long days = hoursSince / 24;
                    sb.append(days).append(" day").append(days > 1 ? "s" : "").append(" ago");
                }
            }
            
            return sb.toString();
        }
    }
}
