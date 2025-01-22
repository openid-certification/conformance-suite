package net.openid.conformance.testmodule;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class to collect the attributes related to a Condition call, such as which class
 * to call, what to do on failure, when the call should be skipped.
 */
public class ConditionCallBuilder implements TestExecutionUnit {

	private Class<? extends Condition> conditionClass = null;
	private Condition condition = null;
	private List<String> requirements = new ArrayList<>();
	private ConditionResult onFail = ConditionResult.FAILURE;
	private ConditionResult onSkip = ConditionResult.INFO;
	private boolean stopOnFailure = true;
	private List<String> skipIfObjectsMissing = new ArrayList<>();
	private List<String> skipIfStringsMissing = new ArrayList<>();
	private List<String> skipIfLongsMissing = new ArrayList<>();
	private List<Pair<String, String>> skipIfElementsMissing = new ArrayList<>();

	/**
	 * Create a new condition call based on the given condition class.
	 *
	 * @param condition The Condition that will be handed to the executor.
	 */
	public ConditionCallBuilder(Condition condition) {
		assert condition != null; // the condition can't be null
		this.condition = condition;
		this.conditionClass = condition.getClass();
	}

	/**
	 * Create a new condition call based on the given condition class.
	 *
	 * @param conditionClass The Condition that will be handed to the executor.
	 */
	public ConditionCallBuilder(Class<? extends Condition> conditionClass) {
		assert conditionClass != null; // the condition class can't be null
		this.conditionClass = conditionClass;
	}

	/**
	 * Add a single requirement tag to this condition call, used to reference
	 * external specification requirements. This is added to any existing
	 * requirements.
	 *
	 * @param requirement The requirement string to add, such as ("OIDCC-2.1.2")
	 * @return this builder
	 */
	public ConditionCallBuilder requirement(String requirement) {
		this.requirements.add(requirement);
		return this;
	}

	/**
	 * Add a list of requirement tags to this condition call, used to reference
	 * external specification requirements. These are added to any existing
	 * requirements.
	 *
	 * @param requirements The requirement strings to add, such as ("OIDCC-2.1.2", "FAPI-R-2.3.1")
	 * @return this builder
	 */
	public ConditionCallBuilder requirements(String... requirements) {
		Collections.addAll(this.requirements, requirements);
		return this;
	}

	/**
	 * Set the result to log if the condition fails by throwing a ConditionError
	 * during evaluation.
	 *
	 * Defaults to FAILURE
	 *
	 * @param onFail The result to log
	 * @return this builder
	 */
	public ConditionCallBuilder onFail(ConditionResult onFail) {
		this.onFail = onFail;
		return this;
	}

	/**
	 * Set the result to log if the condition is skipped via one of the skip mechanisms
	 * (objects, strings, or elements). If skipped, the condition is not evaluated.
	 *
	 * Defaults to INFO
	 *
	 * @param onSkip The result to log
	 * @return this builder
	 */
	public ConditionCallBuilder onSkip(ConditionResult onSkip) {
		this.onSkip = onSkip;
		return this;
	}

	/**
	 * Indicate the test should continue execution even if the condition fails. By default,
	 * test execution will stop if the condition fails.
	 *
	 * @return this builder
	 */
	public ConditionCallBuilder dontStopOnFailure() {
		this.stopOnFailure = false;
		return this;
	}

	/**
	 * Add an object reference to search the Environment for prior to execution. If this object
	 * is not found in the Environment at runtime, the condition is not evaluated and the result
	 * stored in onSkip is logged.
	 *
	 * @param skipIfObjectMissing the object identifier to search for
	 * @return this builder
	 */
	public ConditionCallBuilder skipIfObjectMissing(String skipIfObjectMissing) {
		this.skipIfObjectsMissing.add(skipIfObjectMissing);
		return this;
	}

	/**
	 * Add several object references to search the Environment for prior to execution. If any of
	 * these objects are not found in the Environment at runtime, the condition is not evaluated
	 * and the result stored in onSkip is logged.
	 *
	 * @param skipIfObjectsMissing the object identifiers to search for
	 * @return this builder
	 */
	public ConditionCallBuilder skipIfObjectsMissing(String... skipIfObjectsMissing) {
		if (skipIfObjectsMissing != null) {
			Collections.addAll(this.skipIfObjectsMissing, skipIfObjectsMissing);
		}
		return this;
	}

	/**
	 * Add a string reference to search the Environment for prior to execution. If no string
	 * is found in the Environment at runtime, the condition is not evaluated and the result
	 * stored in onSkip is logged.
	 *
	 * See Environment.getString(String)
	 *
	 * @param skipIfStringMissing the string reference to search for
	 * @return this builder
	 */
	public ConditionCallBuilder skipIfStringMissing(String skipIfStringMissing) {
		this.skipIfStringsMissing.add(skipIfStringMissing);
		return this;
	}

