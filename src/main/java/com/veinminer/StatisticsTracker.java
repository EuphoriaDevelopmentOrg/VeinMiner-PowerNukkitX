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
    
    private final VeinMinerPlugin plugin;
    private final Map<UUID, PlayerStats> playerStats;
    private final boolean enabled;
    private final boolean saveToFile;
    private volatile boolean saving = false;
    
    public StatisticsTracker(VeinMinerPlugin plugin) {
        this.plugin = plugin;
        this.playerStats = new ConcurrentHashMap<>();
        
        Config config = plugin.getConfig();
        this.enabled = config.getBoolean("statistics.enabled", true);
        this.saveToFile = config.getBoolean("statistics.save-to-file", true);
        
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
        
        stats.incrementVeins();
        stats.addBlocks(blockCount);
        stats.updateLastMined();
        stats.updateLargestVein(blockCount);
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
                
                statsConfig.set(uuidStr, data);
            }
            
            statsConfig.save();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save statistics: " + e.getMessage());
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
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load stats for " + uuidStr);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load statistics file: " + e.getMessage());
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
