package io.fintechlabs.testframework.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

import io.fintechlabs.testframework.CollapsingGsonHttpMessageConverter;
import io.fintechlabs.testframework.info.PublicTestInfo;
import io.fintechlabs.testframework.info.TestInfo;
import io.fintechlabs.testframework.info.TestInfoRepository;
import io.fintechlabs.testframework.pagination.PaginationRequest;
import io.fintechlabs.testframework.pagination.PaginationResponse;
import io.fintechlabs.testframework.security.AuthenticationFacade;
import io.fintechlabs.testframework.security.KeyManager;

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

	private Gson gson = CollapsingGsonHttpMessageConverter.getDbObjectCollapsingGson();

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

	@GetMapping(value = "/log/export/{id}", produces = "application/x-gtar")
	@ApiOperation(value = "Export test log by test id")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Exported successfully"),
		@ApiResponse(code = 404, message = "Couldn't find given test Id")
	})
	public ResponseEntity<StreamingResponseBody> export(
		@ApiParam(value = "Id of test") @PathVariable("id") String id,
		@ApiParam(value = "Published data only") @RequestParam(name = "public", defaultValue = "false") boolean publicOnly) {
		List<Document> results = getTestResults(id, null, publicOnly);

		Optional<?> testInfo = Optional.empty();
		String testModuleName = null;

		if (publicOnly) {
			testInfo = testInfos.findByIdPublic(id);
		} else if (authenticationFacade.isAdmin()) {
			testInfo = testInfos.findById(id);
		} else {
			ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();
			if (owner != null) {
				testInfo = testInfos.findByIdAndOwner(id, owner);
			}
		}

		if (!testInfo.isPresent()) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else if (testInfo.get() instanceof TestInfo) {
			testModuleName = ((TestInfo) testInfo.get()).getTestName();
		} else if (testInfo.get() instanceof PublicTestInfo) {
			testModuleName = ((PublicTestInfo) testInfo.get()).getTestName();
		}

		if (testModuleName == null)
			testModuleName = "";

		HttpHeaders headers = new HttpHeaders();

		headers.add("Content-Disposition", "attachment; filename=\"test-log-" + testModuleName + "-" + id + ".tar.bz2\"");

		final Map<String, Object> export = new HashMap<>();

		export.put("exportedAt", new Date());
		export.put("exportedFrom", baseUrl);
		export.put("exportedBy", authenticationFacade.getPrincipal());
		export.put("exportedVersion", version);
		export.put("testInfo", testInfo.get());
		export.put("results", results);

		StreamingResponseBody responseBody = new StreamingResponseBody() {

			@Override
			public void writeTo(OutputStream out) throws IOException {

				try {
					BZip2CompressorOutputStream compressorOutputStream = new BZip2CompressorOutputStream(out);

					TarArchiveOutputStream archiveOutputStream = new TarArchiveOutputStream(compressorOutputStream);

					TarArchiveEntry testLog = new TarArchiveEntry("test-log-" + id + ".json");

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

					TarArchiveEntry signatureFile = new TarArchiveEntry("test-log-" + id + ".sig");

					String encodedSignature = Base64Utils.encodeToUrlSafeString(signature.sign());
					signatureFile.setSize(encodedSignature.getBytes().length);

					archiveOutputStream.putArchiveEntry(signatureFile);

					archiveOutputStream.write(encodedSignature.getBytes());

					archiveOutputStream.closeArchiveEntry();

					archiveOutputStream.close();
				} catch (Exception ex) {
					throw new IOException(ex);
				}
			}
		};

		return ResponseEntity.ok().headers(headers).body(responseBody);
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
