package eu.hiddenite;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class RandomRespawnPlugin extends JavaPlugin implements Listener {
    private final Random random = new Random();
    private Location safeLocation = null;

    private String respawnWorld = null;
    private int respawnRadius = 0;
    private boolean enabledOnFirstJoin = false;
    private boolean enabledOnDeath = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        respawnWorld = getConfig().getString("location.world");
        respawnRadius = getConfig().getInt("location.radius");
        enabledOnFirstJoin = getConfig().getBoolean("enabled.on-first-join");
        enabledOnDeath = getConfig().getBoolean("enabled.on-death");

        if (respawnWorld == null || respawnRadius <= 0) {
            getLogger().warning("Invalid configuration, plugin not enabled.");
            return;
        }

        getLogger().info("World: " + respawnWorld + ", radius: " + respawnRadius);

        getServer().getPluginManager().registerEvents(this, this);

        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
                storeSafeLocationInCache();
            }
        }, 200, 200);
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        if (!enabledOnFirstJoin || event.getPlayer().hasPlayedBefore()) {
            return;
        }

        World world = event.getPlayer().getWorld();
        Location respawnLocation = getSafeLocation(world);
        if (respawnLocation != null) {
            event.getPlayer().teleport(respawnLocation);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!enabledOnDeath || event.isBedSpawn()) {
            return;
        }

        World world = event.getRespawnLocation().getWorld();
        Location respawnLocation = getSafeLocation(world);
        if (respawnLocation != null) {
            event.setRespawnLocation(respawnLocation);
        }
    }

    private Location getSafeLocation(World world) {
        final int MAX_RETRIES = 10;

        if (safeLocation != null) {
            Location location = safeLocation;
            safeLocation = null;
            return location;
        }

        for (int tries = 0; tries < MAX_RETRIES; ++tries) {
            Location location = generateSafeLocation(world);
            if (location != null) {
                return location;
            }
        }
        return null;
    }

    private Location generateSafeLocation(World world) {
        int x = random.nextInt(respawnRadius * 2 + 1) - respawnRadius;
        int z = random.nextInt(respawnRadius * 2 + 1) - respawnRadius;

        Block block = world.getHighestBlockAt(x, z);
        Material type = block.getType();

        getLogger().info("Block [" + x + ", " + z + "] is " + type.name() + " (" +  world.getBiome(x, block.getY(), z) + ")");

        if (type == Material.GRASS_BLOCK || type == Material.STONE || type == Material.SAND || type == Material.PODZOL) {
            return block.getLocation().add(0.5, 2.5, 0.5);
        }

        return null;
    }

    private void storeSafeLocationInCache() {
        // Every 10 seconds, try to cache a safe location.
        if (safeLocation != null) {
            return;
        }

        World world = getServer().getWorld(respawnWorld);
        if (world != null) {
            safeLocation = generateSafeLocation(world);
        }
    }
}
