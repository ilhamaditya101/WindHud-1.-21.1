package id.cataclysm.windhud;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public final class WindHUDPlugin extends JavaPlugin implements Listener {
    private final Map<Player, BossBar> bars = new HashMap<>();
    private final Map<Player, Boolean> toggled = new HashMap<>();
    private final Random random = new Random();
    private BukkitTask hudTask;
    private BukkitTask windTask;
    private double windDegrees;
    private double windStrength;

    private static final String[] DIR16 = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
    private static final String[] DIR8 = {"North", "North-East", "East", "South-East", "South", "South-West", "West", "North-West"};

    @Override public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        windDegrees = getConfig().getDouble("wind.initial-direction-degrees", 45.0);
        rollStrength();
        int updateTicks = Math.max(2, getConfig().getInt("hud.update-ticks", 10));
        hudTask = Bukkit.getScheduler().runTaskTimer(this, this::updateHud, 1L, updateTicks);
        long interval = Math.max(20, getConfig().getLong("wind.change-interval-seconds", 180) * 20L);
        windTask = Bukkit.getScheduler().runTaskTimer(this, this::changeWind, interval, interval);
        for (Player p : Bukkit.getOnlinePlayers()) if (getConfig().getBoolean("hud.enabled-by-default", true)) show(p);
        getLogger().info("WindHUD enabled.");
    }


    @Override public void onDisable() {
        if (hudTask != null) hudTask.cancel();
        if (windTask != null) windTask.cancel();
        bars.values().forEach(BossBar::removeAll);
        bars.clear();
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        if (getConfig().getBoolean("hud.enabled-by-default", true)) show(event.getPlayer());
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { cleanup(event.getPlayer()); }
    public void enableFor(Player p) { show(p); }
    public void cleanup(Player p) { hide(p); toggled.remove(p); }

    private void changeWind() {
        windDegrees = random.nextInt(360);
        rollStrength();
    }

    private void rollStrength() {
        double min = getConfig().getDouble("wind.strength-min", 0.25);
        double max = getConfig().getDouble("wind.strength-max", 1.0);
        if (min > max) { double t = min; min = max; max = t; }
        windStrength = Math.max(0, Math.min(1, min + random.nextDouble() * (max - min)));
    }

    private void show(Player p) {
        if (bars.containsKey(p)) return;
        BossBar bar = Bukkit.createBossBar("WindHUD", parseColor(getConfig().getString("hud.color", "BLUE")), BarStyle.SOLID);
        bar.addPlayer(p);
        bar.setVisible(true);
        bars.put(p, bar);
    }

    private void hide(Player p) {
        BossBar bar = bars.remove(p);
        if (bar != null) bar.removePlayer(p);
    }

    private void updateHud() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!toggled.getOrDefault(p, getConfig().getBoolean("hud.enabled-by-default", true))) { hide(p); continue; }
            show(p);
            BossBar bar = bars.get(p);
            String windDir = direction8(windDegrees);
            String compass = compassLine(p.getLocation().getYaw(), windDegrees);
            int pct = (int)Math.round(windStrength * 100);
            String format = getConfig().getString("hud.title-format", "&bWIND &f{direction} &7| &b{compass} &7| &f{strength}%");
            String title = format.replace("{direction}", windDir).replace("{compass}", compass).replace("{strength}", String.valueOf(pct));
            bar.setTitle(color(title));
            bar.setProgress(Math.max(0.0, Math.min(1.0, windStrength)));
        }
    }


    private String compassLine(float yaw, double wind) {
        // Minecraft yaw: 0=south, -90=east, 90=west, 180=north. Convert to compass bearing.
        double bearing = (yaw + 180.0) % 360.0;
        if (bearing < 0) bearing += 360;
        int playerIndex = (int)Math.round(bearing / 22.5) % 16;
        int windIndex = (int)Math.round(wind / 22.5) % 16;
        String[] labels = {"N","NNE","NE","ENE","E","ESE","SE","SSE","S","SSW","SW","WSW","W","WNW","NW","NNW"};
        return labels[windIndex] + "  " + labels[playerIndex] + " ";
    }

    private String direction8(double degrees) { return DIR8[((int)Math.round(degrees / 45.0)) % 8]; }
    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
    private BarColor parseColor(String s) { try { return BarColor.valueOf(s.toUpperCase()); } catch (Exception e) { return BarColor.BLUE; } }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) { reloadConfig(); sender.sendMessage("WindHUD config reloaded."); return true; }
            sender.sendMessage("This command is player-only except reload."); return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("toggle")) {
            boolean next = !toggled.getOrDefault(p, getConfig().getBoolean("hud.enabled-by-default", true));
            toggled.put(p, next);
            if (next) show(p); else hide(p);
            p.sendMessage(color("&bWindHUD &7» &f" + (next ? "Enabled" : "Disabled")));
            return true;
        }
        if (args[0].equalsIgnoreCase("on")) { toggled.put(p, true); show(p); p.sendMessage(color("&bWindHUD enabled.")); return true; }
        if (args[0].equalsIgnoreCase("off")) { toggled.put(p, false); hide(p); p.sendMessage(color("&bWindHUD disabled.")); return true; }
        if (args[0].equalsIgnoreCase("reload") && p.hasPermission("windhud.admin")) { reloadConfig(); p.sendMessage(color("&bWindHUD config reloaded.")); return true; }
        p.sendMessage(color("&bWindHUD &7» &f/windhud [toggle|on|off|reload]"));
        return true;
    }
}
