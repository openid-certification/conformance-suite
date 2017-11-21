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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.mongodb.DBObject;

import io.fintechlabs.testframework.info.DBTestInfoService;

/**
 * @author jricher
 *
 */
@Controller
public class TestInfoApi {

	@Autowired
	private MongoTemplate mongoTemplate;

	
	@GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<DBObject>> getAllTests() {
		
		List<DBObject> testInfo = mongoTemplate.getCollection(DBTestInfoService.COLLECTION).find().toArray();
		
		return new ResponseEntity<>(testInfo, HttpStatus.OK);
		
	}

	@GetMapping(value = "/info/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getTestInfo(@PathVariable("id") String id) {
		
		DBObject testInfo = mongoTemplate.getCollection(DBTestInfoService.COLLECTION).findOne(id);

		if (testInfo == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else {
			return new ResponseEntity<>(testInfo, HttpStatus.OK);
		}
		
	}
	
}
