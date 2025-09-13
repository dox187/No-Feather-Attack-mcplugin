# FeatherAttackCancel

Feather hit is not an attack — a small Paper plugin that turns feather hits into knockback-only interactions. It cancels damage (so villagers don’t get angry and no damage side-effects fire) while still applying a light custom knockback.

## Features
- Knockback-only feather hits: cancels the damage event and applies custom push.
- Per-player toggle via command `/featherhit`.
- Auto-enable on player join (and on plugin reload for already-online players).
- Optional clearing of mob target on feather hit (configurable).
- Client language message support: `en_us`, `de_de`, `fr_fr`, `es_es`, `hu_hu`.
- Builds for multiple MC versions via Maven profiles (1.21.7 and 1.21.8).

## Requirements
- Java 21 (Paper 1.21+ requires Java 21).
- PaperMC server 1.21.7 or 1.21.8 (matching the built JAR).

## Installation
1. Download/build the JAR for your server version (see Build section).
2. Place the JAR into your server’s `plugins/` folder.
3. Start the server. The plugin will create:
   - `plugins/FeatherAttackCancel/config.yml`
   - `plugins/FeatherAttackCancel/lang/*.yml`

## Commands
- `/featherhit` — Toggle feather knockback-only mode for yourself.

Permissions: none (by default all players can use the command).

## Configuration
File: `plugins/FeatherAttackCancel/config.yml` (use spaces only, no TABs)

Keys
- trigger-item: Uppercase Bukkit material name for the item that triggers the knockback-only hit. Default: FEATHER

- knockback:
  - min-charge: Number (0.0–1.0). Minimum attack cooldown required to apply knockback. 1.0 = fully charged. Default: 0.9
  - horizontal: Number. Base horizontal strength of the knockback vector. Default: 0.6
  - min-upward: Number. Ensures at least this upward (Y) component so hits don’t push targets downward. Default: 0.35
  - scale-by-charge: Boolean. If true, scales horizontal strength by the current attack charge (prevents weak spam from being as strong as full hits). Default: false
  - clamp-velocity: Boolean. If true, clamps the target’s final velocity magnitude to avoid “stacking” launches. Default: true
  - max-velocity: Number. Maximum final velocity magnitude when clamping is enabled. Default: 1.2

- behavior:
  - auto-enable-on-enable: Boolean. When the plugin enables (e.g., server start/reload), automatically enables the feature for players already online. Default: true
  - auto-enable-on-join: Boolean. Automatically enables the feature for players when they join. Default: true

Applying changes
- There is no reload subcommand; `/featherhit` only toggles the feature for the executing player.
- To apply config changes, restart the server (or stop/start).

Language files
- Path: `plugins/FeatherAttackCancel/lang/`
- Fallback order: exact client tag (e.g., `de-de.yml`) → underscore form (e.g., `de_de.yml`) → `en_us.yml`.
- Color codes: use `&` (e.g., `&6`, `&a`); messages are rendered via Adventure components.

Examples
```yaml
# Softer knockback that scales with charge
trigger-item: FEATHER
knockback:
  min-charge: 0.9
  horizontal: 0.45
  min-upward: 0.30
  scale-by-charge: true
  clamp-velocity: true
  max-velocity: 1.0
behavior:
  auto-enable-on-enable: true
  auto-enable-on-join: true
```

## Notes on Namespaced Command Suggestions
If tab-complete shows a namespaced command like `/featherattackcancel:featherhit`, this comes from the plugin name namespace. On Paper, you can hide namespaced suggestions by setting `send-namespaced=false` in `paper-global.yml` (or relevant Paper config for your version).

## Build
This project uses Maven profiles to build for multiple MC versions.

Profiles included:
- `mc-1.21.7` (default) → Paper API `1.21.7-R0.1-SNAPSHOT`
- `mc-1.21.8` → Paper API `1.21.8-R0.1-SNAPSHOT`

Artifacts are created with a classifier indicating the MC version, e.g.:
- `target/featherhit.FeatherCancel-1.0.0-mc-1.21.7.jar`
- `target/featherhit.FeatherCancel-1.0.0-mc-1.21.8.jar`

Build commands (PowerShell on Windows):
```powershell
# Default build (1.21.7)
mvn -q -DskipTests package

# Build for 1.21.8
mvn -q -P mc-1.21.8 -DskipTests package
```

Or run the provided batch script:
```powershell
.\build.bat
```
This will build both versions and list the produced JARs.

### Adding new versions
1. Add a new Maven profile in `pom.xml` with properties:
   - `mcVersion`, `paper-api.version`, and (if needed) `java.release`.
2. Build with `-P mc-<version>`.

## How it Works
- Listens to `EntityDamageByEntityEvent`. When a player with the feature enabled hits with a `FEATHER`:
  - Cancels the damage event to avoid angering villagers and other damage-based triggers.
  - Applies a small custom velocity to simulate a push.
  - Optionally clears mob target if `clear-mob-target` is `true`.

## Troubleshooting
- Plugin doesn’t load: ensure your server version matches the JAR’s `mc-<version>` classifier and that you’re on Java 21.
- Namespaced command showing: set `send-namespaced=false` in Paper config.
- Villagers still get angry: verify the hit is with a `FEATHER` and that damage is being cancelled; also check if other plugins interfere.
- No messages in your language: confirm the correct `lang/xx_yy.yml` exists. The plugin falls back to `en_us.yml`.

## Project Metadata
- Name: `FeatherAttackCancel`
- Main: `com.dox187.featherhit.FeatherCancel`
- Author: `dox187`

## License
This project is licensed under the GNU Affero General Public License v3.0 or later (AGPL-3.0-or-later). See the `LICENSE` file for details.

Key points (non-legal summary):
- You may use, modify, and redistribute the code, including in public servers.
- If you modify and run it on a network (e.g., a public server), users must be able to obtain the complete corresponding source of your modified version.
- Any redistributed or network-deployed modifications must remain under the same license (copyleft).
- You must preserve attribution to the original author.

## Name and Attribution
You may not misrepresent authorship. If you distribute modified versions, keep the original project name and author attribution or clearly state your changes while retaining credit to `dox187`. If you use a different plugin name for your fork, you must still credit the original author and keep the license.
