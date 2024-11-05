package net.openid.conformance.openid.federation;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.HashSet;
import java.util.Set;

public class ValidateEntityStatementMetadataPolicy extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "federation_response_jwt" } )
	public Environment evaluate(Environment env) {

		JsonElement metadataPolicyClaim = env.getElementFromObject("federation_response_jwt", "claims.metadata_policy");
		if (metadataPolicyClaim == null) {
			logSuccess("Entity statement does not contain the metadata claim");
			return env;
		}

		JsonObject metadataPolicy = metadataPolicyClaim.getAsJsonObject();

		Set<String> validEntityTypes = ImmutableSet.of(
			"federation_entity",
			"openid_relying_party",
			"openid_provider",
			"oauth_authorization_server",
			"oauth_client",
			"oauth_resource"
		);

		Set<String> entityTypes = metadataPolicy.keySet();
		Set<String> difference = new HashSet<>(entityTypes);
		difference.removeAll(validEntityTypes);
		if (!difference.isEmpty()) {
			throw error("The metadata policy claim contains invalid entity types", args("expected", validEntityTypes, "actual", entityTypes));
		}

		for (String entityType : entityTypes) {
			JsonObject parameters = metadataPolicy.getAsJsonObject(entityType);
			for (String parameterName : parameters.keySet()) {
				JsonObject parameter = parameters.getAsJsonObject(parameterName);
				for (String operatorName : parameter.keySet()) {
					JsonElement operatorValue = parameter.get(operatorName);
					validateOperatorValue(operatorName, operatorValue);
				}
			}
		}

		logSuccess("Entity statement contains a valid metadata policy claim", args("metadata", metadataPolicy));
		return env;
	}

	protected void validateOperatorValue(String operatorName, JsonElement operatorValue) {
		switch (operatorName) {
			case "value":
				validateValueOperator(operatorValue);
				break;
			case "add":
				validateAddOperator(operatorValue);
				break;
			case "default":
				validateDefaultOperator(operatorValue);
				break;
			case "one_of":
				validateOneOfOperator(operatorValue);
				break;
			case "subset_of":
				validateSubsetOfOperator(operatorValue);
				break;
			case "superset_of":
				validateSupersetOfOperator(operatorValue);
				break;
			case "essential":
				validateEssentialOperator(operatorValue);
				break;
			default:
				throw error("Invalid operator name", args("expected", ImmutableSet.of(
					"value",
					"add",
					"default",
					"one_of",
					"subset_of",
					"superset_of",
					"essential"
				), "actual", operatorName));
		}
	}

	protected void validateValueOperator(JsonElement operatorValue) {
		if (isStringOrNumberOrBoolean(operatorValue) || isObjectOrArray(operatorValue) || isNull(operatorValue)) {
			return;
		}
		throw error("The value operator value must be one of string, number, boolean, object, array, null", args("actual", operatorValue));
	}

	protected void validateAddOperator(JsonElement operatorValue) {
		if (operatorValue.isJsonArray() && (
			isArrayOfStrings(operatorValue.getAsJsonArray()) ||
			isArrayOfNumbers(operatorValue.getAsJsonArray()) ||
			isArrayOfObjects(operatorValue.getAsJsonArray())
		)) {
			return;
		}
		throw error("The add operator value must be one of array of strings, array of numbers, array of objects", args("add", operatorValue));
	}

	protected void validateDefaultOperator(JsonElement operatorValue) {
		if (isStringOrNumberOrBoolean(operatorValue) || isObjectOrArray(operatorValue)) {
			return;
		}
		throw error("The default operator value must be one of string, number, boolean, object, array", args("default", operatorValue));
	}

	protected void validateOneOfOperator(JsonElement operatorValue) {
		if (operatorValue.isJsonArray() && (
			isArrayOfStrings(operatorValue.getAsJsonArray()) ||
				isArrayOfNumbers(operatorValue.getAsJsonArray()) ||
				isArrayOfObjects(operatorValue.getAsJsonArray())
		)) {
			return;
		}
		throw error("The one_of operator value must be one of array of strings, array of numbers, array of objects", args("one_of", operatorValue));
	}

	protected void validateSubsetOfOperator(JsonElement operatorValue) {
		if (operatorValue.isJsonArray() && (
			isArrayOfStrings(operatorValue.getAsJsonArray()) ||
				isArrayOfNumbers(operatorValue.getAsJsonArray()) ||
				isArrayOfObjects(operatorValue.getAsJsonArray())
		)) {
			return;
		}
		throw error("The subset_of operator value must be one of array of strings, array of numbers, array of objects", args("subset_of", operatorValue));
	}

	protected void validateSupersetOfOperator(JsonElement operatorValue) {
		if (operatorValue.isJsonArray() && (
			isArrayOfStrings(operatorValue.getAsJsonArray()) ||
				isArrayOfNumbers(operatorValue.getAsJsonArray()) ||
				isArrayOfObjects(operatorValue.getAsJsonArray())
		)) {
			return;
		}
		throw error("The superset_of operator value must be one of array of strings, array of numbers, array of objects", args("superset_of", operatorValue));
	}

	protected void validateEssentialOperator(JsonElement operatorValue) {
		if(operatorValue.isJsonPrimitive() && operatorValue.getAsJsonPrimitive().isBoolean()) {
			return;
		}
		throw error("The essential operator value must be a boolean", args("essential", operatorValue));
	}

	protected boolean isStringOrNumberOrBoolean(JsonElement element) {
		return element.isJsonPrimitive() && (
			element.getAsJsonPrimitive().isString() ||
				element.getAsJsonPrimitive().isNumber() ||
				element.getAsJsonPrimitive().isBoolean()
		);
	}

	protected boolean isObjectOrArray(JsonElement element) {
		return element.isJsonObject() || element.isJsonArray();
	}

	protected boolean isNull(JsonElement element) {
		return element != null && element.isJsonNull();
	}

	protected boolean isArrayOfStrings(JsonArray array) {
		for (JsonElement element : array) {
			boolean isString = element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
			if (!isString) {
				return false;
			}
		}
		return true;
	}

	protected boolean isArrayOfNumbers(JsonArray array) {
		for (JsonElement element : array) {
			boolean isNumber = element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber();
			if (!isNumber) {
				return false;
			}
		}
		return true;
	}

	protected boolean isArrayOfObjects(JsonArray array) {
		for (JsonElement element : array) {
			boolean isObject = element.isJsonObject();
			if (!isObject) {
				return false;
			}
		}
		return true;
	}


}
