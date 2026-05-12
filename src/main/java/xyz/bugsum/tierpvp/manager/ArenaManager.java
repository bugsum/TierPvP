package xyz.bugsum.tierpvp.manager;

import lombok.Getter;
import xyz.bugsum.tierpvp.module.Arena;

import java.util.*;

public class ArenaManager {
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final LinkedList<Arena> arenaQueue = new LinkedList<>();
    @Getter
    private Arena lastPlayedArena;

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

    public void rebuild(List<Arena> arenas) {
        arenaQueue.clear();
        if (arenas.isEmpty()) return;

        List<Arena> shuffled = new ArrayList<>(arenas);
        Collections.shuffle(shuffled);

        if (lastPlayedArena != null && shuffled.size() > 1 && shuffled.getFirst().getName().equals(lastPlayedArena.getName())) {
            Arena first = shuffled.removeFirst();
            shuffled.add(first);
        }

        arenaQueue.addAll(shuffled);
    }

    public Arena next(List<Arena> allArenas) {
        if (arenaQueue.isEmpty()) {
            rebuild(allArenas);
        }
        if (arenaQueue.isEmpty()) return null;

        Arena arena = arenaQueue.poll();
        lastPlayedArena = arena;
        return arena;
    }

    public boolean isArenaQueueEmpty() {
        return arenaQueue.isEmpty();
    }
}
