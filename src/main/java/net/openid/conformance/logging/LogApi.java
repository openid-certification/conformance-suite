package net.openid.conformance.logging;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.SignatureException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

	@GetMapping(value = "/log", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Get all test logs with paging", notes = "Return all published logs when public data is requested, otherwise all test logs if user is admin, or only the user's test logs")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Retrieved successfully")
	})
	public ResponseEntity<Object> getAllTests(
		@ApiParam(value = "Published data only") @RequestParam(name = "public", defaultValue = "false") boolean publicOnly,
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
	@ApiOperation(value = "Get test log of given testId")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Retrieved successfully")
	})
	public ResponseEntity<List<Document>> getLogResults(
		@ApiParam(value = "Id of test") @PathVariable("id") String id,
		@ApiParam(value = "Since when test created") @RequestParam(value = "since", required = false) Long since,
		@ApiParam(value = "Published data only") @RequestParam(name = "public", defaultValue = "false") boolean publicOnly) {
		List<Document> results = getTestResults(id, since, publicOnly);

		return ResponseEntity.ok().body(results);
	}

	@GetMapping(value = "/log/export/{id}", produces = "application/zip")
	@ApiOperation(value = "Export test log by test id")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Exported successfully"),
		@ApiResponse(code = 404, message = "Couldn't find given test Id")
	})
	public ResponseEntity<StreamingResponseBody> export(
		@ApiParam(value = "Id of test") @PathVariable("id") String id,
		@ApiParam(value = "Published data only") @RequestParam(name = "public", defaultValue = "false") boolean publicOnly) {
		List<Document> results = getTestResults(id, null, publicOnly);

		Optional<?> testInfo = getTestInfo(publicOnly, id);

		String testModuleName = null;
		VariantSelection variant = null;

		if (!testInfo.isPresent()) {
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
	@ApiOperation(value = "Export all test logs of plan by plan id")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Exported successfully"),
		@ApiResponse(code = 404, message = "Couldn't find given plan Id")
	})
	public ResponseEntity<StreamingResponseBody> exportLogsOfPlan(
		@ApiParam(value = "Id of plan") @PathVariable("id") String id,
		@ApiParam(value = "Published data only") @RequestParam(name = "public", defaultValue = "false") boolean publicOnly) {

		Object testPlan = publicOnly ? planService.getPublicPlan(id) : planService.getTestPlan(id);

		String planName = null;
		VariantSelection variant = null;

		List<Plan.Module> modules = new ArrayList<>();

		if (testPlan == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else if (testPlan instanceof PublicPlan) {
			planName = ((PublicPlan) testPlan).getPlanName();
			variant = ((PublicPlan) testPlan).getVariant();
			modules = ((PublicPlan) testPlan).getModules();
		} else if (testPlan instanceof Plan) {
			planName = ((Plan) testPlan).getPlanName();
			variant = ((Plan) testPlan).getVariant();
			modules = ((Plan) testPlan).getModules();
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

						@SuppressWarnings("unchecked")
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

		String encodedSignature = Base64Utils.encodeToUrlSafeString(signature.sign());
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

	private List<Document> getTestResults(String id, Long since, boolean isPublic) {
		boolean summaryOnly;

		if (isPublic) {
			// Check publish status of test
			Optional<PublicTestInfo> testInfo = testInfos.findByIdPublic(id);
			if (!testInfo.isPresent()) {
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
	@ApiOperation(value = "Prepare certification package for a test plan. Also publishes the plan and marks it as immutable.")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Prepared successfully"),
		@ApiResponse(code = 403, message = "Could not publish plan"),
		@ApiResponse(code = 404, message = "Could not find a plan with the given id"),
		@ApiResponse(code = 422, message = "Could not mark the plan as immutable")
	})
	public ResponseEntity<StreamingResponseBody> prepareCertificationPackageForTestPlan(
		@ApiParam(value = "Id of test plan") @PathVariable("id") String id,
		@ApiParam(value = "Signed certification of conformance pdf") @RequestParam("certificationOfConformancePdf") MultipartFile certificationOfConformancePdf,
		@ApiParam(value = "Client data in zip format. Only required for RP tests") @RequestParam("clientSideData") MultipartFile clientSideData

	) {
		if (!planService.publishTestPlan(id, "everything")) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		if (!planService.changeTestPlanImmutableStatus(id, Boolean.TRUE)) {
			return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
		}
		return exportPlanAsZip(id, false, true, certificationOfConformancePdf, clientSideData);
	}


	@GetMapping(value = "/plan/exporthtml/{id}", produces = "application/zip")
	@ApiOperation(value = "Export the full results for this plan as both html and json in a zip")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Exported successfully"),
		@ApiResponse(code = 404, message = "Couldn't find given plan Id")
	})
	public ResponseEntity<StreamingResponseBody> exportPlanAsHTML(
		HttpServletRequest httpRequest,
		@ApiParam(value = "Id of plan") @PathVariable("id") String id,
		@ApiParam(value = "Published data only") @RequestParam(name = "public", defaultValue = "false") boolean publicOnly) {
		return exportPlanAsZip(id, publicOnly, false, null, null);
	}


	protected ResponseEntity<StreamingResponseBody> exportPlanAsZip(String planId, boolean publicOnly, boolean addFolderForHtmlFiles,
																	MultipartFile certificationOfConformancePdf,
																	MultipartFile clientSideData) {

		Object testPlan = publicOnly ? planService.getPublicPlan(planId) : planService.getTestPlan(planId);

		String planName = null;
		VariantSelection variant = null;

		List<Plan.Module> modules = new ArrayList<>();

		if (testPlan == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else if (testPlan instanceof PublicPlan) {
			planName = ((PublicPlan) testPlan).getPlanName();
			variant = ((PublicPlan) testPlan).getVariant();
			modules = ((PublicPlan) testPlan).getModules();
		} else if (testPlan instanceof Plan) {
			planName = ((Plan) testPlan).getPlanName();
			variant = ((Plan) testPlan).getVariant();
			modules = ((Plan) testPlan).getModules();
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
					addPlanHTMLToZip(archiveOutputStream, testPlan, planExportInfo, htmlExportRenderer, addFolderForHtmlFiles);

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

					if(certificationOfConformancePdf!=null && certificationOfConformancePdf.getSize()>0) {
						ZipArchiveEntry zipEntry = new ZipArchiveEntry("OpenID-Certification-of-Conformance.pdf");
						zipEntry.setSize(certificationOfConformancePdf.getSize());
						archiveOutputStream.putArchiveEntry(zipEntry);
						archiveOutputStream.write(certificationOfConformancePdf.getBytes());
						archiveOutputStream.closeArchiveEntry();
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
									boolean addLogsFolder) throws Exception {

		String indexFilename = "index.html";
		String indexSigFilename = "index.html.sig";
		if(addLogsFolder) {
			indexFilename = "test-logs/" + indexFilename;
			indexSigFilename = "test-logs/" + indexSigFilename;
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

		String encodedSignature = Base64Utils.encodeToUrlSafeString(signature.sign());
		signatureFile.setSize(encodedSignature.getBytes().length);

		archiveOutputStream.putArchiveEntry(signatureFile);

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

		String encodedSignature = Base64Utils.encodeToUrlSafeString(signature.sign());
		signatureFile.setSize(encodedSignature.getBytes().length);

		archiveOutputStream.putArchiveEntry(signatureFile);

		archiveOutputStream.write(encodedSignature.getBytes());

		archiveOutputStream.closeArchiveEntry();
	}

	@GetMapping(value = "/log/exporthtml/{id}", produces = "application/zip")
	@ApiOperation(value = "Export test logs as html by test id")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Exported successfully"),
		@ApiResponse(code = 404, message = "Couldn't find given test Id")
	})
	public ResponseEntity<StreamingResponseBody> exportTestHtml(
		@ApiParam(value = "Id of test") @PathVariable("id") String id,
		@ApiParam(value = "Published data only") @RequestParam(name = "public", defaultValue = "false") boolean publicOnly) {
		List<Document> results = getTestResults(id, null, publicOnly);

		Optional<?> testInfo = getTestInfo(publicOnly, id);

		String testModuleName = null;
		VariantSelection variant = null;

		if (!testInfo.isPresent()) {
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

		public void write(int b)
			throws IOException
		{
			write(new byte[]{(byte)b});
		}

		public void write(byte[] b)
			throws IOException
		{
			write(b, 0, b.length);
		}

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

		public void flush()
			throws IOException
		{
			target.flush();
		}

		public void close()
			throws IOException
		{
			// we don't close the target stream when we're done because we might keep writing to it later
			//target.close();
		}
	}

}
