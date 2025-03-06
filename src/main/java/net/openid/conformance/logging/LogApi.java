package net.openid.conformance.logging;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import net.openid.conformance.CollapsingGsonHttpMessageConverter;
import net.openid.conformance.export.HtmlExportRenderer;
import net.openid.conformance.export.PlanExportInfo;
import net.openid.conformance.export.TestExportInfo;
import net.openid.conformance.export.TestHelper;
import net.openid.conformance.info.Plan;
import net.openid.conformance.info.PublicPlan;
import net.openid.conformance.info.PublicTestInfo;
import net.openid.conformance.info.TestInfo;
import net.openid.conformance.info.TestInfoRepository;
import net.openid.conformance.info.TestPlanService;
import net.openid.conformance.pagination.PaginationRequest;
import net.openid.conformance.pagination.PaginationResponse;
import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.security.KeyManager;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.VariantSelection;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.SignatureException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = "/api")
public class LogApi {

	@Value("${fintechlabs.base_url:http://localhost:8080}")
	private String baseUrl;

	@Value("${fintechlabs.version}")
	private String version;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private TestInfoRepository testInfos;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@Autowired
	private KeyManager keyManager;

	@Autowired
	private HtmlExportRenderer htmlExportRenderer;

	private Gson gson = CollapsingGsonHttpMessageConverter.getDbObjectCollapsingGson();

	@Autowired
	private TestPlanService planService;

	@Value("${net.openid.conformance.logging.logapi.certification-package-failed-tests-exception-list}")
	private String[] certificationPackageFailedTestExceptionList;

