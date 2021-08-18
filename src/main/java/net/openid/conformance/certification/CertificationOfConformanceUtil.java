package net.openid.conformance.certification;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class CertificationOfConformanceUtil {
	//These values MUST match the field names as returned by PDField.getFullyQualifiedName()
	//These may need to be updated when the PDF template is updated
	public static final String PDF_FIELD_NAME_OF_IMPLEMENTER = "Name of Entity Implementer Making this Certification";
	public static final String PDF_FIELD_DEPLOYMENT_VERSION = "Software or Service Deployment Name  Version";
	public static final String PDF_FIELD_CONFORMANCE_PROFILE = "OpenID Conformance Profile";
	public static final String PDF_FIELD_TEST_SUITE_SOFTWARE = "Conformance Test Suite Software";
	public static final String PDF_FIELD_TEST_DATE = "Test Date";
	public static final String PDF_FIELD_MORE_INFO_URL = "URL at which people interested in using your implementation can learn about it andor obtain it";
	public static final String PDF_FIELD_IMPL_DESCRIPTION = "1-2 sentence description of the implementation";
	public static final String PDF_FIELD_PROGRAMMING_LANG = "JavaScript for Nodejs Binaries for iOS or Service";
	public static final String PDF_FIELD_LICENSE = "Licensing terms of the software if applicable eg Apache 20 or Proprietary";

	/**
	 * Returns a tmp file.
	 * It is the caller's responsibility to delete the file after returning its contents to the client
	 * @return
	 * @throws IOException
	 */
	public static final File fillCertificationOfConformancePDFTemplate(JsonObject pdfData)
		throws IOException{

		String formTemplate = "templates/pdf/OpenID-Certification-of-Conformance.pdf";
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(formTemplate);
		try (PDDocument pdfDocument = PDDocument.load(inputStream)) {
			PDAcroForm acroForm = pdfDocument.getDocumentCatalog().getAcroForm(null);
			if(acroForm == null) {
				throw new RuntimeException("Invalid PDF template. Please report this error.");
			}

			if(pdfData.has("nameOfImplementer")) {
				PDField implementerField = acroForm.getField(PDF_FIELD_NAME_OF_IMPLEMENTER);
				implementerField.setValue(OIDFJSON.getString(pdfData.get("nameOfImplementer")));
			}

			if(pdfData.has("conformanceProfile")) {
				PDField profileField = acroForm.getField(PDF_FIELD_CONFORMANCE_PROFILE);
				profileField.setValue(OIDFJSON.getString(pdfData.get("conformanceProfile")));
			}

			if(pdfData.has("deploymentVersion")) {
				PDField deploymentVersionField = acroForm.getField(PDF_FIELD_DEPLOYMENT_VERSION);
				deploymentVersionField.setValue(OIDFJSON.getString(pdfData.get("deploymentVersion")));
			}

			if(pdfData.has("suiteSoftware")) {
				PDField suiteSoftwareField = acroForm.getField(PDF_FIELD_TEST_SUITE_SOFTWARE);
				suiteSoftwareField.setValue(OIDFJSON.getString(pdfData.get("suiteSoftware")));
			}

			if(pdfData.has("testDate")) {
				PDField testDateField = acroForm.getField(PDF_FIELD_TEST_DATE);
				testDateField.setValue(OIDFJSON.getString(pdfData.get("testDate")));
			}

			if(pdfData.has("moreInfoUrl")) {
				PDField moreInfoUrlField = acroForm.getField(PDF_FIELD_MORE_INFO_URL);
				moreInfoUrlField.setValue(OIDFJSON.getString(pdfData.get("moreInfoUrl")));
			}

			if(pdfData.has("implementationDescription")) {
				PDField implementationDescriptionField = acroForm.getField(PDF_FIELD_IMPL_DESCRIPTION);
				implementationDescriptionField.setValue(OIDFJSON.getString(pdfData.get("implementationDescription")));
			}

			if(pdfData.has("programmingLanguage")) {
				PDField programmingLanguageField = acroForm.getField(PDF_FIELD_PROGRAMMING_LANG);
				programmingLanguageField.setValue(OIDFJSON.getString(pdfData.get("programmingLanguage")));
			}

			if(pdfData.has("license")) {
				PDField licenseField = acroForm.getField(PDF_FIELD_LICENSE);
				licenseField.setValue(OIDFJSON.getString(pdfData.get("license")));
			}

			File tmpFile = File.createTempFile("certofconf-", ".pdf");
			pdfDocument.save(tmpFile);
			tmpFile.deleteOnExit();
			return tmpFile;
		} catch (IOException e) {
			throw e;
		}
	}
}
