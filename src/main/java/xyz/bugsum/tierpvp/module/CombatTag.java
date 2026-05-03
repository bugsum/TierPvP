package xyz.bugsum.tierpvp.module;

import lombok.Getter;

import java.util.UUID;

public class CombatTag {
    @Getter
    private final UUID taggerId;
    private long combatTagExpiringTime;

    public CombatTag(UUID taggerId, long expiryTime) {
        this.taggerId = taggerId;
        this.combatTagExpiringTime = System.currentTimeMillis() + expiryTime;
    }

    public long getRemainingSeconds() {
        long remaining = combatTagExpiringTime - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= combatTagExpiringTime;
    }

    public void refresh(long durationMillis) {
        this.combatTagExpiringTime = System.currentTimeMillis() + durationMillis;
    }
}
