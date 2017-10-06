var FAPI_UI = {

	goHome : function() {
		window.location.replace("logs.html");
	},

	/**
	 * 
	 */
	loadContents :  function() {
		$.ajax({ 
	        type: 'GET', 
	        url: "log", 
	        data: {}, 
	        success: function (data) { 

	            FAPI_UI.renderTableOfContents(data);

	        }
	    });
	},

	/**
	 * 
	 */
	renderTableOfContents : function(fileIds) {

		for (var i=0;i<fileIds.length;i++) {
			var item = fileIds[i];
			$("#logs .content").append("<div class='link'><a href='log-detail.html?log="+item+"' class='item'>"+item+"</a></div>");
		}
	},

	/**
	 * 
	 */
	showLogDetail : function(name) {

		// load the project JSON
		var file = "log/" + encodeURIComponent(name);

		$.ajax({ 
	        type: 'GET', 
	        url: file, 
	        data: {}, 
	        success: function (data) { 

	            $.each(data, function(i, item) {
	            	$.each(item, function(key, val) {
	 
	            		//filter out anything starting with an underscore as its injected by Mongo
	            		if (key.charAt(0) != "_") {
	            			var str = "<div>";
	            			str += "<span class='key'>"+key+"</span>";

	            			if (key=="time") {
	            				str += "<span class='hypen'> : </span><span class='value'>" + new Date(val) + "</span>";
	            			} else {
	            				str += "<span class='hypen'> : </span><span class='value'>" + val + "</span>";
	            			}

	            			str += "</div>";
	            			$("#logDetail .content").append(str);
	            		}
	            	});

	            	$("#logDetail .content").append("<hr />");

	            });

	        }
	    });

	}
	
}

