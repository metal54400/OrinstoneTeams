package fr.orinstone.orinstoneteams;

import org.bukkit.Location;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Team {
    private final String name;
    private UUID owner;
    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> admins = new HashSet<>();
    private double balance;
    private Location home;

    public Team(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
        members.add(owner);
    }

    public String getName() { return name; }
    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) {
        this.owner = owner;
        members.add(owner);
        admins.remove(owner);
    }
    public Set<UUID> getMembers() { return members; }
    public Set<UUID> getAdmins() { return admins; }
    public boolean isOwner(UUID uuid) { return owner.equals(uuid); }
    public boolean isAdmin(UUID uuid) { return admins.contains(uuid) || isOwner(uuid); }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public Location getHome() { return home; }
    public void setHome(Location home) { this.home = home; }
}
