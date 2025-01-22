package net.openid.conformance.testmodule;

import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.SleepUntilAuthReqExpires;
import net.openid.conformance.condition.client.WaitFor5Seconds;
import net.openid.conformance.frontchannel.BrowserControl;
import net.openid.conformance.info.ImageService;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.runner.TestExecutionManager;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.SkippedCondition;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class AbstractTestModule implements TestModule, DataUtils {

	private static final Logger logger = LoggerFactory.getLogger(AbstractTestModule.class);

	private String id = null; // unique identifier for the test, set from the outside
	private volatile Status status = Status.NOT_YET_CREATED; // current status of the test
	private Result result = Result.UNKNOWN; // results of running the test

	private Map<Class<? extends Enum<?>>, ? extends Enum<?>> variant;
	private Map<String, String> owner; // Owner of the test (i.e. who created it. Should be subject and issuer from OIDC
	protected TestInstanceEventLog eventLog;
	protected BrowserControl browser;
	protected TestExecutionManager executionManager;
	protected Map<String, String> exposed = new HashMap<>(); // exposes runtime values to outside modules
	protected Environment env = new Environment(); // keeps track of values at runtime
	private Instant created; // time stamp of when this test created
	private Instant statusUpdated; // time stamp of when the status was last updated
	private TestInterruptedException finalError; // final error from running the test
	private boolean cleanupCalled = false;

	protected TestInfoService testInfo;
	protected ImageService imageService;

	private Supplier<String> testNameSupplier = Suppliers.memoize(() -> getClass().getDeclaredAnnotation(PublishTestModule.class).testName());

	protected AbstractTestModule() {

	}

	@Override
	public boolean autoStart() {
		/* automatically start all tests by default */
		return true;
	}

	@Override
	public void setProperties(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo, TestExecutionManager executionManager, ImageService imageService) {
		this.id = id;
		this.owner = owner;
		this.eventLog = eventLog;
		this.browser = browser;
		this.testInfo = testInfo;
		this.executionManager = executionManager;
		this.imageService = imageService;

		this.created = Instant.now();
		this.statusUpdated = created; // this will get changed in a moment but set it here for completeness

		setStatusInternal(Status.CREATED);
	}

	@Override
	public void setVariant(Map<Class<? extends Enum<?>>, ? extends Enum<?>> variant) {
		this.variant = variant;
	}

	public <T extends Enum<T>> T getVariant(Class<T> parameter) {
		Enum<?> value = variant.get(parameter);
		if (value == null) {
			throw new IllegalArgumentException("Invalid variant parameter: " + parameter.getSimpleName());
		} else if (!parameter.isAssignableFrom(value.getClass())) {
			throw new RuntimeException("BUG: invalid value for variant %s: %s".formatted(
				parameter.getSimpleName(),
				value));
		}
		return parameter.cast(value);
	}

	@Override
	public Map<String, String> getOwner() {
		return owner;
	}

	/**
	 * Create and evaluate a Condition in the current environment. Throw a @TestFailureException if the Condition fails.
	 *
	 * onFail is set to FAILURE
	 *
	 */
	protected void callAndStopOnFailure(Condition condition, String... requirements) {
		call(condition(condition)
			.onFail(Condition.ConditionResult.FAILURE)
			.requirements(requirements));
	}

	/**
	 * Create and evaluate a Condition in the current environment. Throw a @TestFailureException if the Condition fails.
	 *
	 * onFail is set to FAILURE
	 *
	 */
	protected void callAndStopOnFailure(Class<? extends Condition> conditionClass, String... requirements) {
		call(condition(conditionClass)
			.onFail(Condition.ConditionResult.FAILURE)
			.requirements(requirements));
	}

	/**
	 * Create and evaluate a Condition in the current environment. Throw a @TestFailureException if the Condition fails.
	 */
	protected void callAndStopOnFailure(Condition condition, Condition.ConditionResult onFail, String... requirements) {
		if (onFail != Condition.ConditionResult.FAILURE) {
			throw new TestFailureException(getId(), "callAndStopOnFailure called with onFail != ConditionResult.FAILURE");
		}
		call(condition(condition)
			.requirements(requirements)
			.onFail(onFail));
	}

	/**
	 * Create and evaluate a Condition in the current environment. Throw a @TestFailureException if the Condition fails.
	 */
	protected void callAndStopOnFailure(Class<? extends Condition> conditionClass, Condition.ConditionResult onFail, String... requirements) {
		if (onFail != Condition.ConditionResult.FAILURE) {
			throw new TestFailureException(getId(), "callAndStopOnFailure called with onFail != ConditionResult.FAILURE");
		}
		call(condition(conditionClass)
			.requirements(requirements)
			.onFail(onFail));
	}

	private void logException(Throwable e) {
		Map<String, Object> event = ex(e);
		event.put("msg", "Caught exception from test framework: " + e.getMessage());

		eventLog.log(getName(), event);
	}

	/**
	 * Create and evaluate a Condition in the current environment. Log but ignore if the Condition fails.
	 */
	protected void callAndContinueOnFailure(Condition condition, Condition.ConditionResult onFail, String... requirements) {
		call(condition(condition)
			.requirements(requirements)
			.onFail(onFail)
			.dontStopOnFailure());
	}

	/**
	 * Create and evaluate a Condition in the current environment. Log but ignore if the Condition fails.
	 */
	protected void callAndContinueOnFailure(Class<? extends Condition> conditionClass, Condition.ConditionResult onFail, String... requirements) {
		call(condition(conditionClass)
			.requirements(requirements)
			.onFail(onFail)
			.dontStopOnFailure());
	}

	/**
	 * Create and evaluate a Condition in the current environment, but only if the environment contains the given
	 * objects and strings (both can be null).
	 *
	 * onFail is set to INFO
	 *
	 * requirements are empty
	 */
	protected void skipIfMissing(String[] required, String[] strings, Condition.ConditionResult onSkip,
		Class<? extends Condition> conditionClass) {

		call(condition(conditionClass)
			.skipIfObjectsMissing(required)
			.skipIfStringsMissing(strings)
			.onSkip(onSkip)
			.onFail(Condition.ConditionResult.INFO)
			.dontStopOnFailure());
	}

	/**
	 * Create and evaluate a Condition in the current environment, but only if the environment contains the given
	 * objects and strings (both can be null).
	 *
	 * onFail is set to WARNING
	 *
	 */
	protected void skipIfMissing(String[] required, String[] strings, Condition.ConditionResult onSkip,
		Class<? extends Condition> conditionClass, String... requirements) {
		call(condition(conditionClass)
			.skipIfObjectsMissing(required)
			.skipIfStringsMissing(strings)
			.onSkip(onSkip)
			.requirements(requirements)
			.onFail(Condition.ConditionResult.WARNING)
			.dontStopOnFailure());
	}

	/**
	 * Create and evaluate a Condition in the current environment, but only if the environment contains the given
	 * objects and strings (both can be null).
	 *
	 */
	protected void skipIfMissing(String[] required, String[] strings, Condition.ConditionResult onSkip,
								 Class<? extends Condition> conditionClass, Condition.ConditionResult onFail, String... requirements) {
		call(condition(conditionClass)
			.skipIfObjectsMissing(required)
			.skipIfStringsMissing(strings)
			.onSkip(onSkip)
			.requirements(requirements)
			.onFail(onFail)
			.dontStopOnFailure());
	}

	/**
	 * Create and evaluate a Condition in the current environment, but only if the environment contains the given
	 * objects and strings (both can be null).
	 */
	protected void skipIfElementMissing(String objId, String path, Condition.ConditionResult onSkip,
										Class<? extends Condition> conditionClass, Condition.ConditionResult onFail, String... requirements) {
		call(condition(conditionClass)
			.skipIfElementMissing(objId, path)
			.onSkip(onSkip)
			.requirements(requirements)
			.onFail(onFail)
			.dontStopOnFailure());
	}

	/**
	 * Call the condition as specified in the builder. The ConditionCallBuilder is accessed in the following order:
	 *
	 *  - condition class is instantiated
	 *  - missing objects are checked
	 *  - missing strings are checked
	 *  - missing elements are checked
	 *  - pre-environment objects are checked
	 *  - pre-environment strings are checked
	 *  - condition is evaluated
	 *  - if failed, either throw a test exception or just log the failure
	 *  - if not failed:
	 *  	- post-environment objects are checked
	 *  	- post-environment strings are checked
	 *
	 * @param builder the fully configured condition call builder
	 */
	protected void call(ConditionCallBuilder builder) {

		// We skip these checks for a condition that we deliberately call without the lock held so as not to block
		// other threads; I suspect it means that this condition should have the ability to call setStatus or that
		// their functionality should be in a method in AbstractTestModule instead
		// Not all the 'WaitFor' functions are listed here; that means some of them we are calling with the lock held
		// which may be problematic (e.g. it prevents any incoming connections being process and I suspect may prevent
		// the test being aborted until the sleep expires). It would probably be preferable to always release the
		// lock whilst sleeping (which is probably best achieve by one of the ways outlined in the previous paragraph.)
		if (builder.getConditionClass() != SleepUntilAuthReqExpires.class &&
			builder.getConditionClass() != WaitFor5Seconds.class) {
			if (getStatus() != Status.CREATED) {
				// We don't run this check for 'CREATED' as the lock is currently not held during 'configure'; see
				// https://gitlab.com/openid/conformance-suite/issues/688
				if (!env.getLock().isHeldByCurrentThread()) {
					if (getStatus() != Status.RUNNING) {
						// give a more helpful error message that tells the developer what they have most likely done
						// wrong.
						throw new TestFailureException(getId(), "Condition '" +
							builder.getConditionClass().getSimpleName() + "' called when test status is '" +
							getStatus() + "'. This is a bug in the test module and probably means that a call to " +
							"setStatus(Status.RUNNING) is missing.");
					}

					// otherwise, it's still wrong to not have the lock held, we're just not able to tell the dev why
					throw new TestFailureException(getId(), "Condition '" + builder.getConditionClass().getSimpleName()
						+ "' called on a thread that does not hold lock (test status is '" + getStatus() + "'). This " +
						"is a bug in the test module.");
				}
			}
		}

		try {

			Condition condition = builder.getCondition();
			if (condition == null) {
				// create a new condition object from the class above
				condition = builder.getConditionClass()
					.getDeclaredConstructor()
					.newInstance();
			}
			condition.setProperties(id, eventLog, builder.getOnFail(), builder.getRequirements());

			logger.info(getId() + ": " + (builder.isStopOnFailure() ? ">>" : "}}") + " Calling Condition " + builder.getConditionClass().getSimpleName());

			// check the environment to see if we need to skip this call
			for (String req : builder.getSkipIfObjectsMissing()) {
				if (!env.containsObject(req)) {
					logger.info(getId() + ": [skip] Test condition " + builder.getConditionClass().getSimpleName() + " skipped, couldn't find key in environment: " + req);
					eventLog.log(condition.getMessage(), args(
						"msg", "Skipped evaluation due to missing required object: " + req,
						"expected", req,
						"result", builder.getOnSkip(),
						"mapped", env.isKeyShadowed(req) ? env.getEffectiveKey(req) : null,
						"requirements", builder.getRequirements()
					// TODO: log the environment here?
					));
					updateResultFromConditionFailure(builder.getOnSkip());
					return;
				}
			}
			for (String s : builder.getSkipIfStringsMissing()) {
				if (env.getString(s) == null) {
					logger.info(getId() + ": [skip] Test condition " + builder.getConditionClass().getSimpleName() + " skipped, couldn't find string in environment: " + s);
					eventLog.log(condition.getMessage(), args(
						"msg", "Skipped evaluation due to missing required string: " + s,
						"expected", s,
						"result", builder.getOnSkip(),
						"requirements", builder.getRequirements()
						// TODO: log the environment here?
					));
					updateResultFromConditionFailure(builder.getOnSkip());
					return;
				}
			}
			for (String s : builder.getSkipIfLongsMissing()) {
				if (env.getLong(s) == null) {
					logger.info(getId() + ": [skip] Test condition " + builder.getConditionClass().getSimpleName() + " skipped, couldn't find long integer in environment: " + s);
					eventLog.log(condition.getMessage(), args(
						"msg", "Skipped evaluation due to missing required long integer: " + s,
						"expected", s,
						"result", builder.getOnSkip(),
						"requirements", builder.getRequirements()
						// TODO: log the environment here?
					));
					updateResultFromConditionFailure(builder.getOnSkip());
					return;
				}
			}
			for (Pair<String, String> idx : builder.getSkipIfElementsMissing()) {
				JsonElement el = env.getElementFromObject(idx.getLeft(), idx.getRight());
				if (el == null) {
					logger.info(getId() + ": [skip] Test condition " + builder.getConditionClass().getSimpleName() + " skipped, couldn't find element in environment: " + idx.getLeft() + " " + idx.getRight());
					eventLog.log(condition.getMessage(), args(
						"msg", "Skipped evaluation due to missing required element: " + idx.getLeft() + " " + idx.getRight(),
						"object", idx.getLeft(),
						"path", idx.getRight(),
						"mapped", env.isKeyShadowed(idx.getLeft()) ? env.getEffectiveKey(idx.getLeft()) : null,
						"result", builder.getOnSkip(),
						"requirements", builder.getRequirements()
 					// TODO: log the environment here?
					));
					updateResultFromConditionFailure(builder.getOnSkip());
					return;
				}
			}

			condition.execute(env);

		} catch (ConditionError error) {
			if (error.isPreOrPostError()) {
				logger.info(getId() + ": [pre/post] Test condition failed " + builder.getConditionClass().getSimpleName() + " failure: " + error.getMessage());
				throw new TestFailureException(error);
			} else {
				if (builder.isStopOnFailure()) {
					logger.info(getId() + ": stopOnFailure Test condition failed " + builder.getConditionClass().getSimpleName() + " failure: " + error.getMessage());
					throw new TestFailureException(error);
				} else {
					logger.info(getId() + ": Test condition failure " + builder.getConditionClass().getSimpleName() + " failure: " + error.getMessage());
					updateResultFromConditionFailure(builder.getOnFail());
				}
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			logException(e);
			logger.error(getId() + ": Couldn't create condition object", e);
			throw new TestFailureException(getId(), "Fatal failure from condition: " + builder.getConditionClass().getSimpleName());
		} catch (TestFailureException e) {
			logger.error(getId() + ": Caught TestFailureException", e);
			throw e;
		} catch (Exception | Error e) {
			// it is unusual to catch Error, but if we're running in a background thread and don't catch it, nothing
			// will appear in the test results - and we want to log errors (e.g. stack overflows) into the test results
			// so they're easily visible rather than needing to dig through server console logging
			logException(e);
			logger.error(getId() + ": Generic error from underlying test framework", e);
			throw new TestFailureException(getId(), e);
		}

	}

	/**
	 * Create a new condition call builder, which can be passed to call()
	 */
	protected ConditionCallBuilder condition(Class<? extends Condition> conditionClass) {
		return new ConditionCallBuilder(conditionClass);
	}

	/**
	 * Create a new condition call builder, which can be passed to call()
	 */
	protected ConditionCallBuilder condition(Condition condition) {
		return new ConditionCallBuilder(condition);
	}

	/**
	 * Create a new test execution builder, which can be passed to call()
	 */
	protected Command exec() {
		return new Command();
	}

	/**
	 * Execute a set of test execution commands.
	 *
	 * Commands in the builder are executed in the following order:
	 *
	 *  - environment strings are exposed
	 *  - log blocks are started
	 *  - environment keys are mapped
	 *  - environment keys are unmapped
	 *  - log blocks are ended
	 *
	 */
	protected void call(Command builder) {

		for(String e : builder.getExposeStrings()) {
			exposeEnvString(e);
		}

		if (!Strings.isNullOrEmpty(builder.getStartBlock())) {
			eventLog.startBlock(builder.getStartBlock());
		}

		builder.getEnvCommands().forEach(cmd -> cmd.accept(env));

		if (builder.isEndBlock()) {
			eventLog.endBlock();
		}
	}

	/**
	 * Dispatch function to call a more specific subclass as needed.
	 */
	protected void call(TestExecutionUnit builder) {
		if (builder instanceof ConditionCallBuilder callBuilder) {
			call(callBuilder);
		} else if (builder instanceof Command command) {
			call(command);
		} else if (builder instanceof ConditionSequence sequence) {
			call(sequence);
		} else if (builder instanceof ConditionSequenceCallBuilder callBuilder) {
			call(callBuilder);
		} else if (builder instanceof SkippedCondition condition) {
			eventLog.log(condition.getSource(), args(
					"msg", condition.getMessage()));
		} else {
			throw new TestFailureException(getId(), "Unknown class passed to call() function");
		}
	}

	/**
	 * Create a caller for the given sequence
	 */
	protected ConditionSequenceCallBuilder sequence(Class<? extends ConditionSequence> conditionSequenceClass) {
		return new ConditionSequenceCallBuilder(conditionSequenceClass);
	}

	protected ConditionSequenceCallBuilder sequence(Supplier<? extends ConditionSequence> conditionSequenceConstructor) {
		return new ConditionSequenceCallBuilder(conditionSequenceConstructor);
	}

	private ConditionSequence createSequence(Class<? extends ConditionSequence> conditionSequenceClass) {
		try {
			ConditionSequence conditionSequence = conditionSequenceClass
				.getDeclaredConstructor()
				.newInstance();

			return conditionSequence;

		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			logException(e);
			logger.error(getId() + ": Couldn't create condition sequence object", e);
			throw new TestFailureException(getId(), "Fatal failure from condition sequence: " + conditionSequenceClass.getSimpleName());
		}
	}

	protected ConditionSequence sequenceOf(TestExecutionUnit... units) {
		return new AbstractConditionSequence() {

			@Override
			public void evaluate() {
				call(Arrays.asList(units));
			}
		};
	}

	protected void call(ConditionSequenceCallBuilder builder) {
		ConditionSequence sequence;

		if (builder.getConditionSequenceConstructor() != null) {
			sequence = builder.getConditionSequenceConstructor().get();
		} else {
			sequence = createSequence(builder.getConditionSequenceClass());
		}

		call(sequence);
	}

	protected void call(ConditionSequence sequence) {
		logger.info(getId() + ":   Starting sequence " + sequence.getClass().getSimpleName());

		// execute the sequence
		sequence.evaluate();

		// pass all of the resulting units to the call functions
		sequence.getTestExecutionUnits()
			.forEach(this::call);

		logger.info(getId() + ":   End of sequence " + sequence.getClass().getSimpleName());
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Status getStatus() {
		// Note that this (deliberately) doesn't take a lock on the Environment (as 'setStatus()' does), so the status
		// is potentially inaccurate/immediately out of date if another thread is within a call to setStatus().
		//
		// Taking the lock would be undesireable as it would mean the 'get status' HTTP API would block whenever the
		// test status is RUNNING (which is a lot of the time) as the test has the lock whilst in RUNNING.
		return status;
	}

	protected void logFinalEnv() {
		logger.info(getId() + ": Final environment: " + env);
	}

	@Override
	public void fireSetupDone() {
		eventLog.log(getName(), "Setup Done");
	}

	@Override
	public void fireTestFinished() {

		// first we set our test to WAITING to release the lock (note that this happens in the calling thread) and prepare for finalization
		setStatusInternal(Status.WAITING);
		fireTestFinishedInternal();
	}

	// internal version of above used to skip the 'setStatus(WAITING)' when called from non-test jobs
	private void fireTestFinishedInternal() {

		// this happens in the background so that we can check the state of the browser controller

		getTestExecutionManager().runFinalisationTaskInBackground(() -> {

			// wait for web runners to wrap up first

			Instant timeout = Instant.now().plusSeconds(60); // wait at most 60 seconds
			while (browser.runnersActive()
				&& Instant.now().isBefore(timeout)) {
				Thread.sleep(100); // sleep before we check again
			}

			// really at this point there should be no other threads running (though the placeholder watcher may be)
			// kill everything else anyway - we don't hold any locks so we don't want anything else doing anything
			// whilst or after we tidy up.
			getTestExecutionManager().cancelAllBackgroundTasksExceptFinalisation();

			if (getResult() == Result.UNKNOWN) {
				List<?> filledPlaceholders = imageService.getFilledPlaceholders(getId(), true);
				if (filledPlaceholders.size() > 0) {
					// This is only necessary for placeholders filled by browsercontrol; for images uploaded by the
					// user we set the status to review when the image is uploaded
					fireTestReviewNeeded();
				} else {
					fireTestSuccess();
				}
			}

			// clean up any remaining placeholders here; if we call this function then we have reached a condition where we're not expecting them to be filled externally

			List<String> placeholders = imageService.getRemainingPlaceholders(getId(), true);

			for (String placeholder : placeholders) {
				Map<String, Object> update = ImmutableMap.of(
					"image_no_longer_required", true);
				imageService.fillPlaceholder(getId(), placeholder, update, true);
			}

			eventLog.log(getName(), args(
				"msg", "Test has run to completion",
				"result", Status.FINISHED.toString(),
				"testmodule_result", getResult()));

			// if we weren't interrupted already, then we're finished
			if (!getStatus().equals(Status.INTERRUPTED)) {
				// log the environment here, as "stop" won't do so for the 'finished' case
				logFinalEnv();

				// this must be pretty much the last thing we do, we must NEVER mark the test as finished until
				// everything has happen, as 'FINISHED' is the cue for run-test-plan.py to fetch the results, start
				// the next test, etc.
				// This will run any 'cleanup' tasks for the test module
				setStatusInternal(Status.FINISHED);
			}

			// stop() will also cancel the current thread, so don't do any logging etc after this
			stop("Test has run to completion.");

			return "done";
		});
	}

	@Override
	public void fireTestReviewNeeded() {
		if (!Result.FAILED.equals(result)) {
			setResult(Result.REVIEW);
		}
	}

	private void fireTestSuccess() {
		setResult(Result.PASSED);
	}

	private void fireTestFailure() {
		setResult(Result.FAILED);
	}

	@Override
	public void fireTestSkipped(String msg) throws TestSkippedException {
		// There's some potential conflict here with other results; mainly that setting the result to SKIPPED will
		// overwrite any prior WARNING result. It's debatable which result is more important, it seems like
		// the fact that the test couldn't be completed is the more important.
		//
		// Overwriting 'REVIEW' is also potentially concerning but really we should never skip a test after a user
		// has uploaded a screenshot.
		if (getResult() != Result.FAILED) {
			setResult(Result.SKIPPED);
		}
		throw new TestSkippedException(getId(), msg);
	}

	/**
	 * @return the result
	 */
	@Override
	public Result getResult() {
		return result;
	}

	/**
	 * @param result
	 *            the result to set
	 */
	private void setResult(Result result) {
		this.result = result;
		testInfo.updateTestResult(getId(), getResult());
	}

	private void updateResultFromConditionFailure(Condition.ConditionResult onFail) {
		switch (onFail) {
			case FAILURE:
				setResult(Result.FAILED);
				break;
			case WARNING:
				if (getResult() != Result.FAILED) {
					setResult(Result.WARNING);
				}
				break;
			default:
				// No action
				break;
		}
	}

	protected void setStatus(Status newStatus) {
		switch (newStatus) {
			case CONFIGURED:
			case WAITING:
			case RUNNING:
				setStatusInternal(newStatus);
				break;

			default:
				throw new TestFailureException(getId(), "Test module called setStatus() with a value other than CONFIGURED/WAITING/RUNNING. This is a bug in the test module; it should use a different method to change to the desired state - e.g. fireTestFinished() or throwing a TestFailureException.");
		}
	}

	/*
	 * Test status state machine:
	 *
	 *          /----------->--------------------------------\
	 *         /           /                                  \
	 *        /----------------->----------------\             \
	 *       /           /     /                  v             v
	 *   CREATED -> CONFIGURED -> RUNNING --> FINISHED      INTERRUPTED
	 *                         \     ^--v      ^              ^
	 *                          \-> WAITING --/--------------/
	 *
	 */
	private void setStatusInternal(Status newStatus) {
		try {
			final boolean hadLockOnEntry = env.getLock().isHeldByCurrentThread();

			logger.info(getId() + ": setStatus(" + newStatus.toString() + "): hadLockOnEntry="+hadLockOnEntry+", current status = " + getStatus().toString());

			if (!hadLockOnEntry) {
				// acquire lock immediately - as well as protecting the Environment, the lock also protects the status variable
				acquireLock();
				logger.info(getId() + ": setStatus(" + newStatus.toString() + "): lock acquired, current status = " + getStatus().toString());
			}
			Status oldStatus = getStatus(); // must be after lock is taken

			if (newStatus == Status.RUNNING) {
				if (hadLockOnEntry) {
					// RUNNING->RUNNING isn't good, and moved /to/ RUNNING when we already hold the lock probably isn't right either?
					throw new TestFailureException(getId(), "Illegal test state change by thread that holds lock: " + oldStatus + " -> " + newStatus);
				}
			} else if (newStatus == oldStatus) {
				// nothing to change
				throw new TestFailureException(getId(), "setStatus() called but status is the same: " + oldStatus + " -> " + newStatus);
			}

			if (hadLockOnEntry && oldStatus != Status.RUNNING) {
				throw new TestFailureException(getId(), "Illegal current test status for thread that holds lock: " + oldStatus + " -> " + newStatus);
			}

			// must be after lock acquired, or the status might've changed by the time we wake up

			switch (oldStatus) {
				case NOT_YET_CREATED:
					switch (newStatus) {
						case CREATED:
							break;
						default:
							throw new TestFailureException(getId(), "Illegal test state change: " + oldStatus + " -> " + newStatus);
					}
					break;
				case CREATED:
					switch (newStatus) {
						case CONFIGURED:
						case WAITING:
						case INTERRUPTED:
						case FINISHED:
							break;
						default:
							throw new TestFailureException(getId(), "Illegal test state change: " + oldStatus + " -> " + newStatus);
					}
					break;
				case CONFIGURED:
					switch (newStatus) {
						case RUNNING:
						case INTERRUPTED:
						case FINISHED:
						case WAITING:
							break;
						default:
							throw new TestFailureException(getId(), "Illegal test state change: " + oldStatus + " -> " + newStatus);
					}
					break;
				case RUNNING:  // We should have the lock when we're running
					switch (newStatus) {
						case INTERRUPTED:
							break;
						case FINISHED:
						case WAITING:
							break;
						default:
							throw new TestFailureException(getId(), "Illegal test state change: " + oldStatus + " -> " + newStatus);
					}
					break;
				case WAITING:  // we shouldn't have the lock if we're waiting.
					switch (newStatus) {
						case RUNNING:
						case INTERRUPTED:
						case FINISHED:
							break;
						default:
							throw new TestFailureException(getId(), "Illegal test state change: " + oldStatus + " -> " + newStatus);
					}
					break;
				case INTERRUPTED:
					throw new TestFailureException(getId(), "Illegal test state change: " + oldStatus + " -> " + newStatus);
				case FINISHED:
					throw new TestFailureException(getId(), "Illegal test state change: " + oldStatus + " -> " + newStatus);
				default:
					throw new TestFailureException(getId(), "Illegal test state change: " + oldStatus + " -> " + newStatus);
			}

			if (Status.FINISHED.equals(newStatus) || Status.INTERRUPTED.equals(newStatus)) {
				// make the cleanup steps complete before we move the test to 'FINISHED' or 'INTERRUPTED'
				performFinalCleanup();
			}

			if (Status.FINISHED.equals(newStatus) && getResult() == Result.UNKNOWN) {
				throw new TestFailureException(getId(), "Illegal test state; tried to move from " + oldStatus + " -> " + newStatus + " but 'result' is UNKNOWN");
			}

			this.status = newStatus;
			testInfo.updateTestStatus(getId(), newStatus);

			this.statusUpdated = Instant.now();

			if (Status.RUNNING.equals(newStatus)) {
				// exit with the lock still held, as we should always have the lock when TestConditions are being run
			} else {
				// release the lock as the very final step; this ensure other threads won't start reading the
				// test status until after it's been updated, etc.
				clearLock();
			}
		} catch (Exception | Error e) {
			// It's really best if we don't exit with the lock held, ensuring any other threads trying to take the
			// lock won't end up blocked forever.
			clearLockIfHeld();
			throw e;
		}
	}


	/**
	 * Clear the lock. If we don't have it in the current thread, throw an exception.
	 */
	protected void clearLock(){
		env.getLock().unlock();
	}

	/**
	 * Helper to check if we have the lock, and if we do, unlock it. If we don't have the lock
	 * in the current thread, do nothing.
	 */
	protected void clearLockIfHeld(){
		if(env.getLock().isHeldByCurrentThread()) {
			env.getLock().unlock();
		}
	}

	/**
	 * Add a key/value pair to the exposed values that the user will see in the frontend
	 *
	 * @param key
	 * @param val
	 */
	protected void expose(String key, String val) {
		exposed.put(key, val);
	}

	/**
	 * Expose a value from the environment so the user sees it in the frontend
	 *
	 * @param key
	 */
	protected void exposeEnvString(String key) {
		String val = env.getString(key);
		expose(key, val);
	}

	@Override
	public Map<String, String> getExposedValues() {
		return exposed;
	}

	@Override
	public BrowserControl getBrowser() {
		return this.browser;
	}

	/**
	 * @return the name
	 */
	@Override
	public String getName() {
		return testNameSupplier.get();
	}

	@Override
	public void stop(String reason) {

		if (!(getStatus().equals(Status.FINISHED) || getStatus().equals(Status.INTERRUPTED))) {
			setStatusInternal(Status.INTERRUPTED);
			eventLog.log(getName(), args(
				"msg", "Test was interrupted before it could complete. "+reason,
				"result", Status.INTERRUPTED.toString()));
			// It's a bit weird that the above log() puts INTERRUPTED (a TestModule status value) into 'result' in the
			// db, which normally contains a ConditionResult.

			logFinalEnv();
		}

		// This might interrupt the current thread, so don't do any logging after this
		getTestExecutionManager().cancelAllBackgroundTasks();
	}

	protected void performFinalCleanup() {
		if (!cleanupCalled) {
			logger.info(getId() + ": Performing final clean-up");
			try {
				cleanup();
			} catch (TestFailureException e) {
				eventLog.log(getName(), ex(e, args("msg", "A test failure was raised while cleaning up")));
			} finally {
				cleanupCalled = true;
			}
		}
	}

	@Override
	public void handleException(TestInterruptedException error, String source) {
		logger.error(getId() + ": Caught an error in '"+source+"' while running the test, stopping the test: " + error.getMessage());

		if (error instanceof TestSkippedException) {
			eventLog.log(getName(),
				args(
					"result", TestModule.Result.SKIPPED,
					"msg", "The test was skipped: " + error.getMessage()));
			fireTestFinished();
		} else {
			/* must be a TestFailureException */
			String failure;
			if (error.getCause() instanceof ConditionError) {
				// ConditionError will already have been logged when created in AbstractCondition.java (and
				// ConditionError should not be thrown from other places, see
				// https://gitlab.com/openid/conformance-suite/issues/443 ) - so no need to log again
				failure = error.getCause().getMessage();
			} else {
				failure = error.getMessage();

				Map<String, Object> event = new HashMap<>();
				event.put("caught_at", source);
				if (error.getCause() == null) {
					// this must be a message a TestModule has explicitly thrown, i.e. with
					// throw new TestFailureException(getId(), "Client has incorrectly <...>");
					// log that message rather than 'unexpected exception caught'
					event.put("msg", failure);
				} else {
					// if the root error isn't a ConditionError nor an explicit message from a test module, set this so the UI can display the underlying error in detail
					setFinalError(error);
				}
				eventLog.log(getName(), ex(error, event));
			}

			// Any exception except 'skipped' from a test counts as a failure
			fireTestFailure();
			// stop() might interrupt the current thread, so don't do any logging after this
			stop("The failure '"+failure+"' means the test cannot continue.");
		}
	}

	@Override
	public void cleanup() {
		// Nothing to do in general
	}

	/**
	 * @return the created
	 */
	@Override
	public Instant getCreated() {
		return created;
	}

	@Override
	public Instant getStatusUpdated() {
		return statusUpdated;
	}

	/**
	 * @return the finalError
	 */
	@Override
	public TestInterruptedException getFinalError() {
		return finalError;
	}

	/**
	 * @param finalError the finalError to set
	 */
	@Override
	public void setFinalError(TestInterruptedException finalError) {
		this.finalError = finalError;
	}

	protected void acquireLock() {
		env.getLock().lock();
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		throw new TestFailureException(getId(), "Got an HTTP request to '"+path+"' that wasn't expected");
	}

	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		throw new TestFailureException(getId(), "Got an HTTP request to '"+path+"' that wasn't expected");
	}

	@Override
	public TestExecutionManager getTestExecutionManager() {
		return executionManager;
	}

	protected void waitForPlaceholders() {
		// set up a listener to wait for either an error callback or an image upload
		executionManager.runInBackground(() -> {
			long delayMillis = 1000; // wait for a second before we check the first time

			Thread.sleep(delayMillis);

			while (true) {

				// grab the lock before we check anything in case something is finishing up
				acquireLock();

				// re-fetch the placeholders every check
				List<String> remainingPlaceholders = imageService.getRemainingPlaceholders(getId(), true);

				if (getStatus().equals(Status.FINISHED) || getStatus().equals(Status.INTERRUPTED)) {
					// if the test is finished/interrupted, nothing for us to do, stop looking
					clearLock();
					break;
				}
				if (remainingPlaceholders.isEmpty() && getStatus().equals(Status.WAITING)) {
					// if the test is still waiting, but all the placeholders are gone, then we can call it finished, stop looking
					clearLock();
					fireTestFinishedInternal();
					break;
				}
				// otherwise (test is waiting but placeholders are still there, or test is running, etc), check again in the future
				clearLock();

				if (delayMillis < 30 * 1000) {
					// backoff checks to every 30 seconds so we don't overload db or jvm
					delayMillis *= 2;
				}
				Thread.sleep(delayMillis);

			}

			return "done";
		});
	}

	@Override
	public void checkLockReleased() {
		if (env.getLock().isHeldByCurrentThread()) {
			throw new TestFailureException(getId(), "The test module has incorrectly left the lock held, this is a bug in the test module");
		}
	}

	@Override
	public void forceReleaseLock() {
		clearLockIfHeld();
	}
}
