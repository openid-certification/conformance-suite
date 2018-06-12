/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

import io.fintechlabs.testframework.CollapsingGsonHttpMessageConverter;
import io.fintechlabs.testframework.info.DBTestInfoService;
import io.fintechlabs.testframework.security.AuthenticationFacade;
import io.fintechlabs.testframework.security.KeyManager;

/**
 * @author jricher
 *
 */
@Controller
public class LogApi {

	@Value("${fintechlabs.base_url:http://localhost:8080}")
	private String baseUrl;
	
	@Value("${fintechlabs.version}")
	private String version;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@Autowired
	private KeyManager keyManager;
	
	private Gson gson = CollapsingGsonHttpMessageConverter.getDbObjectCollapsingGson();

	@GetMapping(value = "/log", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<DBObject>> getAllTests() {

		DBObject queryFilter;
		if (authenticationFacade.isAdmin()) {
			queryFilter = BasicDBObjectBuilder.start().get();
		} else {
			ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();
			queryFilter = BasicDBObjectBuilder.start().add("testOwner", owner).get();
		}

		@SuppressWarnings("unchecked")
		List<String> testIds = mongoTemplate.getCollection(DBEventLog.COLLECTION).distinct("testId", queryFilter);

		List<DBObject> results = new ArrayList<>(testIds.size());

		for (String testId : testIds) {
			// fetch the test object from the info log if available
			DBObject testInfo = mongoTemplate.getCollection(DBTestInfoService.COLLECTION).findOne(testId);

			if (testInfo == null) {
				// make a fake document with just the ID
				results.add(BasicDBObjectBuilder.start("_id", testId).get());
			} else {
				// otherwise, add everything
				results.add(testInfo);
			}
		}

		return new ResponseEntity<>(results, HttpStatus.OK);

	}

	@GetMapping(value = "/log/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<DBObject>> getLogResults(@PathVariable("id") String id, @RequestParam(value = "since", required = false) Long since) {
		List<DBObject> results = getTestResults(id, since);
		
		return ResponseEntity.ok().body(results);
	}
	
	@GetMapping(value = "/log/export/{id}", produces = "application/x-gtar")
	public ResponseEntity<StreamingResponseBody> export(@PathVariable("id") String id) {
		List<DBObject> results = getTestResults(id);

		DBObject testInfo = null;
		if (authenticationFacade.isAdmin()) {
			testInfo = mongoTemplate.getCollection(DBTestInfoService.COLLECTION).findOne(id);
		} else {
			ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();
			if (owner != null) {
				testInfo = mongoTemplate.getCollection(DBTestInfoService.COLLECTION).findOne(BasicDBObjectBuilder.start().add("_id", id).add("owner", owner).get());
			}
		}

		if (testInfo == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		HttpHeaders headers = new HttpHeaders();

		headers.add("Content-Disposition", "attachment; filename=\"test-log-" + id + ".tar.bz2\"");
	
		final Map<String, Object> export = new HashMap<>();
		
		export.put("exportedAt", new Date());
		export.put("exportedFrom", baseUrl);
		export.put("exportedBy", authenticationFacade.getPrincipal());
		export.put("exportedVersion", version);
		export.put("testInfo", testInfo);
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

	/**
	 * @param id
	 * @return
	 */
	private List<DBObject> getTestResults(String id) {
		return getTestResults(id, null);
	}

	private List<DBObject> getTestResults(String id, Long since) {
		Criteria criteria = new Criteria();
		criteria.and("testId").is(id);
		
		if (!authenticationFacade.isAdmin()) {
			criteria.and("owner").is(authenticationFacade.getPrincipal());
		}
		
		if (since != null) {
			criteria.and("time").gt(since);
		}
		
		List<DBObject> results = mongoTemplate.getCollection(DBEventLog.COLLECTION).find(criteria.getCriteriaObject())
			.sort(BasicDBObjectBuilder.start()
				.add("time", 1)
				.get())
			.toArray();
		return results;
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
