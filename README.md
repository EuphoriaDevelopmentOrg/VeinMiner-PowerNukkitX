# VeinMiner Plugin for PowerNukkitX

A powerful vein mining plugin for PowerNukkitX that allows players to mine entire veins of ores, trees, and leaves with a single break.

## Features

- 🪨 **Vein Mining** - Mine entire ore veins with one hit
- 🌳 **Tree Chopping** - Chop down entire trees at once
- 🍃 **Leaf Clearing** - Remove all connected leaves quickly
- 🎒 **Auto-Pickup** - Items automatically transfer to inventory
- ⚙️ **Highly Configurable** - Customize every aspect via config
- 📊 **Console Logging** - Track vein mining events in real-time
- 🛠️ **Tool Durability** - Configurable tool wear system
- 🔒 **Sneak to Activate** - Prevents accidental vein mining

## Installation

1. Download `VeinMiner-1.0.1.jar` from releases
2. Place in your server's `plugins` folder
3. Restart your server
4. Configure `plugins/VeinMiner/config.yml` as needed

## Supported Blocks

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

## How to Use

1. **Sneak** (hold Shift) while mining
2. **Break a block** with the proper tool
3. **Watch it work** - All connected blocks are mined instantly
4. **Items auto-pickup** - Goes directly to your inventory

## Configuration

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
```
  leaves: true
```

## Building from Source

```bash
mvn clean package
```

The compiled JAR will be in `target/VeinMiner-1.0.1.jar`

## Requirements

- PowerNukkitX server
- Java 21 or higher

## Support

Found a bug or have a feature request? Open an issue on GitHub!

---

Made with ❤️ by Euphoria Development

