package net.openid.conformance.apis.raidiam.organisationCertificates;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.organisationCertificates.GetCertificatesByCertificateOrKeyIdValidator;
import net.openid.conformance.raidiam.validators.organisationCertificates.GetCertificatesByCertificateTypeValidator;
import net.openid.conformance.raidiam.validators.organisationCertificates.GetCertificatesValidator;
import net.openid.conformance.raidiam.validators.organisationCertificates.PostCertificatesByCertificateTypeValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class OrganisationCertificatesValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/organisationCertificates/GetCertificatesByCertificateOrKeyIdResponse.json")
	public void validateGetCertificatesByCertificateOrKeyIdValidator() {
		GetCertificatesByCertificateOrKeyIdValidator condition = new GetCertificatesByCertificateOrKeyIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationCertificates/GetCertificatesByCertificateTypeResponse.json")
	public void validateGetCertificatesByCertificateTypeValidator() {
		GetCertificatesByCertificateTypeValidator condition = new GetCertificatesByCertificateTypeValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationCertificates/GetCertificatesResponse.json")
	public void validateGetCertificatesValidator() {
		GetCertificatesValidator condition = new GetCertificatesValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationCertificates/PostCertificatesByCertificateTypeResponse.json")
	public void validatePostCertificatesByCertificateTypeValidator() {
		PostCertificatesByCertificateTypeValidator condition = new PostCertificatesByCertificateTypeValidator();
		run(condition);
	}
}
