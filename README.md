# VeinMiner Plugin for PowerNukkitX

A powerful, feature-rich vein mining plugin for PowerNukkitX that allows players to mine entire veins of ores, trees, and leaves with a single break.

## ✨ Features

- 🪨 **Vein Mining** - Mine entire ore veins with one hit
- 🌳 **Tree Chopping** - Chop down entire trees at once
- 🍃 **Leaf Clearing** - Remove all connected leaves quickly
- 🎒 **Auto-Pickup** - Items automatically transfer to inventory
- 📊 **Statistics Tracking** - Track player mining stats with persistent storage
- 🎨 **Visual Effects** - Particle effects for immersive mining
- 🔊 **Sound Effects** - Audio feedback on vein completion
- 🌍 **World Restrictions** - Disable vein mining in specific worlds
- ✨ **Fortune/Silk Touch** - Full enchantment support
- 🎁 **Experience Drops** - XP orbs spawn based on vanilla rates
- 🔔 **Update Checker** - Get notified when new versions are available
- 💬 **Commands** - Player and admin commands with permissions
- ⚙️ **Highly Configurable** - Customize every aspect via config
- 🛠️ **Tool Durability** - Configurable tool wear system
- 🔒 **Sneak to Activate** - Prevents accidental vein mining

## 📦 Installation

1. Download `VeinMiner-1.0.2.jar` from [releases](https://github.com/EuphoriaDevelopmentOrg/VeinMiner-PowerNukkitX/releases)
2. Place in your server's `plugins` folder
3. Restart your server
4. Configure `plugins/VeinMiner/config.yml` as needed

## 🎮 How to Use

1. **Sneak** (hold Shift) while mining
2. **Break a block** with the proper tool
3. **Watch it work** - All connected blocks are mined instantly
4. **Items auto-pickup** - Goes directly to your inventory

### Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/veinminer` | Show plugin help | `veinminer.command` |
| `/veinminer reload` | Reload configuration | `veinminer.reload` |
| `/veinminer stats` | View your mining statistics | `veinminer.stats` |
| `/veinminer toggle` | Enable/disable vein mining | `veinminer.toggle` |

**Aliases**: `/vm`

### Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `veinminer.*` | All permissions | op |
| `veinminer.use` | Use vein mining | true |
| `veinminer.command` | Use /veinminer command | true |
| `veinminer.reload` | Reload configuration | op |
| `veinminer.stats` | View statistics | true |
| `veinminer.toggle` | Toggle vein mining | true |

## 🎯 Supported Blocks

### Ores (18 types)
- **Overworld**: Coal, Iron, Gold, Diamond, Emerald, Lapis, Redstone, Copper
- **Deepslate**: All deepslate ore variants
- **Nether**: Nether Gold Ore, Quartz Ore, Ancient Debris

### Logs (10 types - natural only)
- **Overworld**: Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Mangrove, Cherry
- **Nether**: Crimson Stem, Warped Stem
- *Note: Stripped/processed wood is NOT vein-mineable*

### Leaves (8 types)
- All vanilla leaf types matching supported trees

## ⚙️ Configuration

Edit `plugins/VeinMiner/config.yml`:

```yaml
# Max blocks per vein (-1 for unlimited)
max-blocks: 64

# Require sneaking to activate
sneak-required: true

# Auto-pickup to inventory
auto-pickup:
  enabled: true
  full-inventory-action: "drop"  # or "delete"

# Tool durability
tool-durability:
  enabled: true
  multiplier: 1.0

# World restrictions (disable vein mining in these worlds)
disabled-worlds:
  - "creative_world"
  - "spawn"

# Visual and audio effects
effects:
  particles: true  # Show particle effects when breaking blocks
  sounds: true     # Play sound when vein mining completes

# Statistics tracking
statistics:
  enabled: true
  save-to-file: true  # Persist stats to stats.yml

# Update checker Leave as is for default
update-checker:
  enabled: true
  repository: "EuphoriaDevelopmentOrg/VeinMiner-PowerNukkitX"

# Console logging
logging:
  enabled: true
  log-vein-mining: true
  log-config-loading: true

# Enable/disable block categories
enabled-blocks:
  ores: true
  logs: true
  leaves: true

# Custom messages (supports color codes with &)
messages:
  reload-success: "&aConfiguration reloaded successfully!"
  stats-header: "&6&l=== Your VeinMiner Statistics ==="
  stats-line: "&eTotal Veins: &f{veins} &7| &eTotal Blocks: &f{blocks} &7| &eLargest Vein: &f{largest}"
  toggle-enabled: "&aVein Mining enabled!"
  toggle-disabled: "&cVein Mining disabled!"
  inventory-full: "&c{count} items were {action} (inventory full)"
```

## 🔨 Building from Source

```bash
git clone https://github.com/EuphoriaDevelopmentOrg/VeinMiner-PowerNukkitX.git
cd VeinMiner
mvn clean package
```

The compiled JAR will be in `target/VeinMiner-1.0.2.jar`

## 📋 Requirements

- **PowerNukkitX** server (2.0.0-SNAPSHOT or higher)
- **Java 21** or higher
- **Maven 3.x** (for building from source)

## 📊 Statistics

The plugin tracks the following statistics per player:
- Total veins mined
- Total blocks broken via vein mining
- Largest single vein mined
- Last mining timestamp

Statistics are saved to `plugins/VeinMiner/stats.yml` and persist across server restarts.

## 🐛 Troubleshooting

**Vein mining not working?**
- Ensure you have the `veinminer.use` permission
- Check if you're sneaking (Shift key)
- Verify the world is not in the `disabled-worlds` list
- Use `/veinminer toggle` to check if you've disabled it

**Items not auto-picking up?**
- Check `auto-pickup.enabled` is `true` in config
- Ensure your inventory has space
- Configure `full-inventory-action` for overflow handling

**No statistics showing?**
- Enable statistics in config: `statistics.enabled: true`
- Ensure `stats.yml` is writable in the plugin folder

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## 📝 License

This project is open source. Feel free to use and modify for your server.

## 💬 Support

- Found a bug? [Open an issue](https://github.com/EuphoriaDevelopmentOrg/VeinMiner-PowerNukkitX/issues)
- Have a feature request? [Start a discussion](https://github.com/EuphoriaDevelopmentOrg/VeinMiner-PowerNukkitX/discussions)
- Need help? Check our [Wiki](https://github.com/EuphoriaDevelopmentOrg/VeinMiner-PowerNukkitX/wiki)

## 🌟 Star This Project

If you find VeinMiner useful, please consider giving it a star! It helps others discover the project.

---

Made with ❤️ for the PowerNukkitX community
