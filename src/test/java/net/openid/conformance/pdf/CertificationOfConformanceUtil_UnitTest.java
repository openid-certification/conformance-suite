package net.openid.conformance.pdf;

import net.openid.conformance.certification.CertificationOfConformanceUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CertificationOfConformanceUtil_UnitTest
{
	@BeforeEach
	public void setUp() throws Exception {

	}

	/**
	 * Test pdf template fields and implementation
	 * @throws IOException
	 */
	@Test
	public void fillForm() throws IOException{
		String formTemplate = "src/main/resources/templates/pdf/OpenID-Certification-of-Conformance.pdf";
		File outputFile = File.createTempFile("fillForm-unittest",".pdf");
		final String profileFieldValue = "Test conformance profile";
		final String nameVersionValueWithNonAsciiChars = "Non-ascii chars ĞÜŞİÖÇ çığüşö";
		final String nameOfImplementerValue = "nameOfImplementer field value";
		final String suiteFieldValue = " www.certification.openid.net 4.1.20";
		final String testDateFieldValue = "2021-07-01";
		final String moreInfoUrlFieldValue = "https://openid.net/certification/#FAPI_OPs";
		final String implDescFieldValue = "Can we add new lines to form fields? \n Yes?";
		final String progLangFieldValue = "JavaScript for Node.js";
		final String licenseFieldValue = "Apache 2.0";

		List<String> originalFields = new ArrayList<>();
		List<String> reloadedFields = new ArrayList<>();
		try (PDDocument pdfDocument = PDDocument.load(new File(formTemplate))) {
			PDAcroForm acroForm = pdfDocument.getDocumentCatalog().getAcroForm(null);

			for(PDField field : acroForm.getFields()) {
				originalFields.add(field.getFullyQualifiedName());
				//System.out.println(field.getFullyQualifiedName());
			}

			PDField nameOfImplementerField = acroForm.getField(CertificationOfConformanceUtil.PDF_FIELD_NAME_OF_IMPLEMENTER);
			nameOfImplementerField.setValue(nameOfImplementerValue);

			PDField nameVersionField = acroForm.getField(CertificationOfConformanceUtil.PDF_FIELD_DEPLOYMENT_VERSION);
			nameVersionField.setValue(nameVersionValueWithNonAsciiChars);

			PDField profileField = acroForm.getField(CertificationOfConformanceUtil.PDF_FIELD_CONFORMANCE_PROFILE);
			profileField.setValue(profileFieldValue);

			PDField suiteField = acroForm.getField(CertificationOfConformanceUtil.PDF_FIELD_TEST_SUITE_SOFTWARE);
			suiteField.setValue(suiteFieldValue);

			PDField testDateField = acroForm.getField(CertificationOfConformanceUtil.PDF_FIELD_TEST_DATE);
			testDateField.setValue(testDateFieldValue);

			PDField moreInfoUrlField = acroForm.getField(CertificationOfConformanceUtil.PDF_FIELD_MORE_INFO_URL);
			moreInfoUrlField.setValue(moreInfoUrlFieldValue);

			PDField implDescField = acroForm.getField(CertificationOfConformanceUtil.PDF_FIELD_IMPL_DESCRIPTION);
			implDescField.setValue(implDescFieldValue);

			PDField progLangField = acroForm.getField(CertificationOfConformanceUtil.PDF_FIELD_PROGRAMMING_LANG);
			progLangField.setValue(progLangFieldValue);

			PDField licenseField = acroForm.getField(CertificationOfConformanceUtil.PDF_FIELD_LICENSE);
			licenseField.setValue(licenseFieldValue);

			pdfDocument.save(outputFile);
		} catch (IOException e) {
			throw e;
		}
		//load and verify
		try (PDDocument pdfDocument = PDDocument.load(outputFile)) {
			PDAcroForm acroForm = pdfDocument.getDocumentCatalog().getAcroForm(null);

			for(PDField field : acroForm.getFields()) {
				reloadedFields.add(field.getFullyQualifiedName());
			}
			PDField nameOfImplementerField = acroForm.getField(CertificationOfConformanceUtil.PDF_FIELD_NAME_OF_IMPLEMENTER);
			Assertions.assertEquals(nameOfImplementerValue, nameOfImplementerField.getValueAsString());

			PDField nameField = acroForm.getField(CertificationOfConformanceUtil.PDF_FIELD_DEPLOYMENT_VERSION);
			Assertions.assertEquals(nameVersionValueWithNonAsciiChars, nameField.getValueAsString());

			PDField profileField = acroForm.getField(CertificationOfConformanceUtil.PDF_FIELD_CONFORMANCE_PROFILE);
			Assertions.assertEquals(profileFieldValue, profileField.getValueAsString());

			PDField suiteField = acroForm.getField(CertificationOfConformanceUtil.PDF_FIELD_TEST_SUITE_SOFTWARE);
			Assertions.assertEquals(suiteFieldValue, suiteField.getValueAsString());

			PDField testDateField = acroForm.getField(CertificationOfConformanceUtil.PDF_FIELD_TEST_DATE);
			Assertions.assertEquals(testDateFieldValue, testDateField.getValueAsString());

			PDField moreInfoUrlField = acroForm.getField(CertificationOfConformanceUtil.PDF_FIELD_MORE_INFO_URL);
			Assertions.assertEquals(moreInfoUrlFieldValue, moreInfoUrlField.getValueAsString());

			PDField implDescField = acroForm.getField(CertificationOfConformanceUtil.PDF_FIELD_IMPL_DESCRIPTION);
			Assertions.assertEquals(implDescFieldValue, implDescField.getValueAsString());

			PDField progLangField = acroForm.getField(CertificationOfConformanceUtil.PDF_FIELD_PROGRAMMING_LANG);
			Assertions.assertEquals(progLangFieldValue, progLangField.getValueAsString());

			PDField licenseField = acroForm.getField(CertificationOfConformanceUtil.PDF_FIELD_LICENSE);
			Assertions.assertEquals(licenseFieldValue, licenseField.getValueAsString());

		} catch (IOException e) {
			throw e;
		} finally {
			outputFile.delete();
		}
		Assertions.assertEquals(originalFields, reloadedFields, "Field names in the template and generated pdfs do not match.");
	}
}
