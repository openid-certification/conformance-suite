package net.openid.conformance.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.networknt.schema.Error;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.validation.JsonSchemaValidation;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;
import net.openid.conformance.util.validation.JsonSchemaValidationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Base class for conditions that check for unknown/additional properties in schema-validated JSON.
 * Filters validation messages to only report additionalProperties errors, ignoring structural
 * errors (which are handled by the main validator condition).
 *
 * Subclasses may override {@link #getAllowUnexpectedFieldsConfigKey()} to let a test configuration
 * suppress the warning for specific known-extension property names that the schema does not (yet)
 * cover - an escape hatch for servers that legitimately advertise extension metadata.
 */
public abstract class AbstractCheckForUnexpectedSchemaProperties extends AbstractJsonSchemaBasedValidation {

	@Override
	protected void onValidationFailure(Environment env, JsonSchemaValidationResult validationResult, JsonSchemaValidationInput input) {
		JsonSchemaValidationResult additionalPropsResult = validationResult.onlyUnknownPropertyErrors();
		if (!additionalPropsResult.isValid()) {
			Set<String> ignored = getIgnoredPropertyNames(env);
			List<JsonObject> unknownProps = new ArrayList<>();
			for (Error msg : additionalPropsResult.getValidationMessages()) {
				if (ignored.contains(msg.getProperty())) {
					continue;
				}
				JsonObject entry = new JsonObject();
				entry.addProperty("property", msg.getProperty());
				entry.addProperty("path", JsonSchemaValidation.toInstancePropertyPath(msg.getInstanceLocation(), msg.getProperty()));
				unknownProps.add(entry);
			}
			if (!unknownProps.isEmpty()) {
				String configKey = getAllowUnexpectedFieldsConfigKey();
				String suppressHint = configKey != null
					? " If these are known extensions, add their names to the '" + configKey + "' array in the test configuration to suppress this warning."
					: "";
				throw error("Unknown properties were found in the " + input.getInputName()
						+ ". This may indicate the sender has misunderstood the spec, or it may be using extensions the test suite is unaware of."
						+ suppressHint
						+ " If they are derived from a specification, please open an issue at " + NEW_ISSUE_URL + " (or, if you are unable to, email " + SUPPORT_EMAIL + ") so the test suite can be updated.",
					args("unknown_properties", unknownProps, "input", input.getJsonObject(), "schema_link", "/" + input.getSchemaResource()));
			}
		}
	}

	/**
	 * Property names to treat as known (i.e. not warn about) even though they are absent from the
	 * schema. By default this reads {@link #getAllowUnexpectedFieldsConfigKey()} from the test
	 * configuration; subclasses can override for more complex behaviour.
	 */
	protected Set<String> getIgnoredPropertyNames(Environment env) {
		String configKey = getAllowUnexpectedFieldsConfigKey();
		return configKey == null ? Collections.emptySet() : readIgnoredPropertyNamesFromConfig(env, configKey);
	}

	/**
	 * The path within the {@code config} object of a JSON array of property names the tester wants
	 * treated as known (not warned about). This is a deliberately "hidden" configuration field - it
	 * is not declared in any {@code configurationFields}, so it does not appear on the schedule-test
	 * form, but a tester can still add it to the raw test configuration JSON as an escape hatch for
	 * extension metadata their server legitimately publishes. Returns {@code null} (no escape hatch)
	 * by default; subclasses override to enable it.
	 */
	protected String getAllowUnexpectedFieldsConfigKey() {
		return null;
	}

	/**
	 * Read a JSON array of property names from the given path within the {@code config} environment
	 * object, returning an empty set if it is missing or not an array.
	 */
	protected Set<String> readIgnoredPropertyNamesFromConfig(Environment env, String configPath) {
		Set<String> names = new LinkedHashSet<>();
		JsonElement el = env.getElementFromObject("config", configPath);
		if (el != null && el.isJsonArray()) {
			for (JsonElement item : el.getAsJsonArray()) {
				if (item.isJsonPrimitive()) {
					names.add(OIDFJSON.getString(item));
				}
			}
		}
		return names;
	}
}
