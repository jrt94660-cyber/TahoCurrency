package com.taho.currency;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class TahoCurrencyExpansion extends PlaceholderExpansion {
    private final TahoCurrency plugin;

    public TahoCurrencyExpansion(TahoCurrency plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "tahocurrency";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Taho";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "0";
        if (params.equalsIgnoreCase("short")) {
            return formatShort(plugin.getBalance(player.getUniqueId()));
        }
        return null;
    }

    private String formatShort(long balance) {
        if (balance < 1_000L) return Long.toString(Math.max(0L, balance));

        String[] suffixes = {"K", "M", "B", "T", "Qa", "Qi"};
        BigDecimal value = BigDecimal.valueOf(balance);
        BigDecimal thousand = BigDecimal.valueOf(1_000L);
        int suffixIndex = -1;

        do {
            value = value.divide(thousand, 8, RoundingMode.HALF_UP);
            suffixIndex++;
        } while (value.compareTo(thousand) >= 0 && suffixIndex < suffixes.length - 1);

        value = value.setScale(2, RoundingMode.HALF_UP);
        if (value.compareTo(thousand) >= 0 && suffixIndex < suffixes.length - 1) {
            value = value.divide(thousand, 8, RoundingMode.HALF_UP);
            suffixIndex++;
        }

        DecimalFormat format = new DecimalFormat("0.##",
            DecimalFormatSymbols.getInstance(Locale.US));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(value) + suffixes[suffixIndex];
    }
}
