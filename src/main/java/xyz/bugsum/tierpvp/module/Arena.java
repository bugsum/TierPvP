package xyz.bugsum.tierpvp.module;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import xyz.bugsum.tierpvp.util.ArenaSpawn;

@Getter
public class Arena {
    private final String name;
    @Setter
    private Location spawn;
    @Setter
    private ArenaSpawn arenaSpawn;
    @Setter
    private int arenaVoid;

    public Arena(String name) {
        this.name = name;
        this.arenaVoid = 0;
    }

    public Arena(String name, Location spawn, ArenaSpawn arenaSpawn, int arenaVoid) {
        this.name = name;
        this.spawn = spawn;
        this.arenaSpawn = arenaSpawn;
        this.arenaVoid = arenaVoid;
    }

    public boolean isComplete() {
        return spawn != null && arenaSpawn != null;
    }

    public boolean isInSpawnArea(Location loc) {
        return arenaSpawn != null && arenaSpawn.contains(loc);
    }
}
