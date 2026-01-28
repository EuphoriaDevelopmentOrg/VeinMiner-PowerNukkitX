package com.veinminer;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandEnum;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.utils.TextFormat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class VeinMinerCommand extends Command {
    
    private final VeinMinerPlugin plugin;
    private final Set<UUID> disabledPlayers;
    
    public VeinMinerCommand(VeinMinerPlugin plugin) {
        super("veinminer", "Mine entire veins at once! Use /vm help for more info", "/veinminer", new String[]{"vm", "vmine"});
        this.plugin = plugin;
        this.disabledPlayers = new HashSet<>();
        // Don't set permission here - let plugin.yml handle it
        
        // Set up command parameters for proper display
        this.commandParameters.clear();
        this.commandParameters.put("default", new CommandParameter[]{
            CommandParameter.newEnum("action", true, new CommandEnum("VeinMinerAction", 
                new String[]{"help", "reload", "stats", "toggle", "on", "off", "status"}))
        });
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
        // Always return true to prevent usage message
        try {
            plugin.getLogger().info("Command executed by " + sender.getName() + " with args: " + String.join(", ", args));
            
            if (!sender.hasPermission("veinminer.command")) {
                sender.sendMessage(TextFormat.RED + "You don't have permission to use this command.");
                return true;
            }
            
            if (args.length == 0) {
                plugin.getLogger().info("Showing detailed help to " + sender.getName());
                sendDetailedHelp(sender);
                return true;
            }
            
            String subcommand = args[0].toLowerCase();
            plugin.getLogger().info("Subcommand: " + subcommand);
            
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
            plugin.getLogger().error("Error executing command: " + e.getMessage());
            e.printStackTrace();
            try {
                sender.sendMessage(TextFormat.RED + "An error occurred: " + e.getMessage());
            } catch (Exception ex) {
                plugin.getLogger().error("Error sending error message: " + ex.getMessage());
            }
            return true;
        }
    }
    
    private void sendMainHelp(CommandSender sender) {
        sender.sendMessage(TextFormat.GOLD + "========== " + TextFormat.BOLD + "VeinMiner" + TextFormat.RESET + TextFormat.GOLD + " ==========");
        sender.sendMessage(TextFormat.YELLOW + "Version: " + TextFormat.WHITE + plugin.getDescription().getVersion());
        sender.sendMessage("");
        sender.sendMessage(TextFormat.AQUA + "Quick Commands:");
        if (sender.hasPermission("veinminer.toggle")) {
            sender.sendMessage(TextFormat.YELLOW + "  /vm toggle" + TextFormat.GRAY + " - Toggle vein mining on/off");
            sender.sendMessage(TextFormat.YELLOW + "  /vm status" + TextFormat.GRAY + " - Check your current status");
        }
        if (sender.hasPermission("veinminer.stats")) {
            sender.sendMessage(TextFormat.YELLOW + "  /vm stats" + TextFormat.GRAY + " - View your statistics");
        }
        if (sender.hasPermission("veinminer.reload")) {
            sender.sendMessage(TextFormat.YELLOW + "  /vm reload" + TextFormat.GRAY + " - Reload configuration");
        }
        sender.sendMessage("");
        sender.sendMessage(TextFormat.GRAY + "Type " + TextFormat.WHITE + "/vm help" + TextFormat.GRAY + " for detailed information.");
        sender.sendMessage(TextFormat.GOLD + "====================================");
    }
    
    private void sendDetailedHelp(CommandSender sender) {
        sender.sendMessage(TextFormat.GOLD + "===== VeinMiner Help =====");
        sender.sendMessage("");
        
        if (sender.hasPermission("veinminer.toggle")) {
            sender.sendMessage(TextFormat.AQUA + "Toggle Commands:");
            sender.sendMessage(TextFormat.YELLOW + "  /vm toggle " + TextFormat.GRAY + "- Toggle on/off");
            sender.sendMessage(TextFormat.YELLOW + "  /vm on " + TextFormat.GRAY + "- Enable");
            sender.sendMessage(TextFormat.YELLOW + "  /vm off " + TextFormat.GRAY + "- Disable");
            sender.sendMessage(TextFormat.YELLOW + "  /vm status " + TextFormat.GRAY + "- Check status");
            sender.sendMessage("");
        }
        
        if (sender.hasPermission("veinminer.stats")) {
            sender.sendMessage(TextFormat.AQUA + "Statistics:");
            sender.sendMessage(TextFormat.YELLOW + "  /vm stats " + TextFormat.GRAY + "- View your stats");
            sender.sendMessage("");
        }
        
        if (sender.hasPermission("veinminer.reload")) {
            sender.sendMessage(TextFormat.AQUA + "Admin:");
            sender.sendMessage(TextFormat.YELLOW + "  /vm reload " + TextFormat.GRAY + "- Reload config");
            sender.sendMessage("");
        }
        
        sender.sendMessage(TextFormat.AQUA + "How to Use:");
        sender.sendMessage(TextFormat.WHITE + "  Sneak while mining ores or logs");
        sender.sendMessage(TextFormat.WHITE + "  to break entire veins at once!");
        sender.sendMessage("");
        sender.sendMessage(TextFormat.GRAY + "Aliases: /veinminer, /vm, /vmine");
    }
    
    public boolean isDisabled(Player player) {
        return disabledPlayers.contains(player.getUniqueId());
    }
}
