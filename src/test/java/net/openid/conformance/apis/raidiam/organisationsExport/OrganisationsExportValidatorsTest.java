package net.openid.conformance.apis.raidiam.organisationsExport;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.raidiam.validators.organisationsExport.GetOrganisationsExportMockValidator;
import net.openid.conformance.raidiam.validators.organisationsExport.GetOrganisationsExportOpenDataValidator;
import net.openid.conformance.raidiam.validators.organisationsExport.GetOrganisationsExportRolesValidator;
import net.openid.conformance.raidiam.validators.organisationsExport.GetOrganisationsExportSnapshotByOrganisationIdValidator;
import net.openid.conformance.raidiam.validators.organisationsExport.GetOrganisationsExportSnapshotValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class OrganisationsExportValidatorsTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/organisationsExport/GetOrganisationsExportOpenDataResponse.json")
	public void validateStructureGetOrganisationsValidator() {
		GetOrganisationsExportOpenDataValidator condition = new GetOrganisationsExportOpenDataValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationsExport/GetOrganisationsExportOpenDataResponse_maxLengthError.json")
	public void validateStructureGetOrganisationsValidatorWithWrongMaxLength() {
		GetOrganisationsExportOpenDataValidator condition = new GetOrganisationsExportOpenDataValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("OrganisationId",
			condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationsExport/GetOrganisationsExportRolesResponse.json")
	public void validateStructureGetOrganisationsExportRolesValidator() {
		GetOrganisationsExportRolesValidator condition = new GetOrganisationsExportRolesValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationsExport/GetOrganisationsExportRolesResponse_NullFieldError.json")
	public void validateStructureGetOrganisationsExportRolesValidatorWithNullField() {
		GetOrganisationsExportRolesValidator condition = new GetOrganisationsExportRolesValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementCantBeNullMessage("Status",
			condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationsExport/GetOrganisationsExportMockResponse.json")
	public void validateStructureGetOrganisationsExportMockValidator() {
		GetOrganisationsExportMockValidator condition = new GetOrganisationsExportMockValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationsExport/GetOrganisationsExportSnapshotResponse.json")
	public void validateStructureGetOrganisationsExportSnapshotValidator() {
		GetOrganisationsExportSnapshotValidator condition = new GetOrganisationsExportSnapshotValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationsExport/GetOrganisationsExportSnapshotByOrganisationIdResponse.json")
	public void validateStructureGetOrganisationsExportSnapshotByOrganisationIdValidator() {
		GetOrganisationsExportSnapshotByOrganisationIdValidator condition = new GetOrganisationsExportSnapshotByOrganisationIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationsExport/GetOrganisationsExportMockResponse_maxLengthError.json")
	public void validateStructureWithWrongMaxLength() {
		GetOrganisationsExportMockValidator condition = new GetOrganisationsExportMockValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("OrganisationId",
			condition.getApiName())));
	}
}
