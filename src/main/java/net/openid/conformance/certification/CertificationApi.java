package net.openid.conformance.certification;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping(value = "/api")
public class CertificationApi {

	private final String clientId = "17fa5601-8e8e-44d7-a924-ba9e16636a8b";
	private final String userId = "446dbf0e-a264-45f7-9fa6-2f8c2229be3c";
	private final long expiresIn = 3600;
	private final String scopes = "signature impersonation";
	private final byte[] privateKey;
	private final String aud = "account-d.docusign.com";
	private final String apiUrl = "https://demo.docusign.net/restapi";
	private final String tokenEndpoint = "https://account-d.docusign.com/oauth/token";
	private final String userInfoEndpoint = "https://account-d.docusign.com/oauth/userinfo";

	public CertificationApi() {
		try {
			privateKey = Files.readAllBytes(Paths.get("docusign.test.private.key"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

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
		@Parameter(description = "Test plan id") @PathVariable("id") String id,
		@Parameter(description = "Document data (base64 encoded PDF document) and signatory information") @RequestBody JsonObject body
	) throws Exception {
		String documentData = OIDFJSON.getString(body.get("documentData"));
		String email = OIDFJSON.getString(body.get("email"));
		String name = OIDFJSON.getString(body.get("name"));

		String jwt = JWTUtil.generateDocuSignJWTAssertion(privateKey, aud, clientId, userId, expiresIn, scopes);
		String accessToken = getAccessToken(tokenEndpoint, jwt);
		String accountId = getAccountId(userInfoEndpoint, accessToken);
		String envelopeId = createEnvelope(apiUrl, accessToken, accountId, documentData, email, name);
		String redirectUrl = "https://localhost:8443/static/form.html?plan=" + id + "&envelopeId=" + envelopeId;
		String signUrl = createView(apiUrl, accessToken, accountId, envelopeId, redirectUrl, email, name);

		Map<String, Object> map = new HashMap<>();
		map.put("id", id);
		map.put("recipentSigningUri", signUrl);
		return new ResponseEntity<>(map, HttpStatus.OK);
	}

	@GetMapping(value = "/plan/{id}/sign/{envelopeId}", produces = MediaType.APPLICATION_PDF_VALUE)
	@Operation(summary = "")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Retrieved successfully"),
		@ApiResponse(responseCode = "500", description = "An unexpected error occurred, the error should be reported to the developers")
	})
	public ResponseEntity<Object> getSignedDocument(
		@Parameter(description = "Test plan id") @PathVariable("id") String id,
		@Parameter(description = "Signed document envelopeId") @PathVariable("envelopeId") String envelopeId
	) throws Exception {
		String jwt = JWTUtil.generateDocuSignJWTAssertion(privateKey, aud, clientId, userId, expiresIn, scopes);
		String accessToken = getAccessToken(tokenEndpoint, jwt);
		String accountId = getAccountId(userInfoEndpoint, accessToken);

		byte[] responseBody = Request.Get(apiUrl + "/v2.1/accounts/" + accountId + "/envelopes/" + envelopeId + "/documents/1")
			.addHeader("Authorization", "Bearer " + accessToken)
			.execute()
			.returnContent()
			.asBytes();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentDispositionFormData("attachment", "OpenID-Certification-of-Conformance.pdf");
		return ResponseEntity.ok()
			.headers(headers)
			.contentLength(responseBody.length)
			.body(responseBody);
	}

	private static String getAccessToken(String tokenEndpoint, String jwt) throws IOException {
		String responseBody = Request.Post(tokenEndpoint)
			.bodyForm(Form.form()
				.add("grant_type",  "urn:ietf:params:oauth:grant-type:jwt-bearer")
				.add("assertion", jwt).build())
			.execute()
			.returnContent()
			.asString();
		JsonObject tokenResponse = JsonParser.parseString(responseBody).getAsJsonObject();
		return OIDFJSON.getString(tokenResponse.get("access_token"));
	}

	private static String getAccountId(String userInfoEndpoint, String accessToken) throws IOException {
		String responseBody;

		responseBody = Request.Get(userInfoEndpoint)
			.addHeader("Authorization", "Bearer " + accessToken)
			.execute()
			.returnContent()
			.asString();
		JsonObject userInfoResponse = JsonParser.parseString(responseBody).getAsJsonObject();
		return OIDFJSON.getString(userInfoResponse.get("accounts").getAsJsonArray().get(0).getAsJsonObject().get("account_id"));
	}

	private static String createEnvelope(String apiUrl, String accessToken, String accountId, String documentData, String email, String name) throws IOException {
		String envelope = "{\n" +
			"    \"allowComments\": \"false\",\n" +
			"    \"emailSubject\": \"Certification of Conformance signature\",\n" +
			"    \"documents\": [\n" +
			"        {\n" +
			"            \"documentBase64\": \"" + documentData + "\",\n" +
			"            \"name\": \"OpenID - Certification of Conformance\",\n" +
			"            \"fileExtension\": \"pdf\",\n" +
			"            \"documentId\": \"1\"\n" +
			"        }\n" +
			"    ],\n" +
			"    \"recipients\": {\n" +
			"        \"signers\": [\n" +
			"            {\n" +
			"                \"email\": \"" + email + "\",\n" +
			"                \"name\": \"" + name + "\",\n" +
			"                \"recipientId\": \"1\",\n" +
			"                \"routingOrder\": \"1\",\n" +
			"                \"clientUserId\": \"1000\"\n" +
			"            }\n" +
			"        ]\n" +
			"    },\n" +
			"    \"status\": \"sent\"\n" +
			"}";
		String responseBody = Request.Post(apiUrl + "/v2.1/accounts/" + accountId + "/envelopes")
			.bodyString(envelope, ContentType.APPLICATION_JSON)
			.addHeader("Authorization", "Bearer " + accessToken)
			.execute()
			.returnContent()
			.asString();
		JsonObject envelopeResponse = JsonParser.parseString(responseBody).getAsJsonObject();
		return OIDFJSON.getString(envelopeResponse.get("envelopeId"));
	}

	private static String createView(String apiUrl, String accessToken, String accountId, String envelopeId, String redirectUrl, String email, String name) throws IOException {
		String envelope = "{\n" +
			"    \"returnUrl\": \"" + redirectUrl + "\",\n" +
			"    \"authenticationMethod\": \"none\",\n" +
			"    \"email\": \"" + email + "\",\n" +
			"    \"userName\": \"" + name + "\",\n" +
			"    \"clientUserId\": \"1000\"\n" +
			"}";
		String responseBody = Request.Post(apiUrl + "/v2.1/accounts/" + accountId + "/envelopes/" + envelopeId + "/views/recipient")
			.bodyString(envelope, ContentType.APPLICATION_JSON)
			.addHeader("Authorization", "Bearer " + accessToken)
			.execute()
			.returnContent()
			.asString();
		JsonObject viewResponse = JsonParser.parseString(responseBody).getAsJsonObject();
		return OIDFJSON.getString(viewResponse.get("url"));
	}

			/*
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
}
