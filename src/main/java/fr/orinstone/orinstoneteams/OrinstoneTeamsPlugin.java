package fr.orinstone.orinstoneteams;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class OrinstoneTeamsPlugin extends JavaPlugin {
    private static OrinstoneTeamsPlugin instance;
    private Economy economy;
    private TeamStorage storage;
    private MessageManager messages;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        messages = new MessageManager(this);
        storage = new TeamStorage(this);
        storage.load();

        setupVault();

        TeamCommand command = new TeamCommand(this);
        getCommand("team").setExecutor(command);
        getCommand("team").setTabCompleter(command);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new OrinstoneTeamsExpansion(this).register();
            getLogger().info("PlaceholderAPI détecté: placeholders activés.");
        }

        getLogger().info("OrinstoneTeams activé.");
    }

    @Override
    public void onDisable() {
        if (storage != null) storage.save();
    }

    private void setupVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault non détecté. Les coûts économie sont désactivés.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("Aucun plugin économie compatible Vault détecté.");
            return;
        }

        economy = rsp.getProvider();
        getLogger().info("Vault économie connecté: " + economy.getName());
    }

    public Economy getEconomy() { return economy; }
    public boolean hasEconomy() { return economy != null; }
    public TeamStorage getStorage() { return storage; }
    public MessageManager msg() { return messages; }
}
