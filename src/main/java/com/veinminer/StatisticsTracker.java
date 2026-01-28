package com.veinminer;

import cn.nukkit.Player;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StatisticsTracker {
    
    private final VeinMinerPlugin plugin;
    private final Map<UUID, PlayerStats> playerStats;
    private final boolean enabled;
    private final boolean saveToFile;
    
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
    
    public void recordVeinMine(Player player, int blockCount) {
        if (!enabled) return;
        
        UUID uuid = player.getUniqueId();
        PlayerStats stats = playerStats.computeIfAbsent(uuid, k -> new PlayerStats(player.getName()));
        
        stats.totalVeins++;
        stats.totalBlocks += blockCount;
        stats.lastMined = System.currentTimeMillis();
        
        if (blockCount > stats.largestVein) {
            stats.largestVein = blockCount;
        }
    }
    
    public PlayerStats getStats(Player player) {
        return playerStats.getOrDefault(player.getUniqueId(), new PlayerStats(player.getName()));
    }
    
    public void saveStats() {
        if (!enabled || !saveToFile) return;
        
        File statsFile = new File(plugin.getDataFolder(), "stats.yml");
        Config statsConfig = new Config(statsFile, Config.YAML);
        
        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            String uuidStr = entry.getKey().toString();
            PlayerStats stats = entry.getValue();
            
            Map<String, Object> data = new HashMap<>();
            data.put("name", stats.playerName);
            data.put("totalVeins", stats.totalVeins);
            data.put("totalBlocks", stats.totalBlocks);
            data.put("largestVein", stats.largestVein);
            data.put("lastMined", stats.lastMined);
            
            statsConfig.set(uuidStr, data);
        }
        
        statsConfig.save();
    }
    
    private void loadStats() {
        File statsFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!statsFile.exists()) return;
        
        Config statsConfig = new Config(statsFile, Config.YAML);
        
        for (String uuidStr : statsConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                Map<String, Object> data = statsConfig.getSection(uuidStr).getAllMap();
                
                PlayerStats stats = new PlayerStats((String) data.get("name"));
                stats.totalVeins = ((Number) data.getOrDefault("totalVeins", 0)).intValue();
                stats.totalBlocks = ((Number) data.getOrDefault("totalBlocks", 0)).intValue();
                stats.largestVein = ((Number) data.getOrDefault("largestVein", 0)).intValue();
                stats.lastMined = ((Number) data.getOrDefault("lastMined", 0L)).longValue();
                
                playerStats.put(uuid, stats);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load stats for " + uuidStr);
            }
        }
    }
    
    public static class PlayerStats {
        public String playerName;
        public int totalVeins;
        public int totalBlocks;
        public int largestVein;
        public long lastMined;
        
        public PlayerStats(String playerName) {
            this.playerName = playerName;
            this.totalVeins = 0;
            this.totalBlocks = 0;
            this.largestVein = 0;
            this.lastMined = 0;
        }
        
        public String getFormattedStats() {
            StringBuilder sb = new StringBuilder();
            sb.append(TextFormat.GOLD).append(TextFormat.BOLD).append("=== VeinMiner Statistics ===\n");
            sb.append(TextFormat.YELLOW).append("Total Veins Mined: ").append(TextFormat.WHITE).append(totalVeins).append("\n");
            sb.append(TextFormat.YELLOW).append("Total Blocks Mined: ").append(TextFormat.WHITE).append(totalBlocks).append("\n");
            sb.append(TextFormat.YELLOW).append("Largest Vein: ").append(TextFormat.WHITE).append(largestVein).append(" blocks\n");
            
            if (lastMined > 0) {
                long hoursSince = (System.currentTimeMillis() - lastMined) / (1000 * 60 * 60);
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
