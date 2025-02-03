package net.openid.conformance.condition.rs;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class OIDCCLoadUserInfoWithDistributedClaims extends AbstractCondition {

	@Override
	@PostEnvironment(required = {"user_info", "distributed_claims"}, strings = {"distributed_claims_access_token"})
	public Environment evaluate(Environment env) {

		JsonObject user = new JsonObject();

		user.addProperty("sub", "user-subject-1234531");

		String accessToken = RandomStringUtils.secure().nextAlphanumeric(20);
		env.putString("distributed_claims_access_token", accessToken);

		JsonObject distClaims = new JsonObject();
		distClaims.addProperty("credit_score", 650);
		env.putObject("distributed_claims", distClaims);

		JsonObject claimNames = new JsonObject();
		claimNames.addProperty("credit_score", "src1");
		user.add("_claim_names", claimNames);

		JsonObject claimSources = new JsonObject();
		JsonObject claimEndpoint = new JsonObject();
		String baseUrl = env.getString("base_url");
		if (!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
		}
		claimEndpoint.addProperty("endpoint", baseUrl + "claims_endpoint");
		claimEndpoint.addProperty("access_token", accessToken);
		claimSources.add("src1", claimEndpoint);

		user.add("_claim_sources", claimSources);

		env.putObject("user_info", user);

		logSuccess("Added user information and distributed claims",
					args("user_info", user, "distributed_claims", distClaims, "claims_endpoint", claimEndpoint));

		return env;
	}

}
