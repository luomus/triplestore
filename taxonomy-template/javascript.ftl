<script>

$(function() {
	
	if ($("body").attr("class") === "oldie") {
		var text = 'Your browser is not supported. Please use Mozilla Firefox, Google Chrome  (both are reccomended) or newer version of IE (10,11) (not reccomended)'; 
		$("html").html(text);
		alert(text);
	}
	
	$(document).not("#editTaxonDescriptions").tooltip({
		position: {
			 my: "left bottom", 
			 at: "left top-7"
		}
	});
			
	$("input[type=submit], button, .button").button();
	
	$(".chosen").chosen({ search_contains: true, allow_single_deselect: true });
	
	$('input, textarea').placeholder();
		
	$("form").on('submit', function() {
		if ($(this).valid()) {
			$(this).find('.addButton').hide();
		}
	});
	
	$(".datepicker").datepicker({dateFormat: "yy-mm-dd"});
});

function searchTaxon(taxonSearchForm) {
	var taxon = $(taxonSearchForm).find('input:first').val();
	taxon = taxon.replace("%", "");
	var taxonpageBaseLinkType = $(taxonSearchForm).attr('taxonpageBaseLinkType');
	var taxonpageBaseLinkURL = '${(baseURL + "/orphan")?url}';
	if (taxonpageBaseLinkType === "taxonTree") {
		taxonpageBaseLinkURL = '${(baseURL)?url}';
	} else if (taxonpageBaseLinkType === "taxonDescriptions") {
		taxonpageBaseLinkURL = '${(baseURL + "/taxon-descriptions")?url}';
	} else if (taxonpageBaseLinkType === "iucnEdit") {
		taxonpageBaseLinkURL = '${(baseURL + "/iucn/species")?url}';
	}
	var resultViewContainer = $(taxonSearchForm).find('div:first');
	resultViewContainer.html('').removeClass('collapse');
	resultViewContainer.hide();
	$("body").css("cursor", "progress");
	$.get('${baseURL}/api/taxonomy-search-content/'+encodeURIComponent(taxon)+'?locale=en<#if checklist??>&checklist=${checklist.qname}</#if>&taxonpageBaseLinkURL='+taxonpageBaseLinkURL, function(data) {
		resultViewContainer.html(data);
		resultViewContainer.fadeIn(1000);
		setTimeout(function() {
			if (resultViewContainer.height() > 200) {
				resultViewContainer.collapse({showMoreText: '+ Show more'});
			}
	  		$("body").css("cursor", "default");
	  	}, 50);
  	});
}

</script>
