package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import net.openid.conformance.authzen.condition.CreateAuthzenSubjectSearchApiRequestSteps;
import net.openid.conformance.authzen.condition.EnsureSearchResponsePageIsFirstKey;
import net.openid.conformance.authzen.condition.EnsureSearchResponsePageCountMatchesResults;
import net.openid.conformance.authzen.condition.EnsureSearchResponseResultsWithinPageLimit;
import net.openid.conformance.authzen.condition.EnsureSearchResponseHasPageAfterPageToken;
import net.openid.conformance.authzen.condition.EnsureValidSearchResponsePage;
import net.openid.conformance.authzen.condition.EnsureValidSubjectSearchResponse;
import net.openid.conformance.authzen.condition.ExtractAuthzenApiEndpointSearchResponse;
import net.openid.conformance.authzen.condition.SetAuthzenApiEndpointToSubjectSearchEndpoint;
import net.openid.conformance.authzen.condition.WarnAuthzenEntitySearchResponseUnknownMembers;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.variant.PDPServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;

@VariantConfigurationFields(parameter = PDPServerMetadata.class, value = "static", configurationFields = {
	"pdp.search_subject_endpoint"
})

public abstract class AbstractAuthzenPDPSubjectSearchTest extends AbstractAuthzenPDPSearchTest {

	@Override
	protected ConditionSequence createAuthzenApiRequestSequence() {
		JsonObject request = parseRequest();
		return new CreateAuthzenSubjectSearchApiRequestSteps(
			request.getAsJsonObject("subject"),
			request.getAsJsonObject("resource"),
			request.getAsJsonObject("action"),
			request.getAsJsonObject("context"),
			request.getAsJsonObject("page"));
	}

	@Override
	protected void setAuthzenApiEndpoint() {
		callAndStopOnFailure(SetAuthzenApiEndpointToSubjectSearchEndpoint.class);
	}

	@Override
	protected String getEndpointMetadataParameter() {
		return "search_subject_endpoint";
	}


	@Override
	protected void processAuthApiEndpointResponse() {
		callAndStopOnFailure(ExtractAuthzenApiEndpointSearchResponse.class, "AUTHZEN-8.3");
		callAndStopOnFailure(EnsureValidSubjectSearchResponse.class, "AUTHZEN-8.3", "AUTHZEN-8.4");
		callAndStopOnFailure(EnsureValidSearchResponsePage.class, "AUTHZEN-8.2.2", "AUTHZEN-8.3");
		callAndContinueOnFailure(EnsureSearchResponseHasPageAfterPageToken.class, getPageAfterPageTokenSeverity(), "AUTHZEN-8.2");
		callAndContinueOnFailure(EnsureSearchResponsePageCountMatchesResults.class, ConditionResult.FAILURE, "AUTHZEN-8.2.2");
		callAndContinueOnFailure(EnsureSearchResponseResultsWithinPageLimit.class, ConditionResult.FAILURE, "AUTHZEN-8.2.1");
		callAndContinueOnFailure(EnsureSearchResponsePageIsFirstKey.class, ConditionResult.WARNING, "AUTHZEN-8.3");
		callAndContinueOnFailure(WarnAuthzenEntitySearchResponseUnknownMembers.class, ConditionResult.WARNING, "AUTHZEN-8.3", "AUTHZEN-8.2.2", "AUTHZEN-5.1");
	}
}
