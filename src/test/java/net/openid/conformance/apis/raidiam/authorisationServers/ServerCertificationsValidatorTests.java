package net.openid.conformance.apis.raidiam.authorisationServers;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.authorisationServers.certifications.GetServerCertificationsByCertificationIdValidator;
import net.openid.conformance.raidiam.validators.authorisationServers.certifications.GetServerCertificationsValidator;
import net.openid.conformance.raidiam.validators.authorisationServers.certifications.PostServerCertificationsValidator;
import net.openid.conformance.raidiam.validators.authorisationServers.certifications.PutServerCertificationsByCertificationIdValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class ServerCertificationsValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/authorisationServers/certifications/GetServerCertificationsByCertificationIdResponse.json")
	public void validateGetServerCertificationsByCertificationIdValidator() {
		GetServerCertificationsByCertificationIdValidator condition = new GetServerCertificationsByCertificationIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/authorisationServers/certifications/GetServerCertificationsResponse.json")
	public void validateGetServerCertificationsValidator() {
		GetServerCertificationsValidator condition = new GetServerCertificationsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/authorisationServers/certifications/PostServerCertificationsResponse.json")
	public void validatePostServerCertificationsValidator() {
		PostServerCertificationsValidator condition = new PostServerCertificationsValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/authorisationServers/certifications/PutServerCertificationsByCertificationIdResponse.json")
	public void validatePutServerCertificationsByCertificationIdValidator() {
		PutServerCertificationsByCertificationIdValidator condition = new PutServerCertificationsByCertificationIdValidator();
		run(condition);
	}
}
