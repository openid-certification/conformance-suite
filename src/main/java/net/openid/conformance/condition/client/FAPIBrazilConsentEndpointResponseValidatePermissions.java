package net.openid.conformance.condition.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.*;

public class FAPIBrazilConsentEndpointResponseValidatePermissions extends AbstractCondition {

	private static final String[] personalRegistrationData = {"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ","RESOURCES_READ"};
	private static final String[] personalAdditionalInfo = {"CUSTOMERS_PERSONAL_ADITTIONALINFO_READ", "RESOURCES_READ"};
	private static final String[] businessRegistrationData = {"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ", "RESOURCES_READ"};
	private static final String[] businessAdditionalInfo = {"CUSTOMERS_BUSINESS_ADITTIONALINFO_READ", "RESOURCES_READ"};
	private static final String[] balances = {"ACCOUNTS_READ", "ACCOUNTS_BALANCES_READ", "RESOURCES_READ"};
	private static final String[] limits = {"ACCOUNTS_READ", "ACCOUNTS_OVERDRAFT_LIMITS_READ", "RESOURCES_READ"};
	private static final String[] extras = {"ACCOUNTS_READ", "ACCOUNTS_TRANSACTIONS_READ", "RESOURCES_READ"};
	private static final String[] creditCardLimits = {"CREDIT_CARDS_ACCOUNTS_READ", "CREDIT_CARDS_ACCOUNTS_LIMITS_READ", "RESOURCES_READ"};
	private static final String[] creditCardTransactions = {"CREDIT_CARDS_ACCOUNTS_READ", "CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ", "RESOURCES_READ"};
	private static final String[] creditCardInvoices = {"CREDIT_CARDS_ACCOUNTS_READ", "CREDIT_CARDS_ACCOUNTS_BILLS_READ", "CREDIT_CARDS_ACCOUNTS_BILLS_TRANSACTIONS_READ", "RESOURCES_READ"};
	private static final String[] creditOperationsContractData = {"LOANS_READ", "LOANS_WARRANTIES_READ", "LOANS_SCHEDULED_INSTALMENTS_READ", "LOANS_PAYMENTS_READ", "FINANCINGS_READ", "FINANCINGS_WARRANTIES_READ", "FINANCINGS_SCHEDULED_INSTALMENTS_READ", "FINANCINGS_PAYMENTS_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ", "INVOICE_FINANCINGS_READ", "INVOICE_FINANCINGS_WARRANTIES_READ", "INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ", "INVOICE_FINANCINGS_PAYMENTS_READ", "RESOURCES_READ"};
	private static final String[][] permissionGroups = {personalRegistrationData, personalAdditionalInfo, businessRegistrationData, businessAdditionalInfo, balances, limits, extras, creditCardLimits, creditCardTransactions, creditCardInvoices, creditOperationsContractData};

	boolean jsonArraysIsSubset(JsonArray supersetJson, JsonArray subsetJson) {

		Set<String> superset = new HashSet<>();
		supersetJson.forEach(e -> superset.add(OIDFJSON.getString(e)));

		Set<String> subset = new HashSet<>();
		subsetJson.forEach(e -> subset.add(OIDFJSON.getString(e)));

		return superset.containsAll(subset);
	}

	@Override
	@PreEnvironment(required = { "consent_endpoint_response", "brazil_consent" })
	public Environment evaluate(Environment env) {
		String path = "data.permissions";

		JsonElement grantedPermissionsEl = env.getElementFromObject("consent_endpoint_response", path);
		if (grantedPermissionsEl == null) {
			throw error("Couldn't find "+path+" in the consent response");
		}
		if (!grantedPermissionsEl.isJsonArray()) {
			throw error(path+" in the consent response is not a JSON array", args("permissions", grantedPermissionsEl));
		}
		JsonArray grantedPermissions = (JsonArray) grantedPermissionsEl;
		if (grantedPermissions.size() <= 0) {
			throw error(path+" in the consent response is an empty array", args("permissions", grantedPermissionsEl));
		}

		JsonArray requestedPermissions = (JsonArray) env.getElementFromObject("brazil_consent", "requested_permissions");

		if (!jsonArraysIsSubset(requestedPermissions,grantedPermissions)) {
			throw error("Consent endpoint response contains different permissions than requested", args("granted", grantedPermissionsEl, "requested", requestedPermissions));
		}

		if (!isValidResourceGroup(grantedPermissions)) {
			throw error("Consent endpoint response not a valid permissions grouping");
		}

		logSuccess("Consent endpoint response contains expected permissions", args("granted", grantedPermissionsEl, "requested", requestedPermissions));

		return env;
	}

	private boolean isValidResourceGroup(JsonArray grantedPermissions) {
		Gson gson = new Gson();

		String[] grantedPermissionsArray = gson.fromJson(grantedPermissions, String[].class);
		List<String> grantedPermissionsList = Arrays.asList(grantedPermissionsArray);
		TreeSet<String> grantedPermissionsSet = new TreeSet<>();
		grantedPermissionsSet.addAll(grantedPermissionsList);

		TreeSet<String> permissionsInCompleteGroup = new TreeSet<>();
		for (String[] permissionGroup : permissionGroups){
			TreeSet<String> permGroup = new TreeSet<>();
			permGroup.addAll(Arrays.asList(permissionGroup));

			if(grantedPermissionsSet.equals(permGroup)){
				return true;
			}
			if(grantedPermissionsSet.containsAll(permGroup)){
				permissionsInCompleteGroup.addAll(permGroup);
			}
			if(permissionsInCompleteGroup.equals(grantedPermissionsSet)){
				return true;
			}
		}
		return false;
	}

}
