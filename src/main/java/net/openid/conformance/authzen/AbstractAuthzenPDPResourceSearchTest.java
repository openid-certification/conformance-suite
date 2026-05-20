package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import net.openid.conformance.authzen.condition.CreateAuthzenResourceSearchApiRequestSteps;
import net.openid.conformance.authzen.condition.EnsureValidResourceSearchResponse;
import net.openid.conformance.authzen.condition.EnsureValidSearchResponsePage;
import net.openid.conformance.authzen.condition.ExtractAuthzenApiEndpointSearchResponse;
import net.openid.conformance.authzen.condition.SetAuthzenApiEndpointToResourceSearchEndpoint;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.variant.PDPServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;

@VariantConfigurationFields(parameter = PDPServerMetadata.class, value = "static", configurationFields = {
	"pdp.search_resource_endpoint"
})

public abstract class AbstractAuthzenPDPResourceSearchTest extends AbstractAuthzenPDPSearchTest {

	@Override
	protected ConditionSequence createAuthzenApiRequestSequence() {
		JsonObject request = parseRequest();
		return new CreateAuthzenResourceSearchApiRequestSteps(
			request.getAsJsonObject("subject"),
			request.getAsJsonObject("resource"),
			request.getAsJsonObject("action"),
			request.getAsJsonObject("context"),
			request.getAsJsonObject("page"));
	}

	@Override
	protected void setAuthzenApiEndpoint() {
		callAndStopOnFailure(SetAuthzenApiEndpointToResourceSearchEndpoint.class);
	}

	@Override
	protected void processAuthApiEndpointResponse() {
		callAndStopOnFailure(ExtractAuthzenApiEndpointSearchResponse.class, "AUTHZEN-8.3");
		callAndStopOnFailure(EnsureValidResourceSearchResponse.class, "AUTHZEN-8.3", "AUTHZEN-8.5");
		callAndStopOnFailure(EnsureValidSearchResponsePage.class, "AUTHZEN-8.2.2", "AUTHZEN-8.3");
	}
}
