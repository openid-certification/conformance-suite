package net.openid.conformance.apis.raidiam.softwareStatements;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.softwareStatements.certificates.GetStatementCertificatesByKeyIdValidator;
import net.openid.conformance.raidiam.validators.softwareStatements.certificates.GetStatementCertificatesValidator;
import net.openid.conformance.raidiam.validators.softwareStatements.certificates.PostStatementCertificatesValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class StatementCertificatesValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/softwareStatements/certificates/GetStatementCertificatesByKeyIdResponse.json")
	public void validateGetStatementCertificatesByKeyIdValidator() {
		GetStatementCertificatesByKeyIdValidator condition = new GetStatementCertificatesByKeyIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/softwareStatements/certificates/GetStatementCertificatesResponse.json")
	public void validateGetStatementCertificatesValidator() {
		GetStatementCertificatesValidator condition = new GetStatementCertificatesValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/softwareStatements/certificates/PostStatementCertificatesResponse.json")
	public void validatePostStatementCertificatesValidator() {
		PostStatementCertificatesValidator condition = new PostStatementCertificatesValidator();
		run(condition);
	}
}
