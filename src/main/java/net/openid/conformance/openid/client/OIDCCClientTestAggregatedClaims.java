package net.openid.conformance.openid.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.rs.OIDCCLoadUserInfoWithAggregatedClaims;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

/**
 * rp-claims-aggregated in the old python test suite
 */
@PublishTestModule(
	testName = "oidcc-client-test-aggregated-claims",
	displayName = "OIDCC: Relying party test, aggregated claims",
	summary = "The client is expected to make an authentication request " +
		"(also a token request where applicable) and a userinfo request " +
		"using the selected response_type and other configuration options " +
		"and process a userinfo response or id_token with aggregated claims." +
		"(This test supports only address and phone_number claims, and always returns " +
		"them regardless of requested scopes)",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values = {"id_token"})
public class OIDCCClientTestAggregatedClaims extends AbstractOIDCCClientTest {
	@Override
	protected void configureUserInfo() {
		callAndStopOnFailure(OIDCCLoadUserInfoWithAggregatedClaims.class);
	}

	@Override
	protected JsonObject prepareUserinfoResponse() {
		JsonObject user = env.getObject("user_info");
		env.putObject("user_info_endpoint_response", user);
		return user;
	}

}
