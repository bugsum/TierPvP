package xyz.bugsum.tierpvp.util;

import java.util.List;
import java.util.Map;

public record Tier(int index, String displayName, TierItem weapon, List<TierItem> extras, Map<String, TierItem> armor) {
    public Tier {
        if (extras == null) extras = List.of();
        if (armor == null) armor = Map.of();
    }
}
