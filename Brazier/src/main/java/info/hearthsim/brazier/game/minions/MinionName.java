package info.hearthsim.brazier.game.minions;

import info.hearthsim.brazier.game.EntityName;
import org.jtrim.utils.ExceptionHelper;

import java.util.Objects;

/**
 * The id of a minion, which is essentially the name of the minion.
 */
public final class MinionName implements EntityName {
    private final String name;

    public MinionName(String name) {
        ExceptionHelper.checkNotNullArgument(name, "name");
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final MinionName other = (MinionName)obj;
        return Objects.equals(this.name, other.name);
    }

    @Override
    public String toString() {
        return name;
    }
}