	@GetMapping(value = "/log", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get all test logs with paging", description = "Return all published logs when public data is requested, otherwise all test logs if user is admin, or only the user's test logs")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Retrieved successfully")
	})
	public ResponseEntity<Object> getAllTests(
		@Parameter(description = "Published data only") @RequestParam(name = "public", defaultValue = "false") boolean publicOnly,
		PaginationRequest page) {

		PaginationResponse<?> response;

		if (publicOnly) {
			response = page.getResponse(
					p -> testInfos.findAllPublic(p),
					(s, p) -> testInfos.findAllPublicSearch(s, p));
		} else if (authenticationFacade.isAdmin()) {
			response = page.getResponse(
					p -> testInfos.findAll(p),
					(s, p) -> testInfos.findAllSearch(s, p));
		} else {
			ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();
			response = page.getResponse(
					p -> testInfos.findAllByOwner(owner, p),
					(s, p) -> testInfos.findAllByOwnerSearch(owner, s, p));
		}

		return new ResponseEntity<>(response, HttpStatus.OK);

	}

	@GetMapping(value = "/log/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get test log of given testId")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Retrieved successfully")
	})
	public ResponseEntity<List<Document>> getLogResults(
		@Parameter(description = "Id of test") @PathVariable String id,
		@Parameter(description = "Since when test created") @RequestParam(required = false) Long since,
		@Parameter(description = "Published data only") @RequestParam(name = "public", defaultValue = "false") boolean publicOnly) {
		List<Document> results = getTestResults(id, since, publicOnly);

		return ResponseEntity.ok().body(results);
	}

	@GetMapping(value = "/log/export/{id}", produces = "application/zip")
	@Operation(summary = "Export test log by test id")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Exported successfully"),
		@ApiResponse(responseCode = "404", description = "Couldn't find given test Id")
	})
	public ResponseEntity<StreamingResponseBody> export(
		@Parameter(description = "Id of test") @PathVariable String id,
		@Parameter(description = "Published data only") @RequestParam(name = "public", defaultValue = "false") boolean publicOnly) {
		List<Document> results = getTestResults(id, null, publicOnly);

		Optional<?> testInfo = getTestInfo(publicOnly, id);

		String testModuleName = null;
		VariantSelection variant = null;

		if (testInfo.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else if (testInfo.get() instanceof TestInfo) {
			testModuleName = ((TestInfo) testInfo.get()).getTestName();
			variant = ((TestInfo) testInfo.get()).getVariant();
		} else if (testInfo.get() instanceof PublicTestInfo) {
			testModuleName = ((PublicTestInfo) testInfo.get()).getTestName();
			variant = ((PublicTestInfo) testInfo.get()).getVariant();
		}

		HttpHeaders headers = new HttpHeaders();

		headers.add("Content-Disposition", "attachment; filename=\"test-log-" + (Strings.isNullOrEmpty(testModuleName) ? "" : (testModuleName + "-")) + variantSuffix(variant) + id + ".zip\"");

		final TestExportInfo export = putTestResultToExport(results, testInfo);

		StreamingResponseBody responseBody = new StreamingResponseBody() {

			@Override
			public void writeTo(OutputStream out) throws IOException {

				try {
					ZipArchiveOutputStream archiveOutputStream = new ZipArchiveOutputStream(out);

					String jsonFileName = "test-log-" + id + ".json";

					String sigFileName = "test-log-" + id + ".sig";

					addFilesToZip(archiveOutputStream, jsonFileName, sigFileName, export);

					archiveOutputStream.close();
				} catch (Exception ex) {
					throw new IOException(ex);
				}
			}
		};

		return ResponseEntity.ok().headers(headers).body(responseBody);
	}

	@GetMapping(value = "/plan/export/{id}", produces = "application/zip")
	@Operation(summary = "Export all test logs of plan by plan id")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Exported successfully"),
		@ApiResponse(responseCode = "404", description = "Couldn't find given plan Id")
	})
	public ResponseEntity<StreamingResponseBody> exportLogsOfPlan(
		@Parameter(description = "Id of plan") @PathVariable String id,
		@Parameter(description = "Published data only") @RequestParam(name = "public", defaultValue = "false") boolean publicOnly) {

		Object testPlan = publicOnly ? planService.getPublicPlan(id) : planService.getTestPlan(id);

		String planName = null;
		VariantSelection variant = null;

		List<Plan.Module> modules = new ArrayList<>();

		if (testPlan == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else if (testPlan instanceof PublicPlan plan) {
			planName = plan.getPlanName();
			variant = plan.getVariant();
			modules = plan.getModules();
		} else if (testPlan instanceof Plan plan) {
			planName = plan.getPlanName();
			variant = plan.getVariant();
			modules = plan.getModules();
		}

		List<Map<String, Object>> allLatestLogsExport = new ArrayList<>();

		for (Plan.Module module : modules) {

			String testModuleName = module.getTestModule();
			List<String> instances = module.getInstances();

			if (instances != null && !instances.isEmpty()) {

				String testId = instances.get(instances.size() - 1);

				List<Document> results = getTestResults(testId, null, publicOnly);

				Optional<?> testInfo = getTestInfo(publicOnly, testId);

				final TestExportInfo export = putTestResultToExport(results, testInfo);

				final Map<String, Object> testLogInfoExport = new HashMap<>();

				testLogInfoExport.put("testId", testId);
				testLogInfoExport.put("testModuleName", testModuleName);
				testLogInfoExport.put("export", export);

				allLatestLogsExport.add(testLogInfoExport);

			}
		}

		if (allLatestLogsExport.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		HttpHeaders headers = new HttpHeaders();

		headers.add("Content-Disposition", "attachment; filename=\"" + (Strings.isNullOrEmpty(planName) ? "" : (planName + "-")) + variantSuffix(variant) + id + ".zip\"");

		StreamingResponseBody responseBody = new StreamingResponseBody() {

			@Override
			public void writeTo(OutputStream out) throws IOException {

				try {
					ZipArchiveOutputStream archiveOutputStream = new ZipArchiveOutputStream(out);

					// add all test logs file of a test plan to zip
					for (Map<String, Object> testLogInfoExport : allLatestLogsExport) {

						String jsonFileName = "test-log-" + testLogInfoExport.get("testModuleName") + "-" + testLogInfoExport.get("testId") + ".json";

						String sigFileName = "test-log-" + testLogInfoExport.get("testModuleName") + "-" + testLogInfoExport.get("testId") + ".sig";

						TestExportInfo infoExport = (TestExportInfo) testLogInfoExport.get("export");

						addFilesToZip(archiveOutputStream, jsonFileName, sigFileName, infoExport);

					}

					archiveOutputStream.close();
				} catch (Exception ex) {
					throw new IOException(ex);
				}
			}
		};

		return ResponseEntity.ok().headers(headers).body(responseBody);
	}

	protected void addFilesToZip(ZipArchiveOutputStream archiveOutputStream, String jsonFileName, String sigFileName, TestExportInfo export) throws Exception {

		ZipArchiveEntry testLog = new ZipArchiveEntry(jsonFileName);

		Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(keyManager.getSigningPrivateKey());

		SignatureOutputStream signatureOutputStream = new SignatureOutputStream(archiveOutputStream, signature);

		String json = gson.toJson(export);

		testLog.setSize(json.getBytes().length);
		archiveOutputStream.putArchiveEntry(testLog);

		signatureOutputStream.write(json.getBytes());

		signatureOutputStream.flush();
		signatureOutputStream.close();

		archiveOutputStream.closeArchiveEntry();

		ZipArchiveEntry signatureFile = new ZipArchiveEntry(sigFileName);

		byte[] src = signature.sign();
		String encodedSignature = Base64.getUrlEncoder().encodeToString(src);
		signatureFile.setSize(encodedSignature.getBytes().length);

		archiveOutputStream.putArchiveEntry(signatureFile);

		archiveOutputStream.write(encodedSignature.getBytes());

		archiveOutputStream.closeArchiveEntry();
	}

	protected Optional<?> getTestInfo(boolean publicOnly, String testId) {
		Optional<?> testInfo = Optional.empty();

		if (publicOnly) {
			testInfo = testInfos.findByIdPublic(testId);
		} else if (authenticationFacade.isAdmin()) {
			testInfo = testInfos.findById(testId);
		} else {
			ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();
			if (owner != null) {
				testInfo = testInfos.findByIdAndOwner(testId, owner);
			}
		}
		return testInfo;
	}

	protected TestExportInfo putTestResultToExport(List<Document> results, Optional<?> testInfo) {
		TestExportInfo export = new TestExportInfo(baseUrl, authenticationFacade.getPrincipal(), version, testInfo.get(), results);

		return export;
	}

	@SuppressWarnings("MixedMutabilityReturnType")
	private List<Document> getTestResults(String id, Long since, boolean isPublic) {
		boolean summaryOnly;

		if (isPublic) {
			// Check publish status of test
			Optional<PublicTestInfo> testInfo = testInfos.findByIdPublic(id);
			if (testInfo.isEmpty()) {
				return Collections.emptyList();
			} else {
				summaryOnly = !testInfo.get().getPublish().equals("everything");
			}
		} else {
			summaryOnly = false;
		}

		Criteria criteria = new Criteria();
		criteria.and("testId").is(id);

		if (!isPublic && !authenticationFacade.isAdmin()) {
			criteria.and("testOwner").is(authenticationFacade.getPrincipal());
		}

		if (since != null) {
			criteria.and("time").gt(since);
		}

		Query query = new Query(criteria);
		if (summaryOnly)
		{
			query.fields()
				.include("result")
				.include("testName")
				.include("testId")
				.include("src")
				.include("time");
		}

		return Lists.newArrayList(mongoTemplate
			.getCollection(DBEventLog.COLLECTION)
			.find(query.getQueryObject())
			.projection(query.getFieldsObject())
			.sort(new Document("time", 1)));
	}

	private static String variantSuffix(VariantSelection variant) {
		if (variant == null) {
			return "";
		} else if (variant.isLegacyVariant()) {
			return variant.getLegacyVariant() + "-";
		} else {
			return variant.getVariant().values()
					.stream()
					.collect(Collectors.joining("-"))
					+ "-";
		}
	}


	@PostMapping(value = "/plan/{id}/certificationpackage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/zip")
	@Operation(summary = "Prepare certification package for a test plan. Also publishes the plan and marks it as immutable.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Prepared successfully"),
		@ApiResponse(responseCode = "403", description = "Could not publish plan"),
		@ApiResponse(responseCode = "404", description = "Could not find a plan with the given id"),
		@ApiResponse(responseCode = "422", description = "Could not mark the plan as immutable")
	})
	public ResponseEntity<StreamingResponseBody> prepareCertificationPackageForTestPlan(
		@Parameter(description = "Id of test plan")
			@PathVariable String id,
		@Parameter(description = "Client data in zip format. Only required for RP tests")
			@RequestParam Optional<MultipartFile> clientSideData
	) {
		JsonObject failedTests = getFailedOrIncompleteTests(id, false);
		if(failedTests.isEmpty()) {
			// Publish plan and make immutable only if there are no failed/incomplete tests
			if (!planService.publishTestPlan(id, "everything")) {
				return new ResponseEntity<>(HttpStatus.FORBIDDEN);
			}
			if (!planService.changeTestPlanImmutableStatus(id, Boolean.TRUE)) {
				return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
			}
			return exportPlanAsZip(id, true, false, true, clientSideData.orElse(null));
		} else {
			return createJsonErrorResponseEntity(failedTests);
		}
	}

	private ResponseEntity<StreamingResponseBody> createJsonErrorResponseEntity(JsonObject json) {
		StreamingResponseBody responseBody = new StreamingResponseBody() {
			@Override
			public void writeTo(OutputStream outputStream) throws IOException {
				try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
					outputStreamWriter.write(json.toString());
				}
			}
		};
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return new ResponseEntity<>(responseBody, headers, HttpStatus.UNPROCESSABLE_ENTITY);
	}

	private JsonObject getFailedOrIncompleteTests(String planId, boolean publicOnly) {
		Object testPlan = publicOnly ? planService.getPublicPlan(planId) : planService.getTestPlan(planId);
		JsonObject failedTests = new JsonObject();
		String planName = "";
		String variant = "";

		List<Plan.Module> modules = new ArrayList<>();

		if (testPlan == null) {
			failedTests.addProperty("error", "invalid_plan_id");
			failedTests.addProperty("error_description", "plan ID " + planId + " does not exist");
		} else if (testPlan instanceof PublicPlan) {
			modules = ((PublicPlan) testPlan).getModules();
			planName = ((PublicPlan) testPlan).getPlanName();
			variant = ((PublicPlan) testPlan).getVariant().toString();
		} else if (testPlan instanceof Plan) {
			modules = ((Plan) testPlan).getModules();
			planName = ((Plan) testPlan).getPlanName();
			variant = ((Plan) testPlan).getVariant().toString();
		}

		List<String> failedTestExceptionList = Arrays.asList(certificationPackageFailedTestExceptionList);
		for (Plan.Module module : modules) {
			String testModuleName = module.getTestModule();
			List<String> instances = module.getInstances();

			if (instances != null && !instances.isEmpty()) {
				String testId = instances.get(instances.size() - 1);
				Optional<?> testInfo = getTestInfo(publicOnly, testId);

				String status = null;
				String result = null;
				if (testInfo.get() instanceof TestInfo) {
					status = ((TestInfo) testInfo.get()).getStatus().name();
					result = ((TestInfo) testInfo.get()).getResult();
				} else if (testInfo.get() instanceof PublicTestInfo) {
					status = ((PublicTestInfo) testInfo.get()).getStatus().name();
					result = ((PublicTestInfo) testInfo.get()).getResult();
				}

				// status != FINISHED or
				// result == {FAILED || UNKNOWN} are considered failed
				if(!status.equals(TestModule.Status.FINISHED.name()) ||
					result.equals(TestModule.Result.FAILED.name()) ||
					result.equals(TestModule.Result.UNKNOWN.name())) {

					if(!failedTestExceptionList.contains(testModuleName)) {
						JsonObject failedTestInfo = new JsonObject();
						failedTestInfo.addProperty("testId", testId);
						failedTestInfo.addProperty("status", status);
						failedTestInfo.addProperty("result", result);
						failedTests.add(testModuleName, failedTestInfo);
					}
				}
			} else {
				JsonObject failedTestInfo = new JsonObject();
				failedTestInfo.addProperty("testId", "NOT_YET_CREATED");
				failedTestInfo.addProperty("status", "NOT_YET_CREATED");
				failedTestInfo.addProperty("result", "NOT_YET_CREATED");
				failedTests.add(testModuleName, failedTestInfo);
			}
		}
		JsonObject failedPlanInfo = new JsonObject();
		if(!failedTests.isEmpty()) {
			failedPlanInfo.addProperty("error", "Unable to create certification package");
			failedPlanInfo.addProperty("error_description", "All tests have not been completed or tests have failed. ");
			failedPlanInfo.addProperty("plan_name", planName);
			failedPlanInfo.addProperty("test_plan_id", planId);
			failedPlanInfo.addProperty("variant", variant);
			failedPlanInfo.add("failed_tests", failedTests);
		}
		return failedPlanInfo;
	}

	@GetMapping(value = "/plan/exporthtml/{id}", produces = "application/zip")
	@Operation(summary = "Export the full results for this plan as both html and json in a zip")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Exported successfully"),
		@ApiResponse(responseCode = "404", description = "Couldn't find given plan Id")
	})
	public ResponseEntity<StreamingResponseBody> exportPlanAsHTML(
		HttpServletRequest httpRequest,
		@Parameter(description = "Id of plan") @PathVariable String id,
		@Parameter(description = "Published data only") @RequestParam(name = "public", defaultValue = "false") boolean publicOnly) {
		return exportPlanAsZip(id, false, publicOnly, false,  null);
	}


	@SuppressWarnings("JavaTimeDefaultTimeZone")
	protected ResponseEntity<StreamingResponseBody> exportPlanAsZip(String planId, boolean forCertification, boolean publicOnly, boolean addFolderForHtmlFiles,
																	MultipartFile clientSideData) {

		Object testPlan = publicOnly ? planService.getPublicPlan(planId) : planService.getTestPlan(planId);

		String planName = null;
		VariantSelection variant = null;

		List<Plan.Module> modules = new ArrayList<>();

		if (testPlan == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else if (testPlan instanceof PublicPlan plan) {
			planName = plan.getPlanName();
			variant = plan.getVariant();
			modules = plan.getModules();
		} else if (testPlan instanceof Plan plan) {
			planName = plan.getPlanName();
			variant = plan.getVariant();
			modules = plan.getModules();
		}

		//plan summary page
		PlanExportInfo planExportInfo = new PlanExportInfo(baseUrl, authenticationFacade.getPrincipal(), version, testPlan);

		for (Plan.Module module : modules) {

			String testModuleName = module.getTestModule();
			List<String> instances = module.getInstances();

			if (instances != null && !instances.isEmpty()) {

				String testId = instances.get(instances.size() - 1);

				List<Document> results = getTestResults(testId, null, publicOnly);

				Optional<?> testInfo = getTestInfo(publicOnly, testId);

				final TestExportInfo export = putTestResultToExport(results, testInfo);
				PlanExportInfo.TestExportInfoHolder testExportInfoHolder = new PlanExportInfo.TestExportInfoHolder(testId, testModuleName, export);
				planExportInfo.addTestExportInfoHolder(testExportInfoHolder);

			}
		}

		if (planExportInfo.getTestExportCount()<1) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		String fileDate = DateTimeFormatter.ofPattern("dd-MMM-yyyy").format(LocalDate.now());

		HttpHeaders headers = new HttpHeaders();

		headers.add("Content-Disposition", "attachment; filename=\"" + (Strings.isNullOrEmpty(planName) ? "" : (planName + "-")) + variantSuffix(variant) + planId + "-" + fileDate + ".zip\"");

		StreamingResponseBody responseBody = new StreamingResponseBody() {

			@Override
			public void writeTo(OutputStream out) throws IOException {

				try {
					ZipArchiveOutputStream archiveOutputStream = new ZipArchiveOutputStream(out);
					addPlanHTMLToZip(archiveOutputStream, testPlan, planExportInfo, htmlExportRenderer, addFolderForHtmlFiles, forCertification);

					// add all test logs file of a test plan to zip
					for (PlanExportInfo.TestExportInfoHolder testLogInfoExport : planExportInfo.getTestLogExports()) {

						String htmlFileName = TestHelper.generateHtmlFileName(testLogInfoExport.getTestModuleName(), testLogInfoExport.getTestId());

						String sigFileName = TestHelper.generateSigFileName(testLogInfoExport.getTestModuleName(), testLogInfoExport.getTestId());
						if(addFolderForHtmlFiles){
							htmlFileName = "test-logs/" + htmlFileName;
							sigFileName = "test-logs/" + sigFileName;
						}

						addHTMLFileToZip(archiveOutputStream, htmlFileName, sigFileName, testLogInfoExport.getExport(), htmlExportRenderer);

						String jsonLogFilename = "test-log-"+testLogInfoExport.getTestModuleName()+"-" + testLogInfoExport.getTestId() + ".json";
						String jsonLogSigFilename = "test-log-"+testLogInfoExport.getTestModuleName()+"-" + testLogInfoExport.getTestId() + ".json.sig";
						if(addFolderForHtmlFiles){
							jsonLogFilename = "test-logs/" + jsonLogFilename;
							jsonLogSigFilename = "test-logs/" + jsonLogSigFilename;
						}
						addFilesToZip(archiveOutputStream, jsonLogFilename, jsonLogSigFilename, testLogInfoExport.getExport());

					}

					if(clientSideData!=null && clientSideData.getSize()>0) {
						ZipArchiveEntry zipEntry = new ZipArchiveEntry("client-data/" + clientSideData.getOriginalFilename());
						zipEntry.setSize(clientSideData.getSize());
						archiveOutputStream.putArchiveEntry(zipEntry);
						archiveOutputStream.write(clientSideData.getBytes());
						archiveOutputStream.closeArchiveEntry();
					}
					archiveOutputStream.close();
				} catch (Exception ex) {
					throw new IOException(ex);
				}
			}
		};

		return ResponseEntity.ok().headers(headers).body(responseBody);
	}

	protected void addPlanHTMLToZip(ZipArchiveOutputStream archiveOutputStream,
									Object plan,
									PlanExportInfo planExportInfo,
									HtmlExportRenderer htmlExportRenderer,
									boolean addLogsFolder,
									boolean forCertification) throws Exception {

		String indexFilename = "index.html";
		String indexSigFilename = "index.html.sig";
		String indexJsonFilename = "index.json";
		String indexJsonSigFilename = "index.json.sig";
		if(addLogsFolder) {
			indexFilename = "test-logs/" + indexFilename;
			indexSigFilename = "test-logs/" + indexSigFilename;
			indexJsonFilename = "test-logs/" + indexJsonFilename;
			indexJsonSigFilename = "test-logs/" + indexJsonSigFilename;
		}
		ZipArchiveEntry testLog = new ZipArchiveEntry(indexFilename);

		Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(keyManager.getSigningPrivateKey());

		SignatureOutputStream signatureOutputStream = new SignatureOutputStream(archiveOutputStream, signature);

		String html = htmlExportRenderer.createHtmlForPlan(planExportInfo);
		byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);

		testLog.setSize(htmlBytes.length);
		archiveOutputStream.putArchiveEntry(testLog);

		signatureOutputStream.write(htmlBytes);

		signatureOutputStream.flush();
		signatureOutputStream.close();

		archiveOutputStream.closeArchiveEntry();

		ZipArchiveEntry signatureFile = new ZipArchiveEntry(indexSigFilename);

		byte[] src = signature.sign();
		String encodedSignature = Base64.getUrlEncoder().encodeToString(src);
		signatureFile.setSize(encodedSignature.getBytes().length);

		archiveOutputStream.putArchiveEntry(signatureFile);

		archiveOutputStream.write(encodedSignature.getBytes());

		archiveOutputStream.closeArchiveEntry();

		if(!forCertification){
			return;
		}
		//serializing the PlanInfo as json
		ZipArchiveEntry testJson = new ZipArchiveEntry(indexJsonFilename);

		String json = gson.toJson(planExportInfo.getPlanInfo());
		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

		testJson.setSize(jsonBytes.length);

		archiveOutputStream.putArchiveEntry(testJson);

		Signature jsonSignature = Signature.getInstance("SHA1withRSA");
		jsonSignature.initSign(keyManager.getSigningPrivateKey());
		SignatureOutputStream jsonSignatureOutputStream = new SignatureOutputStream(archiveOutputStream, jsonSignature);
		jsonSignatureOutputStream.write(jsonBytes);
		jsonSignatureOutputStream.flush();
		jsonSignatureOutputStream.close();
		archiveOutputStream.closeArchiveEntry();

		//Serializing the signature of the json file

		ZipArchiveEntry jsonSignatureFile = new ZipArchiveEntry(indexJsonSigFilename);
		encodedSignature = Base64.getUrlEncoder().encodeToString(jsonSignature.sign());
		jsonSignatureFile.setSize(encodedSignature.getBytes().length);
		archiveOutputStream.putArchiveEntry(jsonSignatureFile);
		archiveOutputStream.write(encodedSignature.getBytes());
		archiveOutputStream.closeArchiveEntry();


	}

	protected void addHTMLFileToZip(ZipArchiveOutputStream archiveOutputStream, String htmlFileName, String sigFileName,
									TestExportInfo export, HtmlExportRenderer htmlExportRenderer) throws Exception {

		ZipArchiveEntry testLog = new ZipArchiveEntry(htmlFileName);

		Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initSign(keyManager.getSigningPrivateKey());

		SignatureOutputStream signatureOutputStream = new SignatureOutputStream(archiveOutputStream, signature);

		String html = htmlExportRenderer.createHtmlForTestLogs(export);
		byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);

		testLog.setSize(htmlBytes.length);
		archiveOutputStream.putArchiveEntry(testLog);

		signatureOutputStream.write(htmlBytes);

		signatureOutputStream.flush();
		signatureOutputStream.close();

		archiveOutputStream.closeArchiveEntry();

		ZipArchiveEntry signatureFile = new ZipArchiveEntry(sigFileName);

		byte[] src = signature.sign();
		String encodedSignature = Base64.getUrlEncoder().encodeToString(src);
		signatureFile.setSize(encodedSignature.getBytes().length);

		archiveOutputStream.putArchiveEntry(signatureFile);

		archiveOutputStream.write(encodedSignature.getBytes());

		archiveOutputStream.closeArchiveEntry();
	}

	@GetMapping(value = "/log/exporthtml/{id}", produces = "application/zip")
	@Operation(summary = "Export test logs as html by test id")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Exported successfully"),
		@ApiResponse(responseCode = "404", description = "Couldn't find given test Id")
	})
	public ResponseEntity<StreamingResponseBody> exportTestHtml(
		@Parameter(description = "Id of test") @PathVariable String id,
		@Parameter(description = "Published data only") @RequestParam(name = "public", defaultValue = "false") boolean publicOnly) {
		List<Document> results = getTestResults(id, null, publicOnly);

		Optional<?> testInfo = getTestInfo(publicOnly, id);

		String testModuleName = null;
		VariantSelection variant = null;

		if (testInfo.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else if (testInfo.get() instanceof TestInfo) {
			testModuleName = ((TestInfo) testInfo.get()).getTestName();
			variant = ((TestInfo) testInfo.get()).getVariant();
		} else if (testInfo.get() instanceof PublicTestInfo) {
			testModuleName = ((PublicTestInfo) testInfo.get()).getTestName();
			variant = ((PublicTestInfo) testInfo.get()).getVariant();
		}

		HttpHeaders headers = new HttpHeaders();

		headers.add("Content-Disposition", "attachment; filename=\"test-log-" + (Strings.isNullOrEmpty(testModuleName) ? "" : (testModuleName + "-")) + variantSuffix(variant) + id + ".zip\"");

		final TestExportInfo export = putTestResultToExport(results, testInfo);
		final String testModuleNameFinal = testModuleName;
		StreamingResponseBody responseBody = new StreamingResponseBody() {

			@Override
			public void writeTo(OutputStream out) throws IOException {

				try {
					ZipArchiveOutputStream archiveOutputStream = new ZipArchiveOutputStream(out);

					String htmlFileName = TestHelper.generateHtmlFileName(testModuleNameFinal, id);

					String sigFileName = TestHelper.generateSigFileName(testModuleNameFinal, id);

					addHTMLFileToZip(archiveOutputStream, htmlFileName, sigFileName, export, htmlExportRenderer);

					addFilesToZip(archiveOutputStream, "test-log-"+testModuleNameFinal+"-" + id + ".json",
						"test-log-"+testModuleNameFinal+"-" + id + ".json.sig", export);

					archiveOutputStream.close();
				} catch (Exception ex) {
					throw new IOException(ex);
				}
			}
		};

		return ResponseEntity.ok().headers(headers).body(responseBody);
	}

	private static class SignatureOutputStream extends OutputStream {

		private OutputStream target;
		private Signature sig;

		/**
		 * creates a new SignatureOutputStream which writes to
		 * a target OutputStream and updates the Signature object.
		 */
		public SignatureOutputStream(OutputStream target, Signature sig) {
			this.target = target;
			this.sig = sig;
		}

		@Override
		public void write(int b)
			throws IOException
		{
			write(new byte[]{(byte)b});
		}

		@Override
		public void write(byte[] b)
			throws IOException
		{
			write(b, 0, b.length);
		}

		@Override
		public void write(byte[] b, int offset, int len)
			throws IOException
		{
			target.write(b, offset, len);
			try {
				sig.update(b, offset, len);
			}
			catch(SignatureException ex) {
				throw new IOException(ex);
			}
		}

		@Override
		public void flush()
			throws IOException
		{
			target.flush();
		}

		@Override
		public void close()
			throws IOException
		{
			// we don't close the target stream when we're done because we might keep writing to it later
			//target.close();
		}
	}

}
