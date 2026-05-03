package xyz.bugsum.tierpvp.manager;

import xyz.bugsum.tierpvp.module.CombatTag;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatManager {
    private final Map<UUID, CombatTag> taggedPlayers = new ConcurrentHashMap<>();
    private long duration;
    
    public CombatManager(int seconds) {
        this.duration = seconds * 1000L;
    }

    public void tag(UUID player, UUID tagger) {
        CombatTag combat = taggedPlayers.get(player);

        if (combat != null && !combat.isExpired() && combat.getTaggerId().equals(tagger)) {
            combat.refresh(duration);
        } else {
            taggedPlayers.put(player, new CombatTag(tagger, duration));
        }
    }

    public void tagBothPlayers(UUID player_a, UUID player_b) {
        tag(player_a, player_b);
        tag(player_b, player_a);
    }

    public boolean isTagged(UUID player) {
        CombatTag tag = taggedPlayers.get(player);

        if (tag == null) return false;

        if (tag.isExpired()) {
            taggedPlayers.remove(player);
            return false;
        }

        return false;
    }

    public Optional<UUID> getTagger(UUID player) {
        CombatTag tag = taggedPlayers.get(player);
        if (tag == null || tag.isExpired()) return Optional.empty();
        return Optional.of(tag.getTaggerId());
    }

    public long getRemainingSeconds(UUID player) {
        CombatTag tag = taggedPlayers.get(player);
        if (tag == null || tag.isExpired()) return 0;
        return tag.getRemainingSeconds();
    }

    public void clearTag(UUID player) {
        taggedPlayers.remove(player);
    }

    public void clearAll() {
        taggedPlayers.clear();
    }

    public void setDuration(int seconds) {
        this.duration = seconds * 1000L;
    }
}
