package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import net.openid.conformance.authzen.condition.CreateAuthzenActionSearchApiRequestSteps;
import net.openid.conformance.authzen.condition.EnsureSearchResponsePageIsFirstKey;
import net.openid.conformance.authzen.condition.EnsureValidActionSearchResponse;
import net.openid.conformance.authzen.condition.EnsureSearchResponsePageCountMatchesResults;
import net.openid.conformance.authzen.condition.EnsureSearchResponseResultsWithinPageLimit;
import net.openid.conformance.authzen.condition.EnsureSearchResponseHasPageAfterPageToken;
import net.openid.conformance.authzen.condition.EnsureValidSearchResponsePage;
import net.openid.conformance.authzen.condition.ExtractAuthzenApiEndpointSearchResponse;
import net.openid.conformance.authzen.condition.SetAuthzenApiEndpointToActionSearchEndpoint;
import net.openid.conformance.authzen.condition.WarnAuthzenActionSearchResponseUnknownMembers;
import net.openid.conformance.condition.Condition.ConditionResult;
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

	@Override
	protected String getEndpointMetadataParameter() {
		return "search_action_endpoint";
	}

	@Override
	protected void processAuthApiEndpointResponse() {
		callAndStopOnFailure(ExtractAuthzenApiEndpointSearchResponse.class, "AUTHZEN-8.3");
		callAndStopOnFailure(EnsureValidActionSearchResponse.class, "AUTHZEN-8.3", "AUTHZEN-8.6", "AUTHZEN-5.3");
		callAndStopOnFailure(EnsureValidSearchResponsePage.class, "AUTHZEN-8.2.2", "AUTHZEN-8.3");
		callAndContinueOnFailure(EnsureSearchResponseHasPageAfterPageToken.class, getPageAfterPageTokenSeverity(), "AUTHZEN-8.2", "AUTHZEN-8.2.2");
		callAndContinueOnFailure(EnsureSearchResponsePageCountMatchesResults.class, ConditionResult.FAILURE, "AUTHZEN-8.2.2");
		callAndContinueOnFailure(EnsureSearchResponseResultsWithinPageLimit.class, ConditionResult.FAILURE, "AUTHZEN-8.2.1");
		callAndContinueOnFailure(EnsureSearchResponsePageIsFirstKey.class, ConditionResult.WARNING, "AUTHZEN-8.3");
		callAndContinueOnFailure(WarnAuthzenActionSearchResponseUnknownMembers.class, ConditionResult.WARNING, "AUTHZEN-8.3", "AUTHZEN-8.2.2", "AUTHZEN-5.3");
	}
}
