package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class PrepareAllLoansRelatedConsentsForHappyPathTest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject config = env.getObject("config");
		String baseUrl = env.getString("config", "resource.resourceUrl");
		env.putString("baseUrl", baseUrl);
		env.putString("protected_resource_url", baseUrl.concat("/contracts"));


		String[] permissions = {"LOANS_READ",
			"LOANS_SCHEDULED_INSTALMENTS_READ",
			"LOANS_WARRANTIES_READ",
			"LOANS_PAYMENTS_READ", "RESOURCES_READ" };
		env.putString("consent_permissions", String.join(" ", permissions));
		return env;
	}
}
