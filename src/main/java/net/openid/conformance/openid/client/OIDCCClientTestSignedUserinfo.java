package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.SetUserinfoSignedResponseAlgToRS256;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oidcc-client-test-userinfo-signed",
	displayName = "OIDCC: Relying party test, request and validate signed userinfo",
	summary = "The client is expected to make an authentication request " +
		"(also a token request where applicable) and a userinfo request " +
		"using the selected response_type and other configuration options and " +
		"userinfo_signed_response_alg RS256. ",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values={"id_token"})
public class OIDCCClientTestSignedUserinfo extends AbstractOIDCCClientTest {

	@Override
	protected void validateClientMetadata() {
		super.validateClientMetadata();
		callAndStopOnFailure(SetUserinfoSignedResponseAlgToRS256.class);
	}
}
