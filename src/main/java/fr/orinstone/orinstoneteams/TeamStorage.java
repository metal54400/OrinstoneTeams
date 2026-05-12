package fr.orinstone.orinstoneteams;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class TeamStorage {
    private final OrinstoneTeamsPlugin plugin;
    private final File file;
    private final Map<String, Team> teams = new HashMap<>();

    public TeamStorage(OrinstoneTeamsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "teams.yml");
    }

    public void load() {
        teams.clear();
        if (!file.exists()) return;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yml.getConfigurationSection("teams");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;

            UUID owner = UUID.fromString(s.getString("owner"));
            Team team = new Team(s.getString("name", key), owner);
            team.setBalance(s.getDouble("balance", 0.0));

            for (String raw : s.getStringList("members")) team.getMembers().add(UUID.fromString(raw));
            for (String raw : s.getStringList("admins")) team.getAdmins().add(UUID.fromString(raw));

            ConfigurationSection h = s.getConfigurationSection("home");
            if (h != null && h.getString("world") != null && Bukkit.getWorld(h.getString("world")) != null) {
                team.setHome(new Location(
                        Bukkit.getWorld(h.getString("world")),
                        h.getDouble("x"), h.getDouble("y"), h.getDouble("z"),
                        (float) h.getDouble("yaw"), (float) h.getDouble("pitch")
                ));
            }

            teams.put(key.toLowerCase(Locale.ROOT), team);
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();

        for (Team team : teams.values()) {
            String path = "teams." + team.getName().toLowerCase(Locale.ROOT);
            yml.set(path + ".name", team.getName());
            yml.set(path + ".owner", team.getOwner().toString());
            yml.set(path + ".balance", team.getBalance());
            yml.set(path + ".members", team.getMembers().stream().map(UUID::toString).toList());
            yml.set(path + ".admins", team.getAdmins().stream().map(UUID::toString).toList());

            Location home = team.getHome();
            if (home != null && home.getWorld() != null) {
                yml.set(path + ".home.world", home.getWorld().getName());
                yml.set(path + ".home.x", home.getX());
                yml.set(path + ".home.y", home.getY());
                yml.set(path + ".home.z", home.getZ());
                yml.set(path + ".home.yaw", home.getYaw());
                yml.set(path + ".home.pitch", home.getPitch());
            }
        }

        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder teams.yml: " + e.getMessage());
        }
    }

    public Collection<Team> getTeams() { return teams.values(); }
    public Team getTeam(String name) { return name == null ? null : teams.get(name.toLowerCase(Locale.ROOT)); }

    public Team getPlayerTeam(UUID uuid) {
        for (Team team : teams.values()) if (team.getMembers().contains(uuid)) return team;
        return null;
    }

    public boolean exists(String name) { return getTeam(name) != null; }
    public void addTeam(Team team) {
        teams.put(team.getName().toLowerCase(Locale.ROOT), team);
        save();
    }
    public void removeTeam(Team team) {
        teams.remove(team.getName().toLowerCase(Locale.ROOT));
        save();
    }
}
