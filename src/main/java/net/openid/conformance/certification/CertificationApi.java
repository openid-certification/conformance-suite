package net.openid.conformance.certification;

import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.ApiException;
import com.docusign.esign.client.auth.JWTUtils;
import com.docusign.esign.client.auth.OAuth;
import com.docusign.esign.model.Document;
import com.docusign.esign.model.EnvelopeDefinition;
import com.docusign.esign.model.EnvelopeSummary;
import com.docusign.esign.model.Recipients;
import com.docusign.esign.model.SignHere;
import com.docusign.esign.model.Signer;
import com.docusign.esign.model.Tabs;
import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Controller
@RequestMapping(value = "/api")
public class CertificationApi {

	@PostMapping(value = "/plan/{id}/certificationofconformance", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
	@Operation(summary = "Get certification of conformance pdf template, with pre-populated fields")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Retrieved successfully"),
			@ApiResponse(responseCode = "500", description = "An unexpected error occurred, the error should be reported to the developers")
	})
	public void getCertificationOfConformancePdfTemplate(
			HttpServletResponse response,
			@Parameter(description = "Id of test plan") @PathVariable("id") String id,
			@Parameter(description = "Name of Entity (Implementer) Making this Certification") @RequestParam(name = "nameOfImplementer", required = false) String nameOfImplementer,
			@Parameter(description = "Software or Service (Deployment) Name & Version #") @RequestParam(name = "deploymentVersion", required = false) String deploymentVersion,
			@Parameter(description = "OpenID Conformance Profile") @RequestParam(name = "conformanceProfile", required = false) String conformanceProfile,
			@Parameter(description = "Conformance Test Suite Software") @RequestParam(name = "suiteSoftware", required = false) String suiteSoftware,
			@Parameter(description = "Test Date") @RequestParam(name = "testDate", required = false) String testDate,
			@Parameter(description = "URL at which people interested in using your implementation can learn about it and/or obtain it") @RequestParam(name = "moreInfoUrl", required = false) String moreInfoUrl,
			@Parameter(description = "1-2 sentence description of the implementation") @RequestParam(name = "implementationDescription", required = false) String implementationDescription,
			@Parameter(description = "The programming language of the software and deployment environment for it, if applicable") @RequestParam(name = "programmingLanguage", required = false) String programmingLanguage,
			@Parameter(description = "Licensing terms of the software, if applicable") @RequestParam(name = "license", required = false) String license
	) throws Exception {
		File filledPdfTemplateFile = null;
		try {
			JsonObject data = new JsonObject();
			data.addProperty("nameOfImplementer", nameOfImplementer);
			data.addProperty("deploymentVersion", deploymentVersion);
			data.addProperty("conformanceProfile", conformanceProfile);
			data.addProperty("suiteSoftware", suiteSoftware);
			data.addProperty("testDate", testDate);
			data.addProperty("moreInfoUrl", moreInfoUrl);
			data.addProperty("implementationDescription", implementationDescription);
			data.addProperty("programmingLanguage", programmingLanguage);
			data.addProperty("license", license);
			filledPdfTemplateFile = CertificationOfConformanceUtil.fillCertificationOfConformancePDFTemplate(data);

			FileSystemResource fileSystemResource = new FileSystemResource(filledPdfTemplateFile);
			response.setContentLength((int) fileSystemResource.contentLength());
			response.setContentType(MediaType.APPLICATION_PDF_VALUE);
			response.setHeader("Content-Disposition", "attachment; filename=OpenID-Certification-of-Conformance.pdf");

			IOUtils.copy(fileSystemResource.getInputStream(), response.getOutputStream());
		} finally {
			if (filledPdfTemplateFile != null) {
				filledPdfTemplateFile.delete();
			}
		}
	}

	@PostMapping(value = "/plan/{id}/sign", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Retrieved successfully"),
		@ApiResponse(responseCode = "500", description = "An unexpected error occurred, the error should be reported to the developers")
	})
	public ResponseEntity<Object> sign(
		HttpServletResponse response,
		@Parameter(description = "Id of test plan") @PathVariable("id") String id,
		@Parameter(description = "Document data (base64 encoded PDF document)") @RequestBody JsonObject body
	) throws Exception {

		String clientId = "17fa5601-8e8e-44d7-a924-ba9e16636a8b";
		String userId = "446dbf0e-a264-45f7-9fa6-2f8c2229be3c";
		long expiresIn = 3600;
		String scopes = "signature impersonation";
		byte[] privateKey = Files.readAllBytes(Paths.get("docusign.test.private.key"));
		String oAuthBasePath = "https://demo.docusign.net/restapi";

		String jwt = JWTUtils.generateJWTAssertionFromByteArray(privateKey, oAuthBasePath, clientId, userId, expiresIn, scopes);
		HttpClient client = HttpClientBuilder.create().useSystemProperties().build();

		String s = "";

		new ApiClient();
		/*
		// Get information fro app.config
		Properties prop = new Properties();
		String fileName = "docusign.config";
		FileInputStream fis = new FileInputStream(fileName);
		prop.load(fis);

		// Get access token and accountId
		ApiClient apiClient = new ApiClient("https://demo.docusign.net/restapi");
		apiClient.setOAuthBasePath("account-d.docusign.com");
		ArrayList<String> scopes = new ArrayList<String>();
		scopes.add("signature");
		scopes.add("impersonation");
		byte[] privateKeyBytes = Files.readAllBytes(Paths.get(prop.getProperty("rsaKeyFile")));
		OAuth.OAuthToken oAuthToken = apiClient.requestJWTUserToken(
			prop.getProperty("clientId"),
			prop.getProperty("userId"),
			scopes,
			privateKeyBytes,
			3600);
		String accessToken = oAuthToken.getAccessToken();
		OAuth.UserInfo userInfo = apiClient.getUserInfo(accessToken);
		String accountId = userInfo.getAccounts().get(0).getAccountId();

		// Create envelopeDefinition object
		EnvelopeDefinition envelope = new EnvelopeDefinition();
		envelope.setEmailSubject("Please sign this document set");
		envelope.setStatus("sent");

		// Create tabs object
		SignHere signHere = new SignHere();
		signHere.setDocumentId("1");
		signHere.setPageNumber("1");
		signHere.setXPosition("191");
		signHere.setYPosition("148");
		Tabs tabs = new Tabs();
		tabs.setSignHereTabs(List.of(signHere));
		// Set recipients
		Signer signer = new Signer();
		signer.setEmail("marcus.almgren@oidf.org");
		signer.setName("Kalle Kula");
		signer.recipientId("1");
		signer.setTabs(tabs);
		Recipients recipients = new Recipients();
		recipients.setSigners(List.of(signer));
		envelope.setRecipients(recipients);

		// Add document
		Document document = new Document();
		document.setDocumentBase64(body.get("documentData").getAsString());
		document.setName("OpenID-Certification-of-Conformance.pdf");
		document.setFileExtension("pdf");
		document.setDocumentId(id);
		envelope.setDocuments(List.of(document));

		// Send envelope
		apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
		EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
		EnvelopeSummary results = envelopesApi.createEnvelope(accountId, envelope);
		String recipentSigningUri = results.getRecipientSigningUri();
		*/

		Map<String, Object> map = new HashMap<>();
		map.put("id", id);
		//map.put("recipentSigningUri", recipentSigningUri);
		return new ResponseEntity<>(map, HttpStatus.OK);
	}

}
