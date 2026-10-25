package com.taho.currency;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

final class CoinsStore {
    private final File file;
    private final YamlConfiguration data;

    CoinsStore(File file) {
        this.file = file;
        if (file.getParentFile() != null) file.getParentFile().mkdirs();
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    synchronized Map<UUID, Long> getBalances() {
        Map<UUID, Long> balances = new HashMap<>();
        if (data.isConfigurationSection("balances")) {
            for (String key : data.getConfigurationSection("balances").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    long amount = Math.max(0, data.getLong("balances." + key, 0));
                    if (amount > 0) balances.put(uuid, amount);
                } catch (IllegalArgumentException ignored) { }
            }
        }
        return balances;
    }

    synchronized long get(UUID uuid) {
        return Math.max(0, data.getLong("balances." + uuid, 0));
    }

    synchronized void add(UUID uuid, long amount) {
        if (amount <= 0) return;
        data.set("balances." + uuid, get(uuid) + amount);
        save();
    }

    synchronized boolean withdraw(UUID uuid, long amount) {
        long current = get(uuid);
        if (amount < 0 || current < amount) return false;
        data.set("balances." + uuid, current - amount);
        save();
        return true;
    }

    synchronized void save() {
        try { data.save(file); }
        catch (IOException ex) { throw new IllegalStateException("Could not save currency balances", ex); }
    }
}