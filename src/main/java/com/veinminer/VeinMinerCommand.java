package com.veinminer;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandEnum;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.utils.TextFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Command handler for VeinMiner
 * Implements Listener to clean up player data on quit (prevents memory leaks)
 */
public class VeinMinerCommand extends Command implements Listener {
    
    // Logging tag
    private static final String LOG_TAG = "[VeinMiner] ";
    
    private final VeinMinerPlugin plugin;
    private final Set<UUID> disabledPlayers;
    
    public VeinMinerCommand(VeinMinerPlugin plugin) {
        super("veinminer", "Mine entire veins at once! Use /vm help for more info", "/veinminer", new String[]{"vm", "vmine"});
        this.plugin = plugin;
        this.disabledPlayers = ConcurrentHashMap.newKeySet(); // Thread-safe set
        // Don't set permission here - let plugin.yml handle it
        
        // Register this as a listener for cleanup
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Set up command parameters for proper display
        this.commandParameters.clear();
        this.commandParameters.put("default", new CommandParameter[]{
            CommandParameter.newEnum("action", true, new CommandEnum("VeinMinerAction", 
                new String[]{"help", "reload", "stats", "toggle", "on", "off", "status"}))
        });
    }
    
    /**
     * Clean up player data when they disconnect to prevent memory leaks
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        disabledPlayers.remove(uuid);
    }
    
    /**
     * Provides tab completion for command arguments
     */
    public String[] getSubcommands(CommandSender sender) {
        List<String> subcommands = new ArrayList<>();
        subcommands.add("help");
        if (sender.hasPermission("veinminer.reload")) {
            subcommands.add("reload");
        }
        if (sender.hasPermission("veinminer.stats")) {
            subcommands.add("stats");
        }
        if (sender.hasPermission("veinminer.toggle")) {
            subcommands.add("toggle");
            subcommands.add("on");
            subcommands.add("off");
            subcommands.add("status");
        }
        return subcommands.toArray(new String[0]);
    }
    
    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        try {
            if (!sender.hasPermission("veinminer.command")) {
                sender.sendMessage(TextFormat.RED + "You don't have permission to use this command.");
                return true;
            }
            
            if (args.length == 0) {
                sendDetailedHelp(sender);
                return true;
            }
            
            String subcommand = args[0].toLowerCase();
            
            switch (subcommand) {
                case "help":
                case "?":
                    sendDetailedHelp(sender);
                    return true;
                    
                case "reload":
                case "rl":
                    if (!sender.hasPermission("veinminer.reload")) {
                        sender.sendMessage(TextFormat.RED + "You don't have permission to reload.");
                        return true;
                    }
                    
                    plugin.reloadConfig();
                    plugin.reloadConfiguration();
                    sender.sendMessage(TextFormat.GREEN + "VeinMiner configuration reloaded!");
                    return true;
                    
                case "stats":
                case "statistics":
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(TextFormat.RED + "This command can only be used by players.");
                        return true;
                    }
                    
                    if (!sender.hasPermission("veinminer.stats")) {
                        sender.sendMessage(TextFormat.RED + "You don't have permission to view stats.");
                        return true;
                    }
                    
                    Player player = (Player) sender;
                    StatisticsTracker.PlayerStats stats = plugin.getStatsTracker().getStats(player);
                    sender.sendMessage(TextFormat.GOLD + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                    sender.sendMessage(stats.getFormattedStats());
                    sender.sendMessage(TextFormat.GOLD + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                    return true;
                    
                case "toggle":
                case "t":
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(TextFormat.RED + "This command can only be used by players.");
                        return true;
                    }
                    
                    if (!sender.hasPermission("veinminer.toggle")) {
                        sender.sendMessage(TextFormat.RED + "You don't have permission to toggle vein mining.");
                        return true;
                    }
                    
                    Player togglePlayer = (Player) sender;
                    UUID uuid = togglePlayer.getUniqueId();
                    
                    if (disabledPlayers.contains(uuid)) {
                        disabledPlayers.remove(uuid);
                        sender.sendMessage(TextFormat.GREEN + "✓ VeinMiner enabled! Sneak while mining to activate.");
                    } else {
                        disabledPlayers.add(uuid);
                        sender.sendMessage(TextFormat.RED + "✗ VeinMiner disabled! You'll mine normally.");
                    }
                    return true;
                    
                case "on":
                case "enable":
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(TextFormat.RED + "This command can only be used by players.");
                        return true;
                    }
                    
                    if (!sender.hasPermission("veinminer.toggle")) {
                        sender.sendMessage(TextFormat.RED + "You don't have permission to toggle vein mining.");
                        return true;
                    }
                    
                    Player enablePlayer = (Player) sender;
                    UUID enableUuid = enablePlayer.getUniqueId();
                    
                    if (!disabledPlayers.contains(enableUuid)) {
                        sender.sendMessage(TextFormat.YELLOW + "VeinMiner is already enabled!");
                    } else {
                        disabledPlayers.remove(enableUuid);
                        sender.sendMessage(TextFormat.GREEN + "✓ VeinMiner enabled! Sneak while mining to activate.");
                    }
                    return true;
                    
                case "off":
                case "disable":
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(TextFormat.RED + "This command can only be used by players.");
                        return true;
                    }
                    
