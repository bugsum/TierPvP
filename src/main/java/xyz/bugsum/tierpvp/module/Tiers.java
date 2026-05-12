package xyz.bugsum.tierpvp.module;

import xyz.bugsum.tierpvp.util.Tier;

import java.util.List;
import java.util.Optional;

public class Tiers {
    private final List<Tier> tiers;

    public Tiers(List<Tier> tiers) {
        this.tiers = tiers;
    }

    public Optional<Tier> getLevel(int index) {
        return tiers.stream().filter(l -> l.index() == index).findFirst();
    }

    public Optional<Tier> next(int currentIndex) {
        return getLevel(currentIndex + 1);
    }

    public Optional<Tier> previous(int currentIndex) {
        return getLevel(currentIndex - 1);
    }

    public boolean isFinal(int index) {
        return index == getMaxLevel();
    }

    public int getMinLevel() {
        return tiers.stream().mapToInt(Tier::index).min().orElse(1);
    }

    public int getMaxLevel() {
        return tiers.stream().mapToInt(Tier::index).max().orElse(1);
    }

    public int size() {
        return tiers.size();
    }

    public List<Tier> getAll() {
        return tiers;
    }
}
