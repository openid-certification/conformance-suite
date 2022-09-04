package net.openid.conformance.openid.client;

import net.openid.conformance.condition.rs.EnsureBearerAccessTokenNotInParams;
import net.openid.conformance.condition.rs.ExtractBearerAccessTokenFromHeader;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oidcc-client-test-userinfo-bearer-header",
	displayName = "OIDCC: Relying party test, pass the access token using Bearer authentication scheme",
	summary = "The client is expected to Pass the access token using the 'Bearer' authentication scheme while doing the UserInfo Request." +
		" Corresponds to rp-userinfo-bearer-header test in the old suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values={"id_token"})
public class OIDCCClientTestUserinfoBearerHeader extends AbstractOIDCCClientTest {

	@Override
	protected void extractBearerTokenFromUserinfoRequest() {
		callAndStopOnFailure(ExtractBearerAccessTokenFromHeader.class, "RFC6750-2");
		callAndStopOnFailure(EnsureBearerAccessTokenNotInParams.class, "RFC6750-2");
	}
}
