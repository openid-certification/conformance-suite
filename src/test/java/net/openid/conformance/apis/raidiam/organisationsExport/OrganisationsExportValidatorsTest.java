package net.openid.conformance.apis.raidiam.organisationsExport;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.raidiam.validators.organisationsExport.GetOrganisationsExportMockValidator;
import net.openid.conformance.raidiam.validators.organisationsExport.GetOrganisationsExportOpenDataValidator;
import net.openid.conformance.raidiam.validators.organisationsExport.GetOrganisationsExportRolesValidator;
import net.openid.conformance.raidiam.validators.organisationsExport.GetOrganisationsExportSnapshotByOrganisationIdValidator;
import net.openid.conformance.raidiam.validators.organisationsExport.GetOrganisationsExportSnapshotValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

public class OrganisationsExportValidatorsTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/raidiam/organisationsExport/GetOrganisationsExportOpenDataResponse.json")
	public void validateStructureGetOrganisationsValidator() {
		GetOrganisationsExportOpenDataValidator condition = new GetOrganisationsExportOpenDataValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/raidiam/organisationsExport/GetOrganisationsExportRolesResponse.json")
	public void validateStructureGetOrganisationsExportRolesValidator() {
		GetOrganisationsExportRolesValidator condition = new GetOrganisationsExportRolesValidator();
		run(condition);
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
}
