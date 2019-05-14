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
	searchTaxon(taxonSearchForm, false, false);
}

function searchTaxon(taxonSearchForm, onlySpecies, onlyFinnish) {
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
	} else if (taxonpageBaseLinkType === "iucnRegionalEdit") {
		taxonpageBaseLinkURL = '${(baseURL + "/iucn/regional")?url}';
	}
	var taxonpageURLPostfix = '';
	if ($(taxonSearchForm).attr('taxonpageLinkPostfix')) {
		taxonpageURLPostfix = $(taxonSearchForm).attr('taxonpageLinkPostfix');
	}
	var resultViewContainer = $(taxonSearchForm).find('div:first');
	resultViewContainer.html('').removeClass('collapse');
	resultViewContainer.hide();
	$("body").css("cursor", "progress");
	$.get('${baseURL}/api/taxonomy-search-content/?q='+encodeURIComponent(taxon)+'&onlySpecies='+onlySpecies+'&onlyFinnish='+onlyFinnish+'&locale=en<#if checklist??>&checklist=${checklist.qname}</#if>&taxonpageBaseLinkURL='+taxonpageBaseLinkURL+'&taxonpageURLPostfix='+taxonpageURLPostfix, function(data) {
		resultViewContainer.html(data);
		if (data.length > 5000){
			resultViewContainer.collapse({showMoreText: '+ Show more'});
		}
		resultViewContainer.fadeIn(1000);
	  	$("body").css("cursor", "default");
  	});
}

</script>
