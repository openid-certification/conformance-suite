package net.openid.conformance.apis.raidiam.referencesAuthorityAuthorisationDomain;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.referencesAuthorityAuthorisationDomain.GetAuthorityAuthorisationDomainByAuthorityIdValidator;
import net.openid.conformance.raidiam.validators.referencesAuthorityAuthorisationDomain.GetAuthorityAuthorisationDomainByDomainIdValidator;
import net.openid.conformance.raidiam.validators.referencesAuthorityAuthorisationDomain.GetAuthorityAuthorisationDomainValidator;
import net.openid.conformance.raidiam.validators.referencesAuthorityAuthorisationDomain.PostAuthorityAuthorisationDomainByAuthorityIdValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class AuthorityAuthorisationDomainValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/referencesAuthorityAuthorisationDomain/GetAuthorityAuthorisationDomainByAuthorityIdResponse.json")
	public void validateGetAuthorityAuthorisationDomainByAuthorityIdValidator() {
		GetAuthorityAuthorisationDomainByAuthorityIdValidator condition = new GetAuthorityAuthorisationDomainByAuthorityIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/referencesAuthorityAuthorisationDomain/ GetAuthorityAuthorisationDomainByDomainIdResponse.json")
	public void validateGetAuthorityAuthorisationDomainByDomainIdValidator() {
		GetAuthorityAuthorisationDomainByDomainIdValidator condition = new GetAuthorityAuthorisationDomainByDomainIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/referencesAuthorityAuthorisationDomain/GetAuthorityAuthorisationDomainResponse.json")
	public void validateGetAuthorityAuthorisationDomainValidator() {
		GetAuthorityAuthorisationDomainValidator condition = new GetAuthorityAuthorisationDomainValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/referencesAuthorityAuthorisationDomain/PostAuthorityAuthorisationDomainByAuthorityIdResponse.json")
	public void validatePostAuthorityAuthorisationDomainByAuthorityIdValidator() {
		PostAuthorityAuthorisationDomainByAuthorityIdValidator condition = new PostAuthorityAuthorisationDomainByAuthorityIdValidator();
		run(condition);
	}
}
