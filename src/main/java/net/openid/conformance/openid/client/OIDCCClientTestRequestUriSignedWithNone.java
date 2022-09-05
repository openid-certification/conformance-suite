package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.EnsureRequestObjectWasSignedWithNone;
import net.openid.conformance.condition.as.dynregistration.EnsureRequestObjectSigningAlgIsNoneInClientMetadata;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRequestType;
import net.openid.conformance.variant.VariantNotApplicable;


@PublishTestModule(
	testName = "oidcc-client-test-request-uri-signed-none",
	displayName = "OIDCC: Relying party test, request_uri support with 'none' signing algorithm",
	summary = "The client is expected to pass a request object by reference (and complete the entire flow), " +
		"using the request_uri parameter. The request object must be signed using 'none' algorithm." +
		" Corresponds to rp-request_uri-unsigned test in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = ClientRequestType.class, values={"plain_http_request", "request_object"})
public class OIDCCClientTestRequestUriSignedWithNone extends AbstractOIDCCClientTest {

	@Override
	protected ClientRequestType getEffectiveClientRequestTypeVariant() {
		return ClientRequestType.REQUEST_URI;
	}

	@Override
	protected void validateClientMetadata() {
		super.validateClientMetadata();
		callAndStopOnFailure(EnsureRequestObjectSigningAlgIsNoneInClientMetadata.class);
	}

	@Override
	protected void validateRequestObject() {
		super.validateRequestObject();
		callAndStopOnFailure(EnsureRequestObjectWasSignedWithNone.class);
	}
}
