package net.openid.conformance.openbanking_brasil.testmodules.creditOperations;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import netscape.javascript.JSObject;

public class PrepareAllCreditOperationsPermissionsForHappyPath  extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "consent_permissions")
	public Environment evaluate(Environment env) {
		String[] permissions = {"LOANS_READ", "LOANS_WARRANTIES_READ",
			"LOANS_SCHEDULED_INSTALMENTS_READ", "LOANS_PAYMENTS_READ",
			"FINANCINGS_READ", "FINANCINGS_WARRANTIES_READ",
			"FINANCINGS_SCHEDULED_INSTALMENTS_READ", "FINANCINGS_PAYMENTS_READ",
			"UNARRANGED_ACCOUNTS_OVERDRAFT_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ",
			"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ", "UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ",
			"INVOICE_FINANCINGS_READ", "INVOICE_FINANCINGS_WARRANTIES_READ",
			"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ", "INVOICE_FINANCINGS_PAYMENTS_READ",
			"RESOURCES_READ"};
		JsonObject consentPermission = env.getObject("consent_endpoint_request");
		JsonArray permissionArray = new JsonArray();
		for(String permission : permissions){
			permissionArray.add(permission);
		}
		consentPermission.getAsJsonObject("data").add("permissions", permissionArray);
		env.putString("consent_permissions", String.join(" ", permissions));
		log(consentPermission);
		logSuccess("Set all permissions for credit operations", args("permissions", permissions));
		return env;
	}
}
