package net.openid.conformance.apis.raidiam.softwareStatements;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.softwareStatements.organisation.GetStatementsForAnOrganisationByStatementIdValidator;
import net.openid.conformance.raidiam.validators.softwareStatements.organisation.GetStatementsForAnOrganisationValidator;
import net.openid.conformance.raidiam.validators.softwareStatements.organisation.PostStatementsForAnOrganisationValidator;
import net.openid.conformance.raidiam.validators.softwareStatements.organisation.PutStatementsForAnOrganisationByStatementIdValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

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
