package io.github.syst3ms.skriptparser.effects;

import io.github.syst3ms.skriptparser.Parser;
import io.github.syst3ms.skriptparser.lang.Effect;
import io.github.syst3ms.skriptparser.lang.Expression;
import io.github.syst3ms.skriptparser.lang.TriggerContext;
import io.github.syst3ms.skriptparser.parsing.ParseContext;
import org.jetbrains.annotations.Nullable;

/**
 * Shuts down the whole current sessions.
 * This means that any code under any circumstances will not be run after this effect is triggered.
 * Note that you should use this effect very cautiously.
 *
 * @name Shutdown
 * @pattern shut[ ]down [[the] current session]
 * @since ALPHA
 * @author Mwexim
 */
public class EffShutdown extends Effect {
    private Expression<String> string;

    static {
        Parser.getMainRegistration().addEffect(
            EffShutdown.class,
            "shut[ ]down[ [the] current session]" // TODO fix issue with nested optional groups
        );
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, ParseContext parseContext) {
        return true;
    }

    @Override
    public void execute(TriggerContext ctx) {
        System.exit(0);
    }

    @Override
    public String toString(@Nullable TriggerContext ctx, boolean debug) {
        return "shutdown";
    }
}
