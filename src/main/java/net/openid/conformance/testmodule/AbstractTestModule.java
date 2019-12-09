package net.openid.conformance.testmodule;

import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.frontChannel.BrowserControl;
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
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class AbstractTestModule implements TestModule, DataUtils {

	private static Logger logger = LoggerFactory.getLogger(AbstractTestModule.class);

	// Set up Thread executor
	//private ExecutorService executorService = Executors.newCachedThreadPool();

	private String id = null; // unique identifier for the test, set from the outside
	private Status status = Status.UNKNOWN; // current status of the test
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

		setStatus(Status.CREATED);
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
			throw new RuntimeException(String.format("BUG: invalid value for variant %s: %s",
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
	protected void callAndStopOnFailure(Class<? extends Condition> conditionClass, String... requirements) {
		call(condition(conditionClass)
			.onFail(Condition.ConditionResult.FAILURE)
			.requirements(requirements));
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

	private void logException(Exception e) {
		Map<String, Object> event = ex(e);
		event.put("msg", "Caught exception from test framework");

		eventLog.log(getName(), event);
	}

	/**
	 * Create and evaluate a Condition in the current environment. Log but ignore if the Condition fails.
	 *
	 * onFail is set to INFO if requirements is null or empty, WARNING if requirements are specified
	 *
	 */
	protected void callAndContinueOnFailure(Class<? extends Condition> conditionClass, String... requirements) {
		call(condition(conditionClass)
			.onFail((requirements == null || requirements.length == 0) ? Condition.ConditionResult.INFO : Condition.ConditionResult.WARNING)
			.requirements(requirements)
			.dontStopOnFailure());
	}

	/**
	 * Create and evaluate a Condition in the current environment. Log but ignore if the Condition fails.
	 *
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

		try {

			// create a new condition object from the class above
			Condition condition = builder.getConditionClass()
					.getDeclaredConstructor()
					.newInstance();
			condition.setProperties(id, eventLog, builder.getOnFail(), builder.getRequirements());

			logger.info((builder.isStopOnFailure() ? ">>" : "}}") + " Calling Condition " + builder.getConditionClass().getSimpleName());

			// check the environment to see if we need to skip this call
			for (String req : builder.getSkipIfObjectsMissing()) {
				if (!env.containsObject(req)) {
					logger.info("[skip] Test condition " + builder.getConditionClass().getSimpleName() + " skipped, couldn't find key in environment: " + req);
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
					logger.info("[skip] Test condition " + builder.getConditionClass().getSimpleName() + " skipped, couldn't find string in environment: " + s);
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
			for (Pair<String, String> idx : builder.getSkipIfElementsMissing()) {
				JsonElement el = env.getElementFromObject(idx.getLeft(), idx.getRight());
				if (el == null) {
					logger.info("[skip] Test condition " + builder.getConditionClass().getSimpleName() + " skipped, couldn't find element in environment: " + idx.getLeft() + " " + idx.getRight());
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
				logger.info("[pre/post] Test condition failed " + builder.getConditionClass().getSimpleName() + " failure: " + error.getMessage());
				fireTestFailure();
				throw new TestFailureException(error);
			} else {
				if (builder.isStopOnFailure()) {
					logger.info("stopOnFailure Test condition failed " + builder.getConditionClass().getSimpleName() + " failure: " + error.getMessage());
					fireTestFailure();
					throw new TestFailureException(error);
				} else {
					logger.info("Test condition failure " + builder.getConditionClass().getSimpleName() + " failure: " + error.getMessage());
					updateResultFromConditionFailure(builder.getOnFail());
				}
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			logException(e);
			logger.error("Couldn't create condition object", e);
			fireTestFailure();
			throw new TestFailureException(getId(), "Couldn't create required condition: " + builder.getConditionClass().getSimpleName());
		} catch (TestFailureException e) {
			logger.error("Caught TestFailureException", e);
			fireTestFailure();
			throw e;
		} catch (Exception e) {
			logException(e);
			logger.error("Generic error from underlying test framework", e);
			fireTestFailure();
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
		if (builder instanceof ConditionCallBuilder) {
			call((ConditionCallBuilder)builder);
		} else if (builder instanceof Command) {
			call((Command)builder);
		} else if (builder instanceof ConditionSequence) {
			call((ConditionSequence)builder);
		} else if (builder instanceof ConditionSequenceCallBuilder) {
			call((ConditionSequenceCallBuilder)builder);
		} else if (builder instanceof SkippedCondition) {
			eventLog.log(((SkippedCondition) builder).getSource(), args(
					"msg", ((SkippedCondition) builder).getMessage()));
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
			logger.error("Couldn't create condition sequence object", e);
			fireTestFailure();
			throw new TestFailureException(getId(), "Couldn't create required condition sequence: " + conditionSequenceClass.getSimpleName());
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
		logger.info("   Starting sequence " + sequence.getClass().getSimpleName());

		// execute the sequence
		sequence.evaluate();

		// pass all of the resulting units to the call functions
		sequence.getTestExecutionUnits()
			.forEach(this::call);

		logger.info("   End of sequence " + sequence.getClass().getSimpleName());
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Status getStatus() {
		return status;
	}

	protected void logFinalEnv() {
		logger.info("Final environment: " + env);
	}

	@Override
	public void fireSetupDone() {
		eventLog.log(getName(), "Setup Done");
	}

	@Override
	public void fireTestFinished() {

		// first we set our test to WAITING to release the lock (note that this happens in the calling thread) and prepare for finalization
		setStatus(Status.WAITING);

		// then this happens in the background so that we can check the state of the browser controller

		getTestExecutionManager().runInBackground(() -> {


			// wait for web runners to wrap up first

			Instant timeout = Instant.now().plusSeconds(60); // wait at most 60 seconds
			while (browser.getWebRunners().size() > 0
				&& Instant.now().isBefore(timeout)) {
				Thread.sleep(100); // sleep before we check again
			}

			// if we weren't interrupted already, then we're finished
			if (!getStatus().equals(Status.INTERRUPTED)) {
				setStatus(Status.FINISHED);

				// log the environment here in case "stop" doesn't get it it
				logFinalEnv();
			}

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

			// This might interrupt the current thread, so don't do any logging after this
			stop();

			return "done";
		});
	}

	@Override
	public void fireTestReviewNeeded() {
		if (!Result.FAILED.equals(result)) {
			setResult(Result.REVIEW);
		}
	}

	@Override
	public void fireTestSuccess() {
		setResult(Result.PASSED);
	}

	@Override
	public void fireTestFailure() {
		setResult(Result.FAILED);
	}

	@Override
	public void fireTestSkipped(String msg) throws TestSkippedException {
		if (getResult() != Result.FAILED) {
			setResult(Result.SKIPPED);
		}
		fireTestFinished();
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
	protected void setResult(Result result) {
		this.result = result;
		testInfo.updateTestResult(getId(), getResult());
	}

	protected void updateResultFromConditionFailure(Condition.ConditionResult onFail) {
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
	 * Any state can go to "UNKNOWN"
	 */
	protected void setStatus(Status newStatus) {
		Status oldStatus = getStatus();

		logger.info("setStatus("+newStatus.toString()+"): current status = "+oldStatus.toString());

		if (newStatus == oldStatus) {
			// nothing to change
			return;
		}

		switch (oldStatus) {
			case CREATED:
				switch (newStatus) {
					case CONFIGURED:
					case WAITING:
					case INTERRUPTED:
					case FINISHED:
						break;
					default:
						clearLockIfHeld();
						throw new TestFailureException(getId(), "Illegal test state change: " + oldStatus + " -> " + newStatus);
				}
				break;
			case CONFIGURED:
				switch (newStatus) {
					case RUNNING:
						acquireLock();
						break;
					case INTERRUPTED:
					case FINISHED:
					case WAITING:
						break;
					default:
						clearLock();
						throw new TestFailureException(getId(), "Illegal test state change: " + oldStatus + " -> " + newStatus);
				}
				break;
			case RUNNING:  // We should have the lock when we're running
				switch (newStatus) {
					case INTERRUPTED:
						clearLockIfHeld();
						break;
					case FINISHED:
					case WAITING:
						clearLock();
						break;
					default:
						clearLockIfHeld();
						throw new TestFailureException(getId(), "Illegal test state change: " + oldStatus + " -> " + newStatus);
				}
				break;
			case WAITING:  // we shouldn't have the lock if we're waiting.
				switch (newStatus) {
					case RUNNING:
						acquireLock();  // we want to grab the lock whenever we start running
						break;
					case INTERRUPTED:
					case FINISHED:
						break;
					default:
						clearLockIfHeld();
						throw new TestFailureException(getId(), "Illegal test state change: " + oldStatus + " -> " + newStatus);
				}
				break;
			case INTERRUPTED:
				clearLockIfHeld();
				throw new TestFailureException(getId(), "Illegal test state change: " + oldStatus + " -> " + newStatus);
			case FINISHED:
				clearLockIfHeld();
				throw new TestFailureException(getId(), "Illegal test state change: " + oldStatus + " -> " + newStatus);
			case UNKNOWN:
				// we can go from unknown to anything
				switch (newStatus) {
					case RUNNING:
						acquireLock();  // we want to grab the lock whenever we start running
						break;
					default:
						clearLockIfHeld();
						break;
				}
				break;
			default:
				clearLockIfHeld();
				throw new TestFailureException(getId(), "Illegal test state change: " + oldStatus + " -> " + newStatus);
		}

		this.status = newStatus;
		testInfo.updateTestStatus(getId(), newStatus);

		this.statusUpdated = Instant.now();
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
	 * Add a key/value pair to the exposed values
	 *
	 * @param key
	 * @param val
	 */
	protected void expose(String key, String val) {
		exposed.put(key, val);
	}

	/**
	 * Expose a value from the environment
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

	/* (non-Javadoc)
	 * @see TestModule#stop()
	 */
	@Override
	public void stop() {

		if (!(getStatus().equals(Status.FINISHED) || getStatus().equals(Status.INTERRUPTED))) {
			setStatus(Status.INTERRUPTED);
			eventLog.log(getName(), args(
				"msg", "Test was interrupted before it could complete",
				"result", Status.INTERRUPTED.toString()));

			logFinalEnv();
		}

		if (!cleanupCalled) {
			logger.info("Performing final clean-up");
			try {
				cleanup();
			} catch (TestFailureException e) {
				eventLog.log(getName(), ex(e, args("msg", "A test failure was raised while cleaning up")));
			} finally {
				cleanupCalled = true;
			}
		}

		// This might interrupt the current thread, so don't do any logging after this
		getTestExecutionManager().clearBackgroundTasks();
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

	protected RedirectView redirectToLogDetailPage() {
		return new RedirectView("/log-detail.html?log=" + getId());
	}

	protected void acquireLock() {
		env.getLock().lock();
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		throw new TestFailureException(getId(), "Got an HTTP response we weren't expecting");
	}

	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		throw new TestFailureException(getId(), "Got an HTTP response we weren't expecting");
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

			boolean cont = true;

			while (cont) {

				// grab the lock before we check anything in case something is finishing up
				acquireLock();

				// re-fetch the placeholders every check
				List<String> remainingPlaceholders = imageService.getRemainingPlaceholders(getId(), true);

				if (getStatus().equals(Status.FINISHED) || getStatus().equals(Status.INTERRUPTED)) {
					// if the test is finished/interrupted, nothing for us to do, stop looking
					cont = false;
				} else if (remainingPlaceholders.isEmpty() && getStatus().equals(Status.WAITING)) {
					// if the test is still waiting, but all the placeholders are gone, then we can call it finished, stop looking
					fireTestFinished();
					cont = false;
				} else {
					// otherwise (test is waiting but placeholders are still there, or test is running, etc), check again in the future
					cont = true;
				}

				// let go of the lock
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

}