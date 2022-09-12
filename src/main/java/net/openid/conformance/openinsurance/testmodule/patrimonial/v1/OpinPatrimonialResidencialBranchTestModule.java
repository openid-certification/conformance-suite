package net.openid.conformance.openinsurance.testmodule.patrimonial.v1;

import com.google.gson.JsonObject;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.testmodule.PublishTestModule;


@PublishTestModule(
	testName = "opin-patrimonial-residencial-api-branch-test",
	displayName = "Validates if at least one Policy Id returned is from “Compreensivo Residencial” branch",
	summary = "Validates if at least one Policy Id returned is from “Compreensivo Residencial” branch\n" +
		"\u2022 Creates a consent with all the permissions needed to access the Patrimonial API (“DAMAGES_AND_PEOPLE_PATRIMONIAL_READ”, “DAMAGES_AND_PEOPLE_PATRIMONIAL_POLICYINFO_READ”, “DAMAGES_AND_PEOPLE_PATRIMONIAL_PREMIUM_READ”, “DAMAGES_AND_PEOPLE_PATRIMONIAL_CLAIM_READ”,  “RESOURCES_READ”) \n" +
		"\u2022 Expects 201 - Expects Success on Redirect\n" +
		"\u2022 Calls GET Patrimonial “/” API\n" +
		"\u2022 Expects 201 - Loops through  of the Policy IDs returned\n" +
		"\u2022 Calls GET Patrimonial policy-Info API with each Policy ID\n" +
		"\u2022 Expects 200 - Validate if at least one of the branch object on the response body has the value of  “0114” (Compreensivo Residencial)\n" +
		"\u2022 Return success if at least one of the policy IDs is of “Compreensivo Residencial”, return an warning otherwise",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.brazilCnpj",
		"consent.productType"
	}
)

public class OpinPatrimonialResidencialBranchTestModule extends AbstractOpinPatrimonialBranchTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		setBranch(PatrimonialBranches.COMPREENSIVO_RESIDENCIAL);
		super.onConfigure(config, baseUrl);
	}
}
