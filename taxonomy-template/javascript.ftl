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

</script>
