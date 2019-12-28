package net.openid.conformance.openid.client;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantOverride;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
	testName = "oidcc-client-test-client-secret-basic",
	displayName = "OIDCC: Relying party test using client_secret_basic",
	summary = "The client MUST use client_secret_basic authentication method " +
		"regardless of selected client authentication type in test configuration." +
		"Corresponds to rp-token_endpoint-client_secret_basic in the old suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
/* applicable only when response type includes code */
@VariantNotApplicable(parameter = ResponseType.class, values = {"id_token token", "id_token"})
/* no need to run this test module if we're testing client_secret_basic for the whole test plan */
@VariantNotApplicable(parameter = ClientAuthType.class, values = {"client_secret_basic"})
/* this test module shows that the AS supports client_secret_basic, even if the test plan is for a different client
 * authentication type */
@VariantOverride(parameter = ClientAuthType.class, value = "client_secret_basic")
public class OIDCCClientTestClientSecretBasic extends AbstractOIDCCClientTest {

	/* Just run the standard client test, but expecting the client to use client_secret_basic as per the
	* @VariantOverride */

}
