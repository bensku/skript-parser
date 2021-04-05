package io.github.syst3ms.skriptparser.sections;

import io.github.syst3ms.skriptparser.Parser;
import io.github.syst3ms.skriptparser.file.FileSection;
import io.github.syst3ms.skriptparser.lang.*;
import io.github.syst3ms.skriptparser.lang.control.Continuable;
import io.github.syst3ms.skriptparser.lang.control.SelfReferencing;
import io.github.syst3ms.skriptparser.lang.lambda.ArgumentSection;
import io.github.syst3ms.skriptparser.log.ErrorType;
import io.github.syst3ms.skriptparser.log.SkriptLogger;
import io.github.syst3ms.skriptparser.parsing.ParseContext;
import io.github.syst3ms.skriptparser.parsing.ParserState;
import io.github.syst3ms.skriptparser.types.ranges.Ranges;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * A section that iterates over a collection of elements
 */
public class SecLoop extends ArgumentSection implements Continuable, SelfReferencing {
	static {
		Parser.getMainRegistration().addSection(
				SecLoop.class,
				"loop %integer% times",
				"loop %objects%"
		);
	}

	private static final transient Map<SecLoop, Iterator<?>> iterators = new WeakHashMap<>();

	@Nullable
	private Statement actualNext;
	@Nullable
	private Expression<?> expression;
	private Expression<BigInteger> times;
	private boolean isNumericLoop;

	@Override
	public boolean loadSection(FileSection section, ParserState parserState, SkriptLogger logger) {
		super.loadSection(section, parserState, logger);
		super.setNext(this);
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, ParseContext parseContext) {
		isNumericLoop = matchedPattern == 0;
		if (isNumericLoop) {
			times = (Expression<BigInteger>) expressions[0];
			// We can do some certainty checks with Literals.
			if (times instanceof Literal<?>) {
				var t = ((Optional<BigInteger>) ((Literal<BigInteger>) times).getSingle()).orElse(BigInteger.ONE);
				if (t.intValue() <= 0) {
					parseContext.getLogger().error("Cannot loop a negative or zero amount of times", ErrorType.SEMANTIC_ERROR);
					return false;
				} else if (t.intValue() == 1) {
					parseContext.getLogger().error(
							"Cannot loop a single time",
							ErrorType.SEMANTIC_ERROR,
							"Remove this loop, because looping something once can be achieved without a loop-statement"
					);
					return false;
				}
			}
		} else {
			expression = expressions[0];
			if (expression.isSingle()) {
				parseContext.getLogger().error(
						"Cannot loop a single value",
						ErrorType.SEMANTIC_ERROR,
						"Remove this loop, because you clearly don't need to loop a single value"
				);
				return false;
			}
		}
		return true;
	}

	@Override
	public Optional<? extends Statement> walk(TriggerContext ctx) {
		if (isNumericLoop && expression == null) {
			// We just set the looped expression to a range from 1 to the amount of times.
			// This allows the usage of 'loop-number' to get the current iteration
			expression = rangeOf(ctx, times);
		}

		Iterator<?> iter = iterators.get(this);
		if (iter == null) {
			assert expression != null;
			iter = expression instanceof Variable ? ((Variable<?>) expression).variablesIterator(ctx) : expression.iterator(ctx);
			if (iter.hasNext()) {
				iterators.put(this, iter);
			} else {
				iter = null;
			}
		}

		if (iter != null && iter.hasNext()) {
			setArguments(iter.next());
			return start();
		} else {
			if (iter != null)
				iterators.remove(this);
			return Optional.ofNullable(actualNext);
		}
	}

	@Override
	public Statement setNext(@Nullable Statement next) {
		this.actualNext = next;
		return this;
	}

	@Override
	public Optional<? extends Statement> getContinued(TriggerContext ctx) {
		return Optional.of(this);
	}

	@Override
	public Optional<Statement> getActualNext() {
		return Optional.ofNullable(actualNext);
	}

	@Override
	public String toString(TriggerContext ctx, boolean debug) {
		assert expression != null;
		return "loop " + (isNumericLoop ? times.toString(ctx, debug) + " times" : expression.toString(ctx, debug));
	}

	/**
	 * @return the expression whose values this loop is iterating over
	 */
	public Expression<?> getLoopedExpression() {
		if (isNumericLoop && expression == null) {
			expression = rangeOf(TriggerContext.DUMMY, times);
		}
		return expression;
	}

	public static Map<SecLoop, Iterator<?>> getIterators() {
		return iterators;
	}

	private static Expression<BigInteger> rangeOf(TriggerContext ctx, Expression<BigInteger> size) {
		BigInteger[] range = (BigInteger[]) size.getSingle(ctx)
				.filter(t -> t.compareTo(BigInteger.ZERO) > 0)
				.map(t -> Ranges.getRange(BigInteger.class).orElseThrow()
						.getFunction()
						.apply(BigInteger.ONE, t)) // Upper bound is inclusive
				.orElse(new BigInteger[0]);
		return new SimpleLiteral<>(BigInteger.class, range);
	}
}
