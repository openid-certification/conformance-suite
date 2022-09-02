package net.openid.conformance.openinsurance.testmodule.patrimonial.v1;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VerifyBranch extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = {"branch", "resource_endpoint_response"})
	public Environment evaluate(Environment env) {

		JsonElement body = bodyFrom(env);

		String branchCode = env.getString("branch");
		PatrimonialBranches branch = findBranch(branchCode);
		if (branch == null) {
			throw error("Non-existent branch code saved in the environment.", args(branchCode));
		}

		JsonElement branchElement = findByPath(body, "$.data.insuredObjects.coverages.branch");

		if (branchElement.isJsonPrimitive() && branchElement.getAsString().equals(branch.getBranchCode())) {
			logSuccess(String.format("Successfully found a policyID of type %s", branch.name()));
			env.putBoolean("branch_found", true);
		}

		logFailure(String.format("PolicyId of type %s not found", branch.name()));
		env.putBoolean("branch_found", false);

 		return env;
	}

	private PatrimonialBranches findBranch(String branchCode) {

		for (PatrimonialBranches b : PatrimonialBranches.values()) {
			if (b.getBranchCode().equals(branchCode)) {
				return b;
			}
		}
		return null;
	}
}