                    if (!sender.hasPermission("veinminer.toggle")) {
                        sender.sendMessage(TextFormat.RED + "You don't have permission to toggle vein mining.");
                        return true;
                    }
                    
                    Player disablePlayer = (Player) sender;
                    UUID disableUuid = disablePlayer.getUniqueId();
                    
                    if (disabledPlayers.contains(disableUuid)) {
                        sender.sendMessage(TextFormat.YELLOW + "VeinMiner is already disabled!");
                    } else {
                        disabledPlayers.add(disableUuid);
                        sender.sendMessage(TextFormat.RED + "✗ VeinMiner disabled! You'll mine normally.");
                    }
                    return true;
                    
                case "status":
                case "info":
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(TextFormat.RED + "This command can only be used by players.");
                        return true;
                    }
                    
                    if (!sender.hasPermission("veinminer.toggle")) {
                        sender.sendMessage(TextFormat.RED + "You don't have permission to check status.");
                        return true;
                    }
                    
                    Player statusPlayer = (Player) sender;
                    boolean isEnabled = !disabledPlayers.contains(statusPlayer.getUniqueId());
                    
                    sender.sendMessage(TextFormat.GOLD + "▬▬▬▬▬▬▬▬ " + TextFormat.BOLD + "VeinMiner Status" + TextFormat.RESET + TextFormat.GOLD + " ▬▬▬▬▬▬▬▬");
                    sender.sendMessage(TextFormat.YELLOW + "Status: " + (isEnabled ? TextFormat.GREEN + "✓ Enabled" : TextFormat.RED + "✗ Disabled"));
                    sender.sendMessage(TextFormat.YELLOW + "Version: " + TextFormat.WHITE + plugin.getDescription().getVersion());
                    if (isEnabled) {
                        sender.sendMessage(TextFormat.GRAY + "Tip: Sneak while mining to activate!");
                    } else {
                        sender.sendMessage(TextFormat.GRAY + "Use /vm on to enable vein mining.");
                    }
                    sender.sendMessage(TextFormat.GOLD + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                    return true;
                    
                default:
                    sender.sendMessage(TextFormat.RED + "Unknown subcommand: " + TextFormat.GRAY + subcommand);
                    sender.sendMessage(TextFormat.YELLOW + "Use " + TextFormat.WHITE + "/vm help" + TextFormat.YELLOW + " for a list of commands.");
                    return true;
            }
        } catch (Exception e) {
            plugin.getLogger().error(LOG_TAG + "Error executing command: " + e.getMessage());
            e.printStackTrace();
            try {
                sender.sendMessage(TextFormat.RED + "An error occurred: " + e.getMessage());
            } catch (Exception ex) {
                plugin.getLogger().error(LOG_TAG + "Error sending error message: " + ex.getMessage());
            }
            return true;
        }
    }
    
    private void sendDetailedHelp(CommandSender sender) {
        sender.sendMessage(TextFormat.GOLD + "VeinMiner Help");
        sender.sendMessage(TextFormat.YELLOW + "Commands: /vm toggle, /vm stats, /vm reload");
        sender.sendMessage(TextFormat.GRAY + "Use /vm <command> for more info");
    }
    
    /**
     * Check if vein mining is disabled for a player
     * @param player The player to check
     * @return true if disabled
     */
    public boolean isDisabled(Player player) {
        if (player == null) {
            return false;
        }
        return disabledPlayers.contains(player.getUniqueId());
    }
}
