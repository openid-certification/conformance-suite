var FAPI_UI = {

	visibleFields : ["msg", "src", "time", "result", "requirements", "upload"],
	
	/**
	 * 
	 */
	goHome : function() {
		window.location.replace("logs.html");
	},

	/**
	 * 
	 */
	loadAvailableLogs : function() {
		$.ajax({ 
	        type: 'GET', 
	        url: "log", 
	        data: {}, 
	        success: render
	    });
	},

	/**
	 * 
	 */
	showLogDetail : function(fileId) {
		$.ajax({ 
	        type: 'GET', 
	        url: "log/" + encodeURIComponent(fileId), 
	        data: {}, 
	        success: function (data) { 

			

				render("LogStart", fileId);

	            $.each(data, function(i, item) {

	     
	            	
	            	$.each(item, function(key, val) {
	 
	            		if (!key.startsWith("_") // filter out anything starting with an underscore as it's injected by Mongo
	            				&& key.toLowerCase() != 'testid' // we also don't need to display the test ID
	            				) {

	            			if (key.toLowerCase()=="src") {
	            				render("Source",val);
	            			}

	            			if (key.toLowerCase()=="msg") {
	            				render("Message",val);
	            			}

	            			if (key.toLowerCase()=="requirements") {
	            				render("Requirements",val);
	            			}

	            			if (key.toLowerCase()=="upload") {
	            				render("Upload",val);
	            			}

	            			if (key.toLowerCase()=="result") {
	            				render("Result",val);
	            			}

	          
	            			if (key.toLowerCase()=="time") {
	            				render("Time",new Date(val));
	            			}
	            		}
	            	});

				
					
	            });

				//render(fileId, logs);
	        }
	    });
	}
	
}

