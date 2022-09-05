package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.EnsureRequestObjectWasSignedWithRS256;
import net.openid.conformance.condition.as.dynregistration.EnsureRequestObjectSigningAlgIsRS256InClientMetadata;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRequestType;
import net.openid.conformance.variant.VariantNotApplicable;


@PublishTestModule(
	testName = "oidcc-client-test-request-uri-signed-rs256",
	displayName = "OIDCC: Relying party test, request_uri support with RS256 signing algorithm",
	summary = "The client is expected to pass a request object by reference (and complete the entire flow), " +
		"using the request_uri parameter. The request object must be signed using 'RS256' algorithm." +
		" Corresponds to rp-request_uri-sig test in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = ClientRequestType.class, values={"plain_http_request", "request_object"})
public class OIDCCClientTestRequestUriSignedWithRS256 extends AbstractOIDCCClientTest {

	@Override
	protected ClientRequestType getEffectiveClientRequestTypeVariant() {
		return ClientRequestType.REQUEST_URI;
	}

	@Override
	protected void validateClientMetadata() {
		super.validateClientMetadata();
		callAndStopOnFailure(EnsureRequestObjectSigningAlgIsRS256InClientMetadata.class);
	}

	@Override
	protected void validateRequestObject() {
		super.validateRequestObject();
		callAndStopOnFailure(EnsureRequestObjectWasSignedWithRS256.class);
	}
}
