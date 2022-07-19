package net.openid.conformance.openbanking_brasil.testmodules.support;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonUtils;

import java.util.HashSet;
import java.util.Set;

public class EnsureSpecificCreditOperationsPermissionsWereReturned extends AbstractCondition {


	public enum CreditOperationsPermissionsType {
		LOAN, FINANCINGS, UNARRANGED_ACCOUNTS_OVERDRAFT, INVOICE_FINANCINGS;
	}

	private static final String[] LOANS_PERMISSIONS = {"LOANS_READ", "LOANS_WARRANTIES_READ", "LOANS_SCHEDULED_INSTALMENTS_READ", "LOANS_PAYMENTS_READ", "RESOURCES_READ"};
	private static final String[] FINANCINGS_PERMISSIONS = {"FINANCINGS_READ", "FINANCINGS_WARRANTIES_READ", "FINANCINGS_SCHEDULED_INSTALMENTS_READ", "FINANCINGS_PAYMENTS_READ", "RESOURCES_READ"};
	private static final String[] UNARRANGED_ACCOUNTS_OVERDRAFT_PERMISSIONS = {"UNARRANGED_ACCOUNTS_OVERDRAFT_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ", "RESOURCES_READ"};
	private static final String[] INVOICE_FINANCINGS_PERMISSIONS = {"INVOICE_FINANCINGS_READ", "INVOICE_FINANCINGS_WARRANTIES_READ", "INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ", "INVOICE_FINANCINGS_PAYMENTS_READ", "RESOURCES_READ"};

	private String[] permissionGroup;

	boolean isPermissionGroupSubsetOfJsonArray(JsonArray supersetJson) {

		Set<String> superset = new HashSet<>();
		supersetJson.forEach(e -> superset.add(OIDFJSON.getString(e)));

		return superset.containsAll(Set.of(permissionGroup));
	}

	@Override
	@PreEnvironment(required = "consent_endpoint_response_full", strings = "permission_type")
	public Environment evaluate(Environment env) {

		switch (CreditOperationsPermissionsType.valueOf(env.getString("permission_type"))) {
			case LOAN:
				permissionGroup = LOANS_PERMISSIONS;
				break;
			case FINANCINGS:
				permissionGroup = FINANCINGS_PERMISSIONS;
				break;
			case UNARRANGED_ACCOUNTS_OVERDRAFT:
				permissionGroup = UNARRANGED_ACCOUNTS_OVERDRAFT_PERMISSIONS;
				break;
			case INVOICE_FINANCINGS:
				permissionGroup = INVOICE_FINANCINGS_PERMISSIONS;
				break;
		}
		String bodyJsonString = env.getString("consent_endpoint_response_full", "body");
		JsonObject body;
		try {
			Gson gson = JsonUtils.createBigDecimalAwareGson();
			body = gson.fromJson(bodyJsonString, JsonObject.class);
		} catch (JsonSyntaxException e) {
			throw error("Body is not JSON object", e, args("Body", bodyJsonString));
		}

		if (body == null) {
			throw error("body element is missing in the consent_endpoint_response_full");
		}

		JsonObject data = body.getAsJsonObject("data");
		if (data == null) {
			throw error("data element is missing in the body", args("body", body));
		}

		JsonArray grantedPermissions = data.getAsJsonArray("permissions");

		if (grantedPermissions == null) {
			throw error("permissions element is missing in the data", args("data", data));
		}
		if (!grantedPermissions.isJsonArray()) {
			throw error("permissions element in the consent response is not a JSON array", args("permissions", grantedPermissions));
		}

		if (grantedPermissions.size() <= 0) {
			throw error("permissions element in the consent response is an empty array", args("permissions", grantedPermissions));
		}

		if (!isPermissionGroupSubsetOfJsonArray(grantedPermissions)) {
			throw error("Consent endpoint response does not contain expected permissions", args("granted", grantedPermissions, "expected", permissionGroup));
		}

		logSuccess("Consent endpoint response contains expected permissions", args("granted", grantedPermissions, "expected", permissionGroup));

		return env;
	}
}
