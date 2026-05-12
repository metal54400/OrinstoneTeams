package fr.orinstone.orinstoneteams;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public final class MessageManager {
    private final OrinstoneTeamsPlugin plugin;
    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.##");

    public MessageManager(OrinstoneTeamsPlugin plugin) {
        this.plugin = plugin;
    }

    public String rawPrefix() {
        String prefix = plugin.getConfig().getString("prefix", "&8[&bOrinstoneTeams&8]&r ");
        if (plugin.getConfig().getBoolean("use-smallcaps-prefix", false)) prefix = SmallCaps.convert(prefix);
        return prefix;
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        Object obj = plugin.getConfig().get("messages." + key);
        if (obj instanceof List<?>) {
            for (String line : plugin.getConfig().getStringList("messages." + key)) sendLine(sender, line, placeholders);
        } else {
            sendLine(sender, plugin.getConfig().getString("messages." + key, key), placeholders);
        }
    }

    private void sendLine(CommandSender sender, String line, Map<String, String> placeholders) {
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(rawPrefix() + apply(line, placeholders)));
    }

    public String apply(String text, Map<String, String> placeholders) {
        String out = text == null ? "" : text;
        for (Map.Entry<String, String> e : placeholders.entrySet()) out = out.replace("{" + e.getKey() + "}", e.getValue());
        return out;
    }

    public String teamDisplay(Team team) {
        return plugin.getConfig().getString("team-name-placeholder-color", "&7") + team.getName();
    }

    public String money(double amount) {
        return moneyFormat.format(amount);
    }
}
