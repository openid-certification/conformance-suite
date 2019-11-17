package net.openid.conformance.openid.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.as.AddInvalidSubToUserInfoResponse;
import net.openid.conformance.condition.as.FilterUserInfoForScopes;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;


@PublishTestModule(
	testName = "oidcc-client-test-userinfo-invalid-sub",
	displayName = "OIDCC: Relying party test, sub in userinfo response does not match id_token",
	summary = "The client is expected to make a userinfo request " +
		" and verify the 'sub' value of the UserInfo Response by comparing it with the ID Token's 'sub' value." +
		" The client must identify the invalid 'sub' value and reject the UserInfo Response." +
		" Corresponds to rp-userinfo-bad-sub-claim test in the old suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values={"id_token"})
public class OIDCCClientTestInvalidSubInUserinfoResponse extends AbstractOIDCCClientTest {

	@Override
	protected JsonObject prepareUserinfoResponse() {
		super.prepareUserinfoResponse();
		callAndStopOnFailure(AddInvalidSubToUserInfoResponse.class);
		JsonObject user = env.getObject("user_info_endpoint_response");
		return user;
	}
}
