package fr.orinstone.orinstoneteams;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class OrinstoneTeamsExpansion extends PlaceholderExpansion {
    private final OrinstoneTeamsPlugin plugin;

    public OrinstoneTeamsExpansion(OrinstoneTeamsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "orinstoneteams"; }
    @Override public @NotNull String getAuthor() { return "Orinstone"; }
    @Override public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        Team team = plugin.getStorage().getPlayerTeam(player.getUniqueId());

        if (params.equalsIgnoreCase("team")) {
            return team == null ? "" : plugin.getConfig().getString("team-name-placeholder-color", "&7") + team.getName();
        }
        if (params.equalsIgnoreCase("team_raw")) return team == null ? "" : team.getName();
        if (params.equalsIgnoreCase("team_smallcaps")) {
            return team == null ? "" : plugin.getConfig().getString("team-name-placeholder-color", "&7") + SmallCaps.convert(team.getName());
        }

        return null;
    }
}
