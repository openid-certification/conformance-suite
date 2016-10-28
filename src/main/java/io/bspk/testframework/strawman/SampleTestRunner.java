/*******************************************************************************
 * Copyright 2016 The MITRE Corporation
 *   and the MIT Internet Trust Consortium
 *
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

package io.bspk.testframework.strawman;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author jricher
 *
 */
public class SampleTestRunner {
	
	private static Logger logger = LoggerFactory.getLogger(SampleTestRunner.class);
	
	public static void main(String[] args) {
		
		
		TestModule test = new SampleTestModule();
		
		logger.info("Created: " + test.getId());
		
		logger.info("Status of " + test.getId() + ": " + test.getStatus());
		
		
		JsonObject config = new JsonParser().parse("{}").getAsJsonObject();
		
		EventLog eventLog = new SampleEventLog(test.getId());
		
		test.configure(config, eventLog);
		
		logger.info("Status of " + test.getId() + ": " + test.getStatus());
		
		test.start();
		
		logger.info("Status of " + test.getId() + ": " + test.getStatus());
	
	}
	

}
