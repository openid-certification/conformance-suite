package net.openid.conformance.apis.raidiam.organisationAdminUsers;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.organisationAdminUsers.GetOrganisationAdminUsersByEmailValidator;
import net.openid.conformance.raidiam.validators.organisationAdminUsers.GetOrganisationAdminUsersValidator;
import net.openid.conformance.raidiam.validators.organisationAdminUsers.PostOrganisationAdminUsersValidator;
import net.openid.conformance.raidiam.validators.organisationAdminUsers.PutOrganisationAdminUsersByEmailValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class OrganisationAdminUsersValidatorsTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/organisationAdminUsers/GetOrganisationAdminUsersResponse.json")
	public void GetOrganisationAdminUsers() {
		GetOrganisationAdminUsersValidator condition = new GetOrganisationAdminUsersValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationAdminUsers/GetOrganisationAdminUsersResponse(MissField).json")
	public void GetOrganisationAdminUsersWithMissingField() {
		GetOrganisationAdminUsersValidator condition = new GetOrganisationAdminUsersValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationAdminUsers/GetOrganisationAdminUsersByEmail.json")
	public void GetOrganisationAdminUsersByEmail() {
		GetOrganisationAdminUsersByEmailValidator condition = new GetOrganisationAdminUsersByEmailValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationAdminUsers/GetOrganisationAdminUsersByEmail(MissField).json")
	public void GetOrganisationAdminUsersByEmailWithMissingField() {
		GetOrganisationAdminUsersByEmailValidator condition = new GetOrganisationAdminUsersByEmailValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationAdminUsers/PostOrganisationAdminUsers.json")
	public void PostOrganisationAdminUsers() {
		PostOrganisationAdminUsersValidator condition = new PostOrganisationAdminUsersValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationAdminUsers/PostOrganisationAdminUsers(WithMissField).json")
	public void PostOrganisationAdminUsersWithMissingField() {
		PostOrganisationAdminUsersValidator condition = new PostOrganisationAdminUsersValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationAdminUsers/PutOrganisationAdminUsersByEmailResponse.json")
	public void PutOrganisationAdminUsersByEmailResponse() {
		PutOrganisationAdminUsersByEmailValidator condition = new PutOrganisationAdminUsersByEmailValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationAdminUsers/PutOrganisationAdminUsersByEmailResponse(MissField).json")
	public void PutOrganisationAdminUsersByEmailResponseMissField() {
		PutOrganisationAdminUsersByEmailValidator condition = new PutOrganisationAdminUsersByEmailValidator();
		run(condition);
	}
}
