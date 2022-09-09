package net.openid.conformance;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.WaitForConfiguredSeconds;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalServerTestModule;
import net.openid.conformance.frontchannel.BrowserControl;
import net.openid.conformance.info.ImageService;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.runner.TestExecutionManager;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.PublishTestModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

@PublishTestModule(
	//Even though this class is not a test module per se, this annotation is necessary to ensure the logging. Will be changed
	//once the upstream project is refactored.
    testName = "ConditionSequenceRepeater",
	displayName = "",
	summary = "",
	profile = ""
)
public class ConditionSequenceRepeater extends AbstractFAPI1AdvancedFinalServerTestModule {

	private final Logger logger = LoggerFactory.getLogger(ConditionSequenceRepeater.class);

	private final Supplier<ConditionSequence> sequenceSupplier;
	private final String TIMEOUT_COUNTER_KEY = String.format("LOOPING_COUNTER_%s", getId());
	private int timeout = 5;

	private Optional<Class<? extends Condition>> onTimeoutCondtion = Optional.empty();

	private Optional<Class<? extends Condition>> preSequencePause = Optional.empty();
	private Optional<Class<? extends Condition>> postSequencePause = Optional.of(WaitForConfiguredSeconds.class);
	private Optional<ConditionSequence> onTimeoutCondtionSequence = Optional.empty();
	private Optional<ConditionSequence> onRefreshSequence = Optional.empty();

	private Predicate<Environment> timeoutPredicate = (e) -> {
		int ttl = e.getInteger(TIMEOUT_COUNTER_KEY);
		logger.info("TTL on repeating condition sequence now {}", ttl);
		return !(ttl < timeout);
	};

	private Predicate<Environment> endPredicate = timeoutPredicate;

	public ConditionSequenceRepeater(Environment env, String id,TestInstanceEventLog eventLog,TestInfoService testInfo,
									 TestExecutionManager executionManager,
									 Supplier<ConditionSequence> conditionSequenceSupplier) {

		super.setProperties(id, null, eventLog , null, testInfo, executionManager, null);

		env.putInteger("loopSequencePauseTime", 0);
		env.putInteger("preSequencePauseTime", 0);
		env.putInteger("postSequencePauseTime", 0);
		env.putInteger("refreshIteration", Integer.MAX_VALUE);

		this.env = env;
		this.sequenceSupplier = conditionSequenceSupplier;
	}

	/**
	 * Call after configuration methods. This actually
	 * starts to run the conditionsequence
	 */
	public void run() {
		int ttl = 1;
		do {
			int pause = env.getInteger("preSequencePauseTime");
			env.putInteger("loopSequencePauseTime", pause);
			preSequencePause.ifPresent(c -> callAndStopOnFailure(c));
			env.putInteger(TIMEOUT_COUNTER_KEY, ttl);
			ConditionSequence sequence = sequenceSupplier.get();
			call(exec().startBlock(String.format("Pooling sequence [%s]", ttl)));
			call(sequence);

			int refreshIteration = env.getInteger("refreshIteration");
			if (ttl % refreshIteration == 0) {
				call(exec().startBlock("Pooling Refresh Sequence"));
				onRefreshSequence.ifPresent(s -> call(s));
			}

			ttl++;
			pause = env.getInteger("postSequencePauseTime");
			env.putInteger("loopSequencePauseTime", pause);
			postSequencePause.ifPresent(c -> callAndStopOnFailure(c));
		} while(!endPredicate.test(env));
		if(timeoutPredicate.test(env)) {
			onTimeoutCondtion.ifPresent(c -> callAndStopOnFailure(c));
			onTimeoutCondtionSequence.ifPresent(s -> call(s));
		}
	}

	/**
	 * Overrides the number of times the sequence is repeated if no other condition ends it
	 * @param timeout
	 * @return
	 */
	public ConditionSequenceRepeater times(int timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * Specifies that there should be a *pause* second pause
	 * before each run of the condition sequence
	 * @param pause
	 * @return
	 */
	public ConditionSequenceRepeater leadingPause(int pause) {
		env.putInteger("preSequencePauseTime", pause);
		return this;
	}

	/**
	 * Specifies that there should be a *pause* second pause
	 * after each run of the condition sequence
	 * @param pause
	 * @return
	 */
	public ConditionSequenceRepeater trailingPause(int pause) {
		env.putInteger("postSequencePauseTime", pause);
		return this;
	}

	/**
	 * A
	 * @param refreshIteration is the number of iterations for the condition to be executed
	 * @param conditionSequence to be executed every specific number of iterations
	 * @return
	 */
	public ConditionSequenceRepeater refreshSequence(ConditionSequence conditionSequence, int refreshIteration) {
		env.putInteger("refreshIteration", refreshIteration);
		this.onRefreshSequence = Optional.ofNullable(conditionSequence);
		return this;
	}

	/**
	 * We can run the sequence repeatedly until a given boolean value in
	 * the Environment is changed to false. Typically by a condition within the sequence
	 *
	 * @param conditionKey - the name of the boolean value
	 * @return
	 */
	public ConditionSequenceRepeater untilFalse(String conditionKey) {
		env.putBoolean(conditionKey, true);
		return until((e) -> !e.getBoolean(conditionKey));
	}

	/**
	 * We can run the sequence repeatedly until a given boolean value in
	 * the Environment is changed to true. Typically by a condition within the sequence
	 *
	 * @param conditionKey - the name of the boolean value
	 * @return
	 */
	public ConditionSequenceRepeater untilTrue(String conditionKey) {
		env.putBoolean(conditionKey, false);
		return until((e) -> e.getBoolean(conditionKey));
	}

	/**
	 * We can run the sequence repeatedly until a given string value in
	 * the Environment is changed to the given value. Typically by a condition within the sequence
	 *
	 * @param key - the name of the string value
	 * @param value - the expected value of the string which breaks us out of the loop
	 * @return
	 */
	public ConditionSequenceRepeater untilEqual(String key, String value) {
		return until((e) -> env.getString(key).equals(value));
	}

	/**
	 * We can run the sequence repeatedly until a given int value in
	 * the Environment is changed to the given value. Typically by a condition within the sequence
	 *
	 * @param key - the name of the string value
	 * @param value - the expected value of the int which breaks us out of the loop
	 * @return
	 */
	public ConditionSequenceRepeater untilEqual(String key, int value) {
		return until((e) -> env.getInteger(key) == value);
	}

	/**
	 * Generic method for adding additional predicates which may not be otherwise
	 * covered
	 * @param until - a Predicate<Environment> which, when evaluated to true, ends the loop
	 * @return
	 */
	public ConditionSequenceRepeater until(Predicate<Environment> until) {
		endPredicate = endPredicate.or(until);
		return this;
	}

	/**
	 * A
	 * @param conditionClass to be executed if the loop times out
	 * @return
	 */
	public ConditionSequenceRepeater onTimeout(Class<? extends Condition> conditionClass) {
		this.onTimeoutCondtion = Optional.ofNullable(conditionClass);
		return this;
	}

	/**
	 * A
	 * @param endSequence to be executed if the loop times out
	 * @return
	 */
	public ConditionSequenceRepeater onTimeout(ConditionSequence endSequence) {
		this.onTimeoutCondtionSequence = Optional.ofNullable(endSequence);
		return this;
	}
}
