package net.openid.conformance.apis.raidiam.softwareStatements;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.raidiam.validators.softwareStatements.organisation.GetStatementsForAnOrganisationByStatementIdValidator;
import net.openid.conformance.raidiam.validators.softwareStatements.organisation.GetStatementsForAnOrganisationValidator;
import net.openid.conformance.raidiam.validators.softwareStatements.organisation.PostStatementsForAnOrganisationValidator;
import net.openid.conformance.raidiam.validators.softwareStatements.organisation.PutStatementsForAnOrganisationByStatementIdValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class StatementsForAnOrganisationValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/softwareStatements/organisation/GetStatementsForAnOrganisationByStatementIdResponse.json")
	public void validateGetStatementsForAnOrganisationByStatementIdValidator() {
		GetStatementsForAnOrganisationByStatementIdValidator condition = new GetStatementsForAnOrganisationByStatementIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/softwareStatements/organisation/GetStatementsForAnOrganisationResponse.json")
	public void validateGetStatementsForAnOrganisationValidator() {
		GetStatementsForAnOrganisationValidator condition = new GetStatementsForAnOrganisationValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/softwareStatements/organisation/GetStatementsForAnOrganisationResponse_maxLengthError.json")
	public void validateStructureWithWrongMaxLength() {
		GetStatementsForAnOrganisationValidator condition = new GetStatementsForAnOrganisationValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("ClientId",
			condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/raidiam/softwareStatements/organisation/PostStatementsForAnOrganisationResponse.json")
	public void validatePostStatementsForAnOrganisationValidator() {
		PostStatementsForAnOrganisationValidator condition = new PostStatementsForAnOrganisationValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/softwareStatements/organisation/PutStatementsForAnOrganisationByStatementIdResponse.json")
	public void validatePutStatementsForAnOrganisationByStatementIdValidator() {
		PutStatementsForAnOrganisationByStatementIdValidator condition = new PutStatementsForAnOrganisationByStatementIdValidator();
		run(condition);
	}
}
