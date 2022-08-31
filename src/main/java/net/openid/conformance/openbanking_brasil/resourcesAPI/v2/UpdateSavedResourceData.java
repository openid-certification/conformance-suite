package net.openid.conformance.openbanking_brasil.resourcesAPI.v2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class UpdateSavedResourceData extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_data")
	public Environment evaluate(Environment env) {
		JsonObject resource = env.getObject("resource_data");
		String type = OIDFJSON.getString(resource.get("type"));

		String resourceApi;
		String resourceListEndpoint;
		String resourceEndpoint;
		String idType;
		String resourceId = OIDFJSON.getString(resource.get("resourceId"));

		switch (type) {
			case "ACCOUNT":
				resourceApi = "accounts";
				resourceListEndpoint = "/accounts";
				resourceEndpoint = "/balances";
				idType = "accountId";
				break;
			case "CREDIT_CARD_ACCOUNT":
				resourceApi = "credit-cards-accounts";
				resourceListEndpoint = "/accounts";
				resourceEndpoint = "/bills";
				idType = "creditCardAccountId";
				break;
			case "LOAN":
				resourceApi = "loans";
				resourceListEndpoint = "/contracts";
				resourceEndpoint = "/warranties";
				idType = "contractId";
				break;
			case "FINANCING":
				resourceApi = "financings";
				resourceListEndpoint = "/contracts";
				resourceEndpoint = "/warranties";
				idType = "contractId";
				break;
			case "UNARRANGED_ACCOUNT_OVERDRAFT":
				resourceApi = "unarranged-accounts-overdraft";
				resourceListEndpoint = "/contracts";
				resourceEndpoint = "/warranties";
				idType = "contractId";
				break;
			case "INVOICE_FINANCING":
				resourceApi = "invoice-financings";
				resourceListEndpoint = "/contracts";
				resourceEndpoint = "/warranties";
				idType = "contractId";
				break;
			default:
				throw error("Resource type does not exist.", args("resource type", type));
		}

		resourceEndpoint = resourceListEndpoint + "/" + resourceId + resourceEndpoint;

		resource.addProperty("resource_api", resourceApi);
		resource.addProperty("resource_list_endpoint", resourceListEndpoint);
		resource.addProperty("resource_endpoint", resourceEndpoint);
		resource.addProperty("id_type", idType);

		env.putObject("resource_data", resource);

		logSuccess("Resource data updated with required information for the next steps.", args("resource_data", resource));

		return env;
	}
}
