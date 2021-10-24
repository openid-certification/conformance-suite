package net.openid.conformance.apis.raidiam.organisationDomainUsers;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.organisationDomainUsers.GetOrganisationDomainUsersByEmailValidator;
import net.openid.conformance.raidiam.validators.organisationDomainUsers.GetOrganisationDomainUsersValidator;
import net.openid.conformance.raidiam.validators.organisationDomainUsers.PostOrganisationDomainUsersValidator;
import net.openid.conformance.raidiam.validators.organisationDomainUsers.PutOrganisationDomainUsersByEmailValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class OrganisationDomainUsersValidatorsTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/organisationDomainUsers/GetOrganisationDomainUsersResponse.json")
	public void GetOrganisationDomainUsers() {
		GetOrganisationDomainUsersValidator condition = new GetOrganisationDomainUsersValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationDomainUsers/GetOrganisationDomainUsersResponse(MissField).json")
	public void GetOrganisationDomainWithMissingField() {
		GetOrganisationDomainUsersValidator condition = new GetOrganisationDomainUsersValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationDomainUsers/GetOrganisationDomainUsersByEmailResponse.json")
	public void GetOrganisationDomainUsersByEmail() {
		GetOrganisationDomainUsersByEmailValidator condition = new GetOrganisationDomainUsersByEmailValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationDomainUsers/GetOrganisationDomainUsersByEmailResponse(MissField).json")
	public void GetOrganisationDomainUsersByEmailWithMissingField() {
		GetOrganisationDomainUsersByEmailValidator condition = new GetOrganisationDomainUsersByEmailValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationDomainUsers/PostOrganisationDomainUsersResponse.json")
	public void PostOrganisationDomainUsers() {
		PostOrganisationDomainUsersValidator condition = new PostOrganisationDomainUsersValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationDomainUsers/PostOrganisationDomainUsersResponse(MissField).json")
	public void PostOrganisationDomainUsersWithMissingField() {
		PostOrganisationDomainUsersValidator condition = new PostOrganisationDomainUsersValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationDomainUsers/PutOrganisationDomainUsersByEmailResponse.json")
	public void PutOrganisationDomainUsersByEmail() {
		PutOrganisationDomainUsersByEmailValidator condition = new PutOrganisationDomainUsersByEmailValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationDomainUsers/PutOrganisationDomainUsersByEmailResponse(MissField).json")
	public void PutOrganisationDomainUsersByEmailWithMissingField() {
		PutOrganisationDomainUsersByEmailValidator condition = new PutOrganisationDomainUsersByEmailValidator();
		run(condition);
	}
}