	/**
	 * Add a long reference to search the Environment for prior to execution. If no long
	 * is found in the Environment at runtime, the condition is not evaluated and the result
	 * stored in onSkip is logged.
	 *
	 * See Environment.getLong(String)
	 *
	 * @param skipIfLongMissing the long reference to search for
	 * @return this builder
	 */
	public ConditionCallBuilder skipIfLongMissing(String skipIfLongMissing) {
		this.skipIfLongsMissing.add(skipIfLongMissing);
		return this;
	}

	/**
	 * Add several string references to search the Environment for prior to execution. If any
	 * of these strings are not found in the Environment at runtime, the condition is not evaluated
	 * and the result stored in onSkip is logged.
	 *
	 * See Environment.getString(String)
	 *
	 * @param skipIfStringsMissing the string references to search for
	 * @return this builder
	 */
	public ConditionCallBuilder skipIfStringsMissing(String... skipIfStringsMissing) {
		if (skipIfStringsMissing != null) {
			Collections.addAll(this.skipIfStringsMissing, skipIfStringsMissing);
		}
		return this;
	}

	/**
	 * Add several long references to search the Environment for prior to execution. If any
	 * of these longs are not found in the Environment at runtime, the condition is not evaluated
	 * and the result stored in onSkip is logged.
	 *
	 * See Environment.getLong(String)
	 *
	 * @param skipIfLongsMissing the string references to search for
	 * @return this builder
	 */
	public ConditionCallBuilder skipIfLongsMissing(String... skipIfLongsMissing) {
		if (skipIfLongsMissing != null) {
			Collections.addAll(this.skipIfLongsMissing, skipIfLongsMissing);
		}
		return this;
	}

	/**
	 * Add an element reference to search the Environment for prior to execution. If the element
	 * is not found in the Environment at runtime, the condition is not evaluated and
	 * the result stored in onSkip is logged.
	 *
	 * See Environment.getElement(String, String)
	 *
	 * @param objId The object in the environment to search
	 * @param path The path within the object to retrieve, in dot-separated format such as "foo.bar"
	 * @return this builder
	 */
	public ConditionCallBuilder skipIfElementMissing(String objId, String path) {
		if (objId != null && path != null) {
			this.skipIfElementsMissing.add(new ImmutablePair<>(objId, path));
		}
		return this;
	}


	// getters

	/**
	 * Get the Condition to be called. May be null.
	 *
	 * @return the condition
	 */
	public Condition getCondition() {
		return condition;
	}

	/**
	 * Get the Condition class to be constructed and called. Can not be null.
	 *
	 * @return the condition class
	 */
	public Class<? extends Condition> getConditionClass() {
		return conditionClass;
	}

	/**
	 * Get the list of requirement strings associated with this construction call. Defaults to an empty list.
	 *
	 * @return the list of requirement strings
	 */
	public String[] getRequirements() {
		return requirements.toArray(new String[] {});
	}

	/**
	 * Get the result to log on condition failure. Defaults to FAILURE.
	 *
	 * @return the result to log on condition failure
	 */
	public ConditionResult getOnFail() {
		return onFail;
	}

	/**
	 * Get the result to log when the condition is skipped. Defaults to INFO.
	 *
	 * @return the result to log when the condition is skipped
	 */
	public ConditionResult getOnSkip() {
		return onSkip;
	}

	/**
	 * Get whether to stop the test if this condition call fails. Defaults to true.
	 *
	 * @return whether to stop the test on condition call failure
	 */
	public boolean isStopOnFailure() {
		return stopOnFailure;
	}

	/**
	 * If any of these objects are missing from the Environment, the condition call is skipped. Defaults to an empty list.
	 *
	 * @return the list of object references to search for
	 */
	public List<String> getSkipIfObjectsMissing() {
		return skipIfObjectsMissing;
	}

	/**
	 * If any of these strings are missing from the Environment, the condition call is skipped. Defaults to an empty list.
	 *
	 * @return the list of strings to search for
	 */
	public List<String> getSkipIfStringsMissing() {
		return skipIfStringsMissing;
	}

	/**
	 * If any of these longs are missing from the Environment, the condition call is skipped. Defaults to an empty list.
	 *
	 * @return the list of longs to search for
	 */
	public List<String> getSkipIfLongsMissing() {
		return skipIfLongsMissing;
	}

	/**
	 * If any of these elements are missing from the Environment, the condition call is skipped. Defaults to an empty list.
	 *
	 * @return the list of elements to search for
	 */
	public List<Pair<String,String>> getSkipIfElementsMissing() {
		return skipIfElementsMissing;
	}

}
