package fr.orinstone.orinstoneteams;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public final class TeamCommand implements CommandExecutor, TabCompleter {
    private final OrinstoneTeamsPlugin plugin;
    private final Map<UUID, String> invitations = new HashMap<>();

    public TeamCommand(OrinstoneTeamsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("orinstoneteams.use")) {
            plugin.msg().send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            plugin.msg().send(sender, "help");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(sender, args);
            case "info" -> info(sender, args);
            case "baltop" -> baltop(sender);
            case "sethome" -> sethome(sender);
            case "home" -> home(sender);
            case "list" -> list(sender);
            case "join" -> join(sender, args);
            case "invite" -> invite(sender, args);
            case "disband" -> disband(sender);
            case "leave" -> leave(sender);
            case "bal" -> bal(sender);
            case "deposit" -> deposit(sender, args);
            case "withdraw", "weraws" -> withdraw(sender, args);
            case "setowner" -> setowner(sender, args);
            case "setadmin" -> setadmin(sender, args);
            default -> plugin.msg().send(sender, "help");
        }
        return true;
    }

    private Player player(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            plugin.msg().send(sender, "only-player");
            return null;
        }
        return p;
    }

    private void create(CommandSender sender, String[] args) {
        Player p = player(sender);
        if (p == null) return;
        if (args.length < 2) { plugin.msg().send(sender, "help"); return; }
        if (plugin.getStorage().getPlayerTeam(p.getUniqueId()) != null) { plugin.msg().send(sender, "already-in-team"); return; }

        String name = args[1];
        int min = plugin.getConfig().getInt("settings.min-team-name-length", 3);
        int max = plugin.getConfig().getInt("settings.max-team-name-length", 16);
        if (name.length() < min || name.length() > max || !name.matches("[A-Za-z0-9_-]+")) {
            plugin.msg().send(sender, "invalid-team-name", Map.of("min", String.valueOf(min), "max", String.valueOf(max)));
            return;
        }
        if (plugin.getStorage().exists(name)) { plugin.msg().send(sender, "team-exists"); return; }

        double cost = plugin.getConfig().getDouble("settings.create-cost", 0.0);
        if (cost > 0) {
            if (!plugin.hasEconomy()) { plugin.msg().send(sender, "vault-missing"); return; }
            Economy eco = plugin.getEconomy();
            if (!eco.has(p, cost)) {
                plugin.msg().send(sender, "not-enough-money", Map.of("amount", plugin.msg().money(cost)));
                return;
            }
            eco.withdrawPlayer(p, cost);
        }

        Team team = new Team(name, p.getUniqueId());
        plugin.getStorage().addTeam(team);
        plugin.msg().send(sender, "team-created", placeholders(team));
    }

    private void info(CommandSender sender, String[] args) {
        Team team;
        if (args.length >= 2) {
            team = plugin.getStorage().getTeam(args[1]);
        } else {
            Player p = player(sender);
            if (p == null) return;
            team = plugin.getStorage().getPlayerTeam(p.getUniqueId());
        }

        if (team == null) { plugin.msg().send(sender, "team-not-found"); return; }

        Map<String, String> ph = placeholders(team);
        ph.put("owner", name(team.getOwner()));
        ph.put("admins", team.getAdmins().isEmpty() ? "Aucun" : team.getAdmins().stream().map(this::name).collect(Collectors.joining(", ")));
        ph.put("members", team.getMembers().stream().map(this::name).collect(Collectors.joining(", ")));
        ph.put("balance", plugin.msg().money(team.getBalance()));
        plugin.msg().send(sender, "info", ph);
    }

    private void baltop(CommandSender sender) {
        plugin.msg().send(sender, "baltop-header");
        List<Team> teams = plugin.getStorage().getTeams().stream()
                .sorted(Comparator.comparingDouble(Team::getBalance).reversed())
                .limit(10)
                .toList();
        int rank = 1;
        for (Team team : teams) {
            plugin.msg().send(sender, "baltop-line", Map.of(
                    "rank", String.valueOf(rank++),
                    "team", plugin.msg().teamDisplay(team),
                    "balance", plugin.msg().money(team.getBalance())
            ));
        }
    }

    private void sethome(CommandSender sender) {
        Player p = player(sender);
        if (p == null) return;
        Team team = plugin.getStorage().getPlayerTeam(p.getUniqueId());
        if (team == null) { plugin.msg().send(sender, "not-in-team"); return; }
        if (!team.isAdmin(p.getUniqueId())) { plugin.msg().send(sender, "not-admin"); return; }
        team.setHome(p.getLocation());
        plugin.getStorage().save();
        plugin.msg().send(sender, "sethome");
    }

    private void home(CommandSender sender) {
        Player p = player(sender);
        if (p == null) return;
        Team team = plugin.getStorage().getPlayerTeam(p.getUniqueId());
        if (team == null) { plugin.msg().send(sender, "not-in-team"); return; }
        if (team.getHome() == null) { plugin.msg().send(sender, "no-home"); return; }
        p.teleportAsync(team.getHome());
        plugin.msg().send(sender, "teleported-home", placeholders(team));
    }

    private void list(CommandSender sender) {
        Collection<Team> teams = plugin.getStorage().getTeams();
        if (teams.isEmpty()) { plugin.msg().send(sender, "list-empty"); return; }
        for (Team team : teams) {
            plugin.msg().send(sender, "list-format", Map.of(
                    "team", plugin.msg().teamDisplay(team),
                    "members", String.valueOf(team.getMembers().size())
            ));
        }
    }


    private void invite(CommandSender sender, String[] args) {
        Player p = player(sender);
        if (p == null) return;

        if (args.length < 2) {
            plugin.msg().send(sender, "help");
            return;
        }

        Team team = plugin.getStorage().getPlayerTeam(p.getUniqueId());
        if (team == null) {
            plugin.msg().send(sender, "not-in-team");
            return;
        }

        if (!team.isAdmin(p.getUniqueId())) {
            plugin.msg().send(sender, "not-admin");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.msg().send(sender, "player-not-online");
            return;
        }

        if (target.getUniqueId().equals(p.getUniqueId())) {
            plugin.msg().send(sender, "cant-invite-yourself");
            return;
        }

        if (plugin.getStorage().getPlayerTeam(target.getUniqueId()) != null) {
            plugin.msg().send(sender, "target-already-in-team");
            return;
        }

        invitations.put(target.getUniqueId(), team.getName().toLowerCase(Locale.ROOT));

        Map<String, String> ph = placeholders(team);
        ph.put("player", target.getName());

        plugin.msg().send(sender, "invite-sent", ph);
        plugin.msg().send(target, "invite-received", ph);
    }

    private void join(CommandSender sender, String[] args) {
        Player p = player(sender);
        if (p == null) return;
        if (args.length < 2) { plugin.msg().send(sender, "help"); return; }
        if (plugin.getStorage().getPlayerTeam(p.getUniqueId()) != null) { plugin.msg().send(sender, "already-in-team"); return; }

        Team team = plugin.getStorage().getTeam(args[1]);
        if (team == null) { plugin.msg().send(sender, "team-not-found"); return; }

        if (plugin.getConfig().getBoolean("settings.join-requires-invite", false)) {
            String invitedTeam = invitations.get(p.getUniqueId());
            if (invitedTeam == null || !invitedTeam.equalsIgnoreCase(team.getName())) {
                plugin.msg().send(sender, "no-invite");
                return;
            }
        }

        team.getMembers().add(p.getUniqueId());
        invitations.remove(p.getUniqueId());
        plugin.getStorage().save();
        plugin.msg().send(sender, "joined-team", placeholders(team));
    }

    private void disband(CommandSender sender) {
        Player p = player(sender);
        if (p == null) return;
        Team team = plugin.getStorage().getPlayerTeam(p.getUniqueId());
        if (team == null) { plugin.msg().send(sender, "not-in-team"); return; }
        if (!team.isOwner(p.getUniqueId())) { plugin.msg().send(sender, "not-owner"); return; }

        plugin.getStorage().removeTeam(team);
        plugin.msg().send(sender, "disbanded", placeholders(team));
    }

    private void leave(CommandSender sender) {
        Player p = player(sender);
        if (p == null) return;
        Team team = plugin.getStorage().getPlayerTeam(p.getUniqueId());
        if (team == null) { plugin.msg().send(sender, "not-in-team"); return; }
        if (team.isOwner(p.getUniqueId())) { plugin.msg().send(sender, "owner-cant-leave"); return; }

        team.getMembers().remove(p.getUniqueId());
        team.getAdmins().remove(p.getUniqueId());
        plugin.getStorage().save();
        plugin.msg().send(sender, "left-team", placeholders(team));
    }

    private void bal(CommandSender sender) {
        Player p = player(sender);
        if (p == null) return;
        Team team = plugin.getStorage().getPlayerTeam(p.getUniqueId());
        if (team == null) { plugin.msg().send(sender, "not-in-team"); return; }

        plugin.msg().send(sender, "balance", Map.of(
                "team", plugin.msg().teamDisplay(team),
                "balance", plugin.msg().money(team.getBalance())
        ));
    }


    private void deposit(CommandSender sender, String[] args) {
        Player p = player(sender);
        if (p == null) return;

        if (args.length < 2) {
            plugin.msg().send(sender, "money-invalid-amount");
            return;
        }

        Team team = plugin.getStorage().getPlayerTeam(p.getUniqueId());
        if (team == null) {
            plugin.msg().send(sender, "not-in-team");
            return;
        }

        if (!plugin.hasEconomy()) {
            plugin.msg().send(sender, "vault-missing");
            return;
        }

        double amount = parseAmount(args[1]);
        if (amount <= 0) {
            plugin.msg().send(sender, "money-invalid-amount");
            return;
        }

        Economy eco = plugin.getEconomy();
        if (!eco.has(p, amount)) {
            plugin.msg().send(sender, "not-enough-money", Map.of("amount", plugin.msg().money(amount)));
            return;
        }

        eco.withdrawPlayer(p, amount);
        team.setBalance(team.getBalance() + amount);
        plugin.getStorage().save();

        Map<String, String> ph = placeholders(team);
        ph.put("amount", plugin.msg().money(amount));
        ph.put("balance", plugin.msg().money(team.getBalance()));
        plugin.msg().send(sender, "deposit-success", ph);
    }

    private void withdraw(CommandSender sender, String[] args) {
        Player p = player(sender);
        if (p == null) return;

        if (args.length < 2) {
            plugin.msg().send(sender, "money-invalid-amount");
            return;
        }

        Team team = plugin.getStorage().getPlayerTeam(p.getUniqueId());
        if (team == null) {
            plugin.msg().send(sender, "not-in-team");
            return;
        }

        if (!team.isAdmin(p.getUniqueId())) {
            plugin.msg().send(sender, "not-admin");
            return;
        }

        if (!plugin.hasEconomy()) {
            plugin.msg().send(sender, "vault-missing");
            return;
        }

        double amount = parseAmount(args[1]);
        if (amount <= 0) {
            plugin.msg().send(sender, "money-invalid-amount");
            return;
        }

        if (team.getBalance() < amount) {
            plugin.msg().send(sender, "team-not-enough-money", Map.of("amount", plugin.msg().money(amount)));
            return;
        }

        Economy eco = plugin.getEconomy();
        team.setBalance(team.getBalance() - amount);
        eco.depositPlayer(p, amount);
        plugin.getStorage().save();

        Map<String, String> ph = placeholders(team);
        ph.put("amount", plugin.msg().money(amount));
        ph.put("balance", plugin.msg().money(team.getBalance()));
        plugin.msg().send(sender, "withdraw-success", ph);
    }

    private double parseAmount(String raw) {
        try {
            return Double.parseDouble(raw.replace(",", "."));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void setowner(CommandSender sender, String[] args) {
        Player p = player(sender);
        if (p == null) return;
        if (args.length < 2) { plugin.msg().send(sender, "help"); return; }

        Team team = plugin.getStorage().getPlayerTeam(p.getUniqueId());
        if (team == null) { plugin.msg().send(sender, "not-in-team"); return; }
        if (!team.isOwner(p.getUniqueId())) { plugin.msg().send(sender, "not-owner"); return; }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) { plugin.msg().send(sender, "player-not-found"); return; }
        if (!team.getMembers().contains(target.getUniqueId())) { plugin.msg().send(sender, "player-not-found"); return; }

        team.setOwner(target.getUniqueId());
        plugin.getStorage().save();

        Map<String, String> ph = placeholders(team);
        ph.put("player", target.getName() == null ? args[1] : target.getName());
        plugin.msg().send(sender, "owner-set", ph);
    }

    private void setadmin(CommandSender sender, String[] args) {
        Player p = player(sender);
        if (p == null) return;
        if (args.length < 2) { plugin.msg().send(sender, "help"); return; }

        Team team = plugin.getStorage().getPlayerTeam(p.getUniqueId());
        if (team == null) { plugin.msg().send(sender, "not-in-team"); return; }
        if (!team.isOwner(p.getUniqueId())) { plugin.msg().send(sender, "not-owner"); return; }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) { plugin.msg().send(sender, "player-not-found"); return; }
        if (!team.getMembers().contains(target.getUniqueId()) || team.isOwner(target.getUniqueId())) {
            plugin.msg().send(sender, "player-not-found");
            return;
        }

        Map<String, String> ph = placeholders(team);
        ph.put("player", target.getName() == null ? args[1] : target.getName());

        if (team.getAdmins().contains(target.getUniqueId())) {
            team.getAdmins().remove(target.getUniqueId());
            plugin.msg().send(sender, "admin-removed", ph);
        } else {
            team.getAdmins().add(target.getUniqueId());
            plugin.msg().send(sender, "admin-added", ph);
        }

        plugin.getStorage().save();
    }

    private Map<String, String> placeholders(Team team) {
        Map<String, String> map = new HashMap<>();
        map.put("team", plugin.msg().teamDisplay(team));
        map.put("team_raw", team.getName());
        return map;
    }

    private String name(UUID uuid) {
        OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
        return p.getName() == null ? uuid.toString().substring(0, 8) : p.getName();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filter(List.of("create", "info", "baltop", "sethome", "home", "list", "join", "invite", "disband", "leave", "bal", "deposit", "withdraw", "weraws", "setowner", "setadmin"), args[0]);
        }

        if (args.length == 2 && List.of("join", "info").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(plugin.getStorage().getTeams().stream().map(Team::getName).toList(), args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }

        if (args.length == 2 && List.of("setowner", "setadmin").contains(args[0].toLowerCase(Locale.ROOT)) && sender instanceof Player p) {
            Team team = plugin.getStorage().getPlayerTeam(p.getUniqueId());
            if (team != null) return filter(team.getMembers().stream().map(this::name).toList(), args[1]);
        }

        return List.of();
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return values.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
