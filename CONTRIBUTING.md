# Contributing to VeinMiner

Thank you for your interest in contributing to VeinMiner! This document provides guidelines and instructions for contributing to the project.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Submitting Changes](#submitting-changes)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Bug Reports](#bug-reports)
- [Feature Requests](#feature-requests)

## 🤝 Code of Conduct

By participating in this project, you agree to maintain a respectful and inclusive environment:

- Be respectful and considerate of others
- Welcome newcomers and help them learn
- Focus on constructive criticism
- Accept responsibility for mistakes
- Prioritize what's best for the community

## 🚀 Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YourUsername/VeinMiner.git
   cd VeinMiner
   ```
3. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/OriginalRepo/VeinMiner.git
   ```
4. **Create a branch** for your changes:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## 🛠️ Development Setup

### Prerequisites

- **Java 21** or higher ([Download](https://adoptium.net/))
- **Maven 3.x** ([Download](https://maven.apache.org/download.cgi))
- **PowerNukkitX** server for testing
- **Git** for version control
- An IDE (IntelliJ IDEA, Eclipse, or VS Code recommended)

### Building the Project

```bash
# Compile and package
mvn clean package

# Run tests (when available)
mvn test

# Install to local Maven repository
mvn clean install
```

The compiled JAR will be in `target/VeinMiner-1.0.2.jar`

### Project Structure

```
VeinMiner/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/veinminer/
│       │       ├── VeinMinerPlugin.java      # Main plugin class
│       │       ├── StatisticsTracker.java    # Statistics system
│       │       └── VeinMinerCommand.java     # Command handler
│       └── resources/
│           ├── plugin.yml                    # Plugin metadata
│           └── config.yml                    # Default configuration
├── pom.xml                                   # Maven configuration
├── README.md                                 # Project documentation
├── CONTRIBUTING.md                           # This file
└── .gitignore                               # Git ignore rules
```

## 🔧 Making Changes

### Before You Start

1. **Check existing issues** to avoid duplicate work
2. **Discuss major changes** by opening an issue first
3. **Keep changes focused** - one feature or fix per PR

### Development Workflow

1. **Pull latest changes**:
   ```bash
   git checkout main
   git pull upstream main
   ```

2. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make your changes** following the [coding standards](#coding-standards)

4. **Test thoroughly** on a local PowerNukkitX server

5. **Commit with clear messages**:
   ```bash
   git add .
   git commit -m "Add feature: description of what you did"
   ```

### Commit Message Guidelines

Use clear, descriptive commit messages:

- **feat**: New feature (e.g., `feat: add blacklist for specific blocks`)
- **fix**: Bug fix (e.g., `fix: resolve crash when mining bedrock`)
- **docs**: Documentation changes (e.g., `docs: update config examples`)
- **style**: Code style/formatting (e.g., `style: fix indentation`)
- **refactor**: Code refactoring (e.g., `refactor: simplify BFS algorithm`)
- **perf**: Performance improvements (e.g., `perf: optimize vein detection`)
- **test**: Adding tests (e.g., `test: add unit tests for statistics`)
- **chore**: Maintenance tasks (e.g., `chore: update dependencies`)

Examples:
```
feat: add cooldown system for vein mining
fix: prevent mining in protected regions
docs: add troubleshooting section to README
refactor: extract particle logic to separate class
```

## 📤 Submitting Changes

### Pull Request Process

1. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

2. **Open a Pull Request** on GitHub with:
   - Clear title describing the change
   - Detailed description of what and why
   - Reference to related issues (e.g., "Fixes #123")
   - Screenshots/GIFs for UI changes

3. **Respond to feedback** from reviewers

4. **Update your PR** if requested:
   ```bash
   git add .
   git commit -m "Address review feedback"
   git push origin feature/your-feature-name
   ```

### Pull Request Checklist

Before submitting, ensure:

- [ ] Code compiles without errors (`mvn clean package`)
- [ ] Follows coding standards (see below)
- [ ] Tested on a PowerNukkitX server
- [ ] Documentation updated (README, config examples)
- [ ] No unnecessary debug code or comments
- [ ] Commit messages are clear and descriptive
- [ ] No merge conflicts with main branch

## 📐 Coding Standards

### Java Style Guidelines

- **Indentation**: 4 spaces (no tabs)
- **Braces**: Opening brace on same line
- **Naming Conventions**:
  - Classes: `PascalCase` (e.g., `VeinMinerPlugin`)
  - Methods: `camelCase` (e.g., `findVein`)
  - Constants: `UPPER_SNAKE_CASE` (e.g., `MAX_BLOCKS`)
  - Variables: `camelCase` (e.g., `veinBlocks`)

### Code Quality

```java
// ✅ Good - Clear, documented, proper naming
/**
 * Find all connected blocks of the same type using BFS
 * @param startBlock The initial block to start from
 * @param visited Set of already visited blocks
 * @return Set of blocks in the vein
 */
private Set<Block> findVein(Block startBlock, Set<Block> visited) {
    Set<Block> vein = new HashSet<>();
    // Implementation...
}

// ❌ Bad - Unclear, no documentation
private Set<Block> fv(Block b, Set<Block> v) {
    Set<Block> s = new HashSet<>();
    // Implementation...
}
```

### Best Practices

1. **Null Safety**: Always check for null before using objects
   ```java
   if (player != null && player.isOnline()) {
       // Safe to use player
   }
   ```

2. **Resource Management**: Close resources properly
   ```java
   try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
       // Use reader
   } // Automatically closed
   ```

3. **Performance**: Consider performance implications
   ```java
   // ✅ Good - Cache results
   String blockId = block.getId();
   if (veinBlocks.contains(blockId)) { ... }
   
   // ❌ Bad - Repeated calls
   if (veinBlocks.contains(block.getId())) {
       String id = block.getId();
   }
   ```

4. **Comments**: Write self-documenting code, use comments for complex logic
   ```java
   // ✅ Good - Clear purpose
   // Check all 26 neighbors in a 3x3x3 cube
   for (int dx = -1; dx <= 1; dx++) { ... }
   
   // ❌ Bad - Obvious comment
   i++; // Increment i
   ```

## 🧪 Testing

### Manual Testing Checklist

Before submitting changes, test:

1. **Basic Functionality**
   - [ ] Plugin loads without errors
   - [ ] Configuration loads correctly
   - [ ] Vein mining works for ores/logs/leaves
   - [ ] Commands execute properly

2. **Edge Cases**
   - [ ] Mining with full inventory
   - [ ] Mining with broken tool
   - [ ] Mining in disabled worlds
   - [ ] Mining with Fortune/Silk Touch

3. **Performance**
   - [ ] No lag with large veins (64+ blocks)
   - [ ] No memory leaks during extended use
   - [ ] Statistics save/load correctly

4. **Compatibility**
   - [ ] Works with latest PowerNukkitX version
   - [ ] No conflicts with common plugins
   - [ ] Respects region protection plugins

### Setting Up Test Server

1. Download PowerNukkitX server
2. Place your compiled JAR in `plugins/`
3. Start server and test all features
4. Check console for errors/warnings

## 🐛 Bug Reports

Found a bug? Help us fix it!

### Before Reporting

1. **Search existing issues** to avoid duplicates
2. **Test on latest version** of the plugin
3. **Reproduce the bug** consistently
4. **Gather information** (logs, config, version)

### Creating a Bug Report

Use the following template:

```markdown
**Describe the bug**
A clear description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. Configure plugin with '...'
2. Mine block '....'
3. Observe error

**Expected behavior**
What you expected to happen.

**Actual behavior**
What actually happened.

**Environment**
- VeinMiner version: [e.g., 1.0.2]
- PowerNukkitX version: [e.g., 2.0.0-SNAPSHOT]
- Java version: [e.g., Java 21]
- Server OS: [e.g., Windows 11]

**Logs**
```
Paste relevant logs here
```

**Configuration**
```yaml
# Paste relevant config.yml sections
```

**Screenshots**
If applicable, add screenshots.
```

## 💡 Feature Requests

Have an idea? We'd love to hear it!

### Creating a Feature Request

```markdown
**Is your feature request related to a problem?**
A clear description of the problem. Ex. "I'm frustrated when [...]"

**Describe the solution you'd like**
A clear description of what you want to happen.

**Describe alternatives you've considered**
Other solutions or features you've considered.

**Additional context**
Any other context, mockups, or examples.

**Configuration example** (if applicable)
```yaml
# How you envision the config
new-feature:
  enabled: true
  option: value
```
```

## 📞 Getting Help

Need help contributing?
- **Discussions**: Start a discussion on GitHub
- **Issues**: Ask questions in existing issues

## 🎯 Good First Issues

New to the project? Look for issues labeled:
- `good first issue` - Easy tasks for beginners
- `help wanted` - We'd appreciate help on these
- `documentation` - Improve docs (no coding needed)

## 🌟 Recognition

Contributors will be:
- Listed in the project README
- Credited in release notes
- Recognized in our community

Thank you for contributing to VeinMiner! 🎉

---

*Last updated: January 28, 2026*
