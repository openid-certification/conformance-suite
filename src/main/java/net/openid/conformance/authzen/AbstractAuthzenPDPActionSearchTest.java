package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import net.openid.conformance.authzen.condition.CreateAuthzenActionSearchApiRequestSteps;
import net.openid.conformance.authzen.condition.SetAuthzenApiEndpointToActionSearchEndpoint;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.variant.PDPServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;

@VariantConfigurationFields(parameter = PDPServerMetadata.class, value = "static", configurationFields = {
	"pdp.search_action_endpoint"
})

public abstract class AbstractAuthzenPDPActionSearchTest extends AbstractAuthzenPDPSearchTest {

	@Override
	protected ConditionSequence createAuthzenApiRequestSequence() {
		JsonObject request = parseRequest();
		return new CreateAuthzenActionSearchApiRequestSteps(
			request.getAsJsonObject("subject"),
			request.getAsJsonObject("resource"),
			request.getAsJsonObject("context"),
			request.getAsJsonObject("page"));
	}

	@Override
	protected void setAuthzenApiEndpoint() {
		callAndStopOnFailure(SetAuthzenApiEndpointToActionSearchEndpoint.class);
	}


}
