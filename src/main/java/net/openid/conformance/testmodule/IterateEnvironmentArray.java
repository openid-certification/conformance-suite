package net.openid.conformance.testmodule;

import com.google.common.base.Strings;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.sequence.ConditionSequence;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A test execution unit that iterates over an environment array and runs a sub-sequence once per element.
 *
 * <p>The keys configured via {@link #currentString}, {@link #currentElement},
 * {@link #iterationIndex} and {@link #iterationCount} are written on every iteration and removed
 * from the environment when the loop finishes (including on early exit via exception), so per-iteration
 * state does not leak into subsequent steps. Callers that want to capture the last iteration's value
 * should read it inside the body before the loop ends. For an empty array no keys are written and
 * no cleanup is performed.
 */
public class IterateEnvironmentArray implements TestExecutionUnit {

	private final String sourceObject;
	private final String sourcePath;
	private final ConditionSequenceCallBuilder sequenceCallBuilder;

	private String currentElementObject;
	private String currentElementPath;
	private String currentStringKey;
	private String iterationIndexKey;
	private String iterationCountKey;
	private Function<IterationContext, String> logBlockLabelBuilder;

	public IterateEnvironmentArray(String sourceObject, String sourcePath, Class<? extends ConditionSequence> conditionSequenceClass) {
		this(sourceObject, sourcePath, new ConditionSequenceCallBuilder(conditionSequenceClass));
	}

	public IterateEnvironmentArray(String sourceObject, String sourcePath, Supplier<? extends ConditionSequence> conditionSequenceConstructor) {
		this(sourceObject, sourcePath, new ConditionSequenceCallBuilder(conditionSequenceConstructor));
	}

	private IterateEnvironmentArray(String sourceObject, String sourcePath, ConditionSequenceCallBuilder sequenceCallBuilder) {
		assert sourceObject != null;
		assert sourcePath != null;
		assert sequenceCallBuilder != null;
		this.sourceObject = sourceObject;
		this.sourcePath = sourcePath;
		this.sequenceCallBuilder = sequenceCallBuilder;
	}

	public IterateEnvironmentArray currentElement(String envObjectKey, String envPath) {
		this.currentElementObject = envObjectKey;
		this.currentElementPath = envPath;
		return this;
	}

	public IterateEnvironmentArray currentString(String envStringKey) {
		this.currentStringKey = envStringKey;
		return this;
	}

	public IterateEnvironmentArray iterationIndex(String envIntegerKey) {
		this.iterationIndexKey = envIntegerKey;
		return this;
	}

	public IterateEnvironmentArray iterationCount(String envIntegerKey) {
		this.iterationCountKey = envIntegerKey;
		return this;
	}

	public IterateEnvironmentArray logBlockLabels(Function<IterationContext, String> logBlockLabelBuilder) {
		this.logBlockLabelBuilder = logBlockLabelBuilder;
		return this;
	}

	public String getSourceObject() {
		return sourceObject;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public ConditionSequenceCallBuilder getSequenceCallBuilder() {
		return sequenceCallBuilder;
	}

	public void prepareIteration(Environment env, JsonElement element, int iterationIndex, int iterationCount) {
		if (!Strings.isNullOrEmpty(currentElementObject) && !Strings.isNullOrEmpty(currentElementPath)) {
			putElement(env, currentElementObject, currentElementPath, element.deepCopy());
		}
		if (!Strings.isNullOrEmpty(currentStringKey)) {
			if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
				throw new IllegalArgumentException("Current iteration element is not a string");
			}
			env.putString(currentStringKey, OIDFJSON.getString(element));
		}
		if (!Strings.isNullOrEmpty(iterationIndexKey)) {
			env.putInteger(iterationIndexKey, iterationIndex + 1);
		}
		if (!Strings.isNullOrEmpty(iterationCountKey)) {
			env.putInteger(iterationCountKey, iterationCount);
		}
	}

	public String getLogBlockLabel(JsonElement element, int iterationIndex, int iterationCount) {
		if (logBlockLabelBuilder == null) {
			return null;
		}
		return logBlockLabelBuilder.apply(new IterationContext(element.deepCopy(), iterationIndex + 1, iterationCount));
	}

	/**
	 * Remove iteration-state keys from the environment. Invoked once after the loop completes
	 * (or on early exit via exception); not invoked for empty arrays since no keys were written.
	 */
	public void cleanupAfterIteration(Environment env, int iterationCount) {
		if (iterationCount == 0) {
			return;
		}
		if (!Strings.isNullOrEmpty(currentElementObject) && !Strings.isNullOrEmpty(currentElementPath)) {
			env.removeElement(currentElementObject, currentElementPath);
		}
		if (!Strings.isNullOrEmpty(currentStringKey)) {
			env.removeNativeValue(currentStringKey);
		}
		if (!Strings.isNullOrEmpty(iterationIndexKey)) {
			env.removeNativeValue(iterationIndexKey);
		}
		if (!Strings.isNullOrEmpty(iterationCountKey)) {
			env.removeNativeValue(iterationCountKey);
		}
	}

	private void putElement(Environment env, String key, String path, JsonElement value) {
		if (Strings.isNullOrEmpty(key) || Strings.isNullOrEmpty(path)) {
			throw new IllegalArgumentException("Environment key and path must both be set");
		}

		JsonObject o = env.getObject(key);
		if (o == null) {
			o = new JsonObject();
			env.putObject(key, o);
		}

		ArrayList<String> pathSegments = Lists.newArrayList(Splitter.on('.').split(path));
		int lastIndex = pathSegments.size() - 1;
		String lastSegment = pathSegments.get(lastIndex);
		pathSegments.remove(lastIndex);

		for (String pathSegment : pathSegments) {
			JsonElement next = o.get(pathSegment);
			if (next == null) {
				next = new JsonObject();
				o.add(pathSegment, next);
			} else if (!next.isJsonObject()) {
				throw new IllegalArgumentException("Non-object value found while writing iteration element to " + key + "." + path);
			}
			o = next.getAsJsonObject();
		}

		o.add(lastSegment, value);
	}

	public static final class IterationContext {
		private final JsonElement element;
		private final int iteration;
		private final int iterationCount;

		private IterationContext(JsonElement element, int iteration, int iterationCount) {
			this.element = element;
			this.iteration = iteration;
			this.iterationCount = iterationCount;
		}

		public JsonElement getElement() {
			return element;
		}

		public int getIteration() {
			return iteration;
		}

		public int getIterationCount() {
			return iterationCount;
		}
	}
}
