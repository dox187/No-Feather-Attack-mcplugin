/*
 * NoFeatherAttack
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Copyright (c) 2025 dox187
 */
package com.dox187.featherhit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class NoFeatherAttack extends JavaPlugin implements Listener {

    private final Set<UUID> enabledPlayers = new HashSet<>();
    // Config-backed fields
    private Material triggerItem = Material.FEATHER;
    private double minCharge = 0.9d;
    private double horizontal = 0.6d;
    private double minUpward = 0.35d;
    private boolean scaleByCharge = false;
    private boolean clampVelocity = true;
    private double maxVelocity = 1.2d;
    private boolean autoEnableOnEnable = true;
    private boolean autoEnableOnJoin = true;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("NoFeatherAttack enabled!");

        // Ensure config.yml is saved with defaults
        saveDefaultConfig();
        // Load settings from config.yml
        try {
            loadSettings();
        } catch (Throwable t) {
            // Keep safe defaults if config is broken (e.g., TABs in YAML)
            getLogger().severe("Failed to load config.yml, using built-in defaults. Error: " + t.getMessage());
        }

        // Create lang folder and copy default language files
        File langFolder = new File(getDataFolder(), "lang");
        if (!langFolder.exists()) langFolder.mkdirs();

        String[] languages = {"en_us", "hu_hu", "de_de", "fr_fr", "es_es"};
        for (String lang : languages) {
            File f = new File(langFolder, lang + ".yml");
            if (!f.exists()) {
                saveResource("lang/" + lang + ".yml", false);
            }
        }

        // Auto-enable feature for players already online (e.g., on /reload)
        if (autoEnableOnEnable) {
            getServer().getOnlinePlayers().forEach(p -> enabledPlayers.add(p.getUniqueId()));
        }
    }

    @Override
    public void onDisable() {
        enabledPlayers.clear();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!enabledPlayers.contains(player.getUniqueId())) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand != null && hand.getType() == triggerItem) {
            // Cancel actual damage
            event.setCancelled(true);

            // Attack charge: 0.0 .. 1.0 (1.0 = fully charged)
            float charge = player.getAttackCooldown();

            // Apply only if charge is high enough
            if (charge < (float) minCharge) {
                return;
            }

            // Direction and knockback vector
            Vector dir = player.getLocation().getDirection().normalize();
            double strength = horizontal * (scaleByCharge ? charge : 1.0d);
            Vector add = dir.multiply(strength); // horizontal strength
            add.setY(Math.max(add.getY(), minUpward)); // ensure a minimum upward component

            // Optionally clamp final velocity to reduce stacking/launching
            Vector v = event.getEntity().getVelocity().add(add);
            if (clampVelocity) {
                double len = v.length();
                if (len > maxVelocity && len > 0) {
                    v = v.multiply(maxVelocity / len);
                }
            }
            event.getEntity().setVelocity(v);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Auto-enable on join
        if (autoEnableOnJoin) {
            enabledPlayers.add(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Cleanup on quit
        enabledPlayers.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getLangMessage("not_a_player", "en_us"));
            return true;
        }

        UUID id = player.getUniqueId();
        if (enabledPlayers.contains(id)) {
            enabledPlayers.remove(id);
            sendLangMessage(player, "feather_off");
        } else {
            enabledPlayers.add(id);
            sendLangMessage(player, "feather_on");
        }
        return true;
    }

    private void sendLangMessage(Player player, String key) {
        String locale = player.locale().toLanguageTag().toLowerCase(); // client language (e.g. de-de)
        File langFile = resolveLangFile(locale);
        FileConfiguration lang = YamlConfiguration.loadConfiguration(langFile);
        String msg = lang.getString(key, key);
        sendMessage(player, msg);
    }

    private String getLangMessage(String key, String lang) {
        File langFile = resolveLangFile(lang.toLowerCase());
        if (!langFile.exists()) return key;
        FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
        return config.getString(key, key);
    }

    // Send a message to player, translating legacy '&' color codes to a Component
    private void sendMessage(Player player, String s) {
        if (s == null || s.isEmpty()) return;
        Component c = LegacyComponentSerializer.legacyAmpersand().deserialize(s);
        player.sendMessage(c);
    }

    // Resolve a language file path from a full tag (e.g. "de-de") or short key (e.g. "de")
    private File resolveLangFile(String localeOrLang) {
        File data = getDataFolder();
        // Try exact match first (e.g. de-de -> de-de.yml)
        File exact = new File(data, "lang/" + localeOrLang + ".yml");
        if (exact.exists()) return exact;

        // Try language_language (e.g. de -> de_de.yml)
        String lang2 = (localeOrLang.length() >= 2) ? localeOrLang.substring(0, 2) : localeOrLang;
        File doubled = new File(data, "lang/" + lang2 + "_" + lang2 + ".yml");
        if (doubled.exists()) return doubled;

        // Try language_us for English fallback variants (e.g. en -> en_us.yml)
        if ("en".equals(lang2)) {
            File enus = new File(data, "lang/en_us.yml");
            if (enus.exists()) return enus;
        }

        // Default fallback
        return new File(data, "lang/en_us.yml");
    }

    // Load settings from config.yml into fields
    private void loadSettings() {
        FileConfiguration cfg = getConfig();
        // Item
        String itemName = cfg.getString("trigger-item", "FEATHER");
        try {
            triggerItem = Material.valueOf(itemName.toUpperCase());
        } catch (IllegalArgumentException ex) {
            getLogger().warning("Invalid trigger-item '" + itemName + "', falling back to FEATHER");
            triggerItem = Material.FEATHER;
        }

        // Knockback settings
        minCharge = clamp01(cfg.getDouble("knockback.min-charge", 0.9d));
        horizontal = Math.max(0.0d, cfg.getDouble("knockback.horizontal", 0.6d));
        minUpward = cfg.getDouble("knockback.min-upward", 0.35d);
        scaleByCharge = cfg.getBoolean("knockback.scale-by-charge", false);
        clampVelocity = cfg.getBoolean("knockback.clamp-velocity", true);
        maxVelocity = Math.max(0.0d, cfg.getDouble("knockback.max-velocity", 1.2d));

        // Behavior
        autoEnableOnEnable = cfg.getBoolean("behavior.auto-enable-on-enable", true);
        autoEnableOnJoin = cfg.getBoolean("behavior.auto-enable-on-join", true);
    }

    // Clamp a double into [0,1]
    private static double clamp01(double v) {
        if (v < 0.0d) return 0.0d;
        if (v > 1.0d) return 1.0d;
        return v;
    }
}
