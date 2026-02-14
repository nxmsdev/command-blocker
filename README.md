# CommandBlocker

A lightweight PaperMC plugin for blocking commands on Minecraft 1.21.1 servers.

## Features

- Block Commands - Prevent players from using specific commands
- Invisible Blocking - Blocked commands appear as if they don't exist
- Tab Complete Filtering - Blocked commands are hidden from tab completion
- Namespace Support - Block commands with plugin prefix (e.g., minecraft:msg, essentials:home)
- Bypass System - Allow specific players to use blocked commands
- Language Support - English and Polish, selectable in config.yml

## Permissions

| Permission             | Description |
|:-----------------------|:------------|
| commandblocker.command | Access to /commandblock and /cb commands |
| commandblocker.add     | Access to /commandblock add command |
| commandblocker.remove  | Access to /commandblock remove command |
| commandblocker.list    | Access to /commandblock list command |
| commandblocker.reload  | Access to /commandblock reload command |
| commandblocker.bypass  | Bypass blocked commands restriction |
| commandblocker.admin   | Grants access to all CommandBlock commands and bypass |

## Commands

Main commands: `/commandblocker` or `/cb`

| Command | Description |
|:--------|:------------|
| /cb add <plugin:command> | Add command to blocked list |
| /cb remove <plugin:command> | Remove command from blocked list |
| /cb list | Show all blocked commands |
| /cb reload | Reload configuration and messages |
| /cb help | Show help message |

## Configuration

### config.yml

    # Language setting (en / pl)
    language: en

    # List of blocked commands
    # Format: plugin:command (e.g., minecraft:msg, essentials:home)
    blocked-commands:
      - "plguin:command"

### Language Selection

    language: en   # English
    language: pl   # Polish

### Command Format

Commands must be specified with their namespace prefix:

- `minecraft:me` - Vanilla /me command
- `minecraft:tell` - Vanilla /tell command
- `essentials:home` - Essentials /home command
- `pluginname:command` - Any plugin command

## Behavior

### For Regular Players

- Blocked commands are completely hidden from tab completion
- Executing a blocked command shows "Unknown command" message
- Commands appear as if they never existed

### For Players with Bypass

- Players with `commandblock.bypass` permission can use all commands
- All commands are visible in tab completion
- Blocked commands execute normally

## Examples

Block the /me command:

    /cb add minecraft:me

Block Essentials /home command:

    /cb add essentials:home

Unblock a command:

    /cb remove minecraft:me

View all blocked commands:

    /cb list

## Other

Author: [nxmsdev](https://github.com/nxmsdev)

Website: https://www.nxms.dev

License: [CC BY-NY 4.0](https://creativecommons.org/licenses/by-nc/4.0/legalcode)