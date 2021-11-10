package net.openid.conformance.apis.raidiam.referencesAuthorisationDomainRole;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.referencesAuthorisationDomainRole.GetAuthorisationDomainRoleByDomainNameValidator;
import net.openid.conformance.raidiam.validators.referencesAuthorisationDomainRole.GetAuthorisationDomainRoleValidator;
import net.openid.conformance.raidiam.validators.referencesAuthorisationDomainRole.PostAuthorisationDomainRoleValidator;
import net.openid.conformance.raidiam.validators.referencesAuthorisationDomainRole.PutAuthorisationDomainRoleValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class AuthorisationDomainRoleValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/referencesAuthorisationDomainRole/GetAuthorisationDomainRoleByDomainNameResponse.json")
	public void validateStructureGetAuthorisationDomainByDomainNameValidator() {
		GetAuthorisationDomainRoleByDomainNameValidator condition = new GetAuthorisationDomainRoleByDomainNameValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/referencesAuthorisationDomainRole/GetAuthorisationDomainRoleResponse.json")
	public void validateGetAuthorisationDomainRoleValidator() {
		GetAuthorisationDomainRoleValidator condition = new GetAuthorisationDomainRoleValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/referencesAuthorisationDomainRole/PostAuthorisationDomainRoleResponse.json")
	public void validatePostAuthorisationDomainRoleValidator() {
		PostAuthorisationDomainRoleValidator condition = new PostAuthorisationDomainRoleValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/referencesAuthorisationDomainRole/PutAuthorisationDomainRoleResponse.json")
	public void validatePutAuthorisationDomainRoleValidator() {
		PutAuthorisationDomainRoleValidator condition = new PutAuthorisationDomainRoleValidator();
		run(condition);
	}
}
