package com.veinminer;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.utils.TextFormat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VeinMinerCommand extends Command {
    
    private final VeinMinerPlugin plugin;
    private final Set<UUID> disabledPlayers;
    
    public VeinMinerCommand(VeinMinerPlugin plugin) {
        super("veinminer", "VeinMiner main command", "/veinminer <reload|stats|toggle>", new String[]{"vm"});
        this.plugin = plugin;
        this.disabledPlayers = new HashSet<>();
        this.setPermission("veinminer.command");
    }
    
    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("veinminer.command")) {
            sender.sendMessage(TextFormat.RED + "You don't have permission to use this command.");
            return false;
        }
        
        if (args.length == 0) {
            sender.sendMessage(TextFormat.GOLD + "VeinMiner v" + plugin.getDescription().getVersion());
            sender.sendMessage(TextFormat.YELLOW + "Usage: /veinminer <reload|stats|toggle>");
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("veinminer.reload")) {
                    sender.sendMessage(TextFormat.RED + "You don't have permission to reload.");
                    return false;
                }
                
                plugin.reloadConfig();
                plugin.reloadConfiguration();
                sender.sendMessage(TextFormat.GREEN + "VeinMiner configuration reloaded!");
                return true;
                
            case "stats":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(TextFormat.RED + "This command can only be used by players.");
                    return false;
                }
                
                if (!sender.hasPermission("veinminer.stats")) {
                    sender.sendMessage(TextFormat.RED + "You don't have permission to view stats.");
                    return false;
                }
                
                Player player = (Player) sender;
                StatisticsTracker.PlayerStats stats = plugin.getStatsTracker().getStats(player);
                sender.sendMessage(stats.getFormattedStats());
                return true;
                
            case "toggle":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(TextFormat.RED + "This command can only be used by players.");
                    return false;
                }
                
                if (!sender.hasPermission("veinminer.toggle")) {
                    sender.sendMessage(TextFormat.RED + "You don't have permission to toggle vein mining.");
                    return false;
                }
                
                Player togglePlayer = (Player) sender;
                UUID uuid = togglePlayer.getUniqueId();
                
                if (disabledPlayers.contains(uuid)) {
                    disabledPlayers.remove(uuid);
                    sender.sendMessage(TextFormat.GREEN + "VeinMiner enabled!");
                } else {
                    disabledPlayers.add(uuid);
                    sender.sendMessage(TextFormat.RED + "VeinMiner disabled!");
                }
                return true;
                
            default:
                sender.sendMessage(TextFormat.RED + "Unknown subcommand. Use: reload, stats, or toggle");
                return false;
        }
    }
    
    public boolean isDisabled(Player player) {
        return disabledPlayers.contains(player.getUniqueId());
    }
}
