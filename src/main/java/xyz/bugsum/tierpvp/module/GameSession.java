package xyz.bugsum.tierpvp.module;

import lombok.Getter;
import lombok.Setter;
import xyz.bugsum.tierpvp.util.GameState;

@Setter
@Getter
public class GameSession {
    private Arena arena;
    private GameState state;

    public GameSession(Arena arena) {
        this.arena = arena;
        this.state = GameState.ACTIVE;
    }

    public boolean isActive() {
        return state == GameState.ACTIVE;
    }

    public boolean isCelebrating() {
        return state == GameState.CELEBRATING;
    }
}
