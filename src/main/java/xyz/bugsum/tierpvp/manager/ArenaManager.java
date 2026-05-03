package xyz.bugsum.tierpvp.manager;

import xyz.bugsum.tierpvp.module.Arena;

import java.util.*;

public class ArenaManager {
    private final Map<String, Arena> arenas = new LinkedHashMap<>();

    public void addArena(Arena arena) {
        arenas.put(arena.getName().toLowerCase(), arena);
    }

    public void removeArena(String name) {
        arenas.remove(name.toLowerCase());
    }

    public Optional<Arena> getArena(String name) {
        return Optional.ofNullable(arenas.get(name.toLowerCase()));
    }

    public Collection<Arena> getAllArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    public List<Arena> getCompleteArenas() {
        return arenas.values().stream().filter(Arena::isComplete).toList();
    }

    public boolean hasArena(String name) {
        return arenas.containsKey(name.toLowerCase());
    }

    public int size() {
        return arenas.size();
    }
}
