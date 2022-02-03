package net.openid.conformance.apis.raidiam.organisations;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.raidiam.validators.organisations.GetOrganisationsByOrganisationIdTnchistoryValidator;
import net.openid.conformance.raidiam.validators.organisations.GetOrganisationsByOrganisationIdValidator;
import net.openid.conformance.raidiam.validators.organisations.GetOrganisationsEssPollByEnvelopeIdValidator;
import net.openid.conformance.raidiam.validators.organisations.GetOrganisationsEssPollValidator;
import net.openid.conformance.raidiam.validators.organisations.GetOrganisationsValidator;
import net.openid.conformance.raidiam.validators.organisations.PostOrganisationsByOrganisationIdInitiatesigningValidator;
import net.openid.conformance.raidiam.validators.organisations.PostOrganisationsValidator;
import net.openid.conformance.raidiam.validators.organisations.PutOrganisationsByOrganisationIdValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class OrganisationsValidatorsTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/organisations/GetOrganisationsResponse.json")
	public void validateStructureGetOrganisationsValidator() {
		GetOrganisationsValidator condition = new GetOrganisationsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisations/PostOrganisationsResponse.json")
	public void validateStructurePostOrganisationsValidator() {
		PostOrganisationsValidator condition = new PostOrganisationsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisations/GetOrganisationsEssPollByEnvelopeIdResponse.json")
	public void validateStructureGetOrganisationsEssPollByEnvelopeIdValidator() {
		GetOrganisationsEssPollByEnvelopeIdValidator condition = new GetOrganisationsEssPollByEnvelopeIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisations/GetOrganisationsEssPollResponse.json")
	public void validateStructureGetOrganisationsEssPollValidator() {
		GetOrganisationsEssPollValidator condition = new GetOrganisationsEssPollValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisations/PutOrganisationsByOrganisationIdResponse.json")
	public void validateStructurePutOrganisationsByOrganisationIdValidator() {
		PutOrganisationsByOrganisationIdValidator condition = new PutOrganisationsByOrganisationIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisations/GetOrganisationsByOrganisationIdResponse.json")
	public void validateStructureGetOrganisationsByOrganisationIdValidator() {
		GetOrganisationsByOrganisationIdValidator condition = new GetOrganisationsByOrganisationIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisations/ GetOrganisationsByOrganisationIdTnchistoryResponse.json")
	public void validateStructureGetOrganisationsByOrganisationIdTnchistoryValidator() {
		GetOrganisationsByOrganisationIdTnchistoryValidator condition = new GetOrganisationsByOrganisationIdTnchistoryValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisations/PostOrganisationsByOrganisationIdInitiatesigningResponse.json")
	public void validateStructurePostOrganisationsByOrganisationIdInitiatesigningValidator() {
		PostOrganisationsByOrganisationIdInitiatesigningValidator condition = new PostOrganisationsByOrganisationIdInitiatesigningValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisations/GetOrganisationsResponse_maxLengthError.json")
	public void validateStructureWithWrongMaxLength() {
		GetOrganisationsValidator condition = new GetOrganisationsValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("OrganisationId",
			condition.getApiName())));
	}
}
