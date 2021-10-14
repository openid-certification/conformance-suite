package net.openid.conformance.ekyc.test.oidccore;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ekyc-server-happypath",
	displayName = "eKYC Happy Path Server Test",
	summary = "Request only one claim, selected from the list of claims_in_verified_claims_supported, " +
		"without requesting any other verification element and expect a happy path flow.",
	profile = "OIDCC",
	configurationFields = {
		"trust_framework",
		"verified_claim_names"
	}
)
public class EKYCHappyPathTest extends BaseEKYCTestWithOIDCCore {

}
