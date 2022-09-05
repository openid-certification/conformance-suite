package net.openid.conformance.openid.client;

import net.openid.conformance.condition.rs.OIDCCExtractBearerAccessTokenFromBodyParams;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oidcc-client-test-userinfo-bearer-body",
	displayName = "OIDCC: Relying party test, pass the access token as form-encoded body parameter",
	summary = "The client is expected to pass the access token " +
		"as form-encoded body parameter while doing the UserInfo Request." +
		" Corresponds to rp-userinfo-bearer-body test in the old suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values={"id_token"})
public class OIDCCClientTestUserinfoBearerBody extends AbstractOIDCCClientTest {

	@Override
	protected void extractBearerTokenFromUserinfoRequest() {
		callAndStopOnFailure(OIDCCExtractBearerAccessTokenFromBodyParams.class, "RFC6750-2");
	}
}
