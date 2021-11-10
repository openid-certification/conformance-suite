package net.openid.conformance.apis.raidiam.referencesAuthorisationDomain;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.referencesAuthorisationDomain.GetAuthorisationDomainByDomainNameValidator;
import net.openid.conformance.raidiam.validators.referencesAuthorisationDomain.GetAuthorisationDomainValidator;
import net.openid.conformance.raidiam.validators.referencesAuthorisationDomain.PostAuthorisationDomainValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class AuthorisationDomainValidatorsTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/referencesAuthorisationDomain/GetAuthorisationDomainByDomainNameResponse.json")
	public void validateStructureGetAuthorisationDomainByDomainNameValidator() {
		GetAuthorisationDomainByDomainNameValidator condition = new GetAuthorisationDomainByDomainNameValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/referencesAuthorisationDomain/GetAuthorisationDomainResponse.json")
	public void validateGetAuthorisationDomainValidator() {
		GetAuthorisationDomainValidator condition = new GetAuthorisationDomainValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/referencesAuthorisationDomain/PostAuthorisationDomainResponse.json")
	public void validatePostAuthorisationDomainValidator() {
		PostAuthorisationDomainValidator condition = new PostAuthorisationDomainValidator();
		run(condition);
	}
}
