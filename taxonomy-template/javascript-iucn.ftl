<script>

$(function() {
});

function changeYear() {
	var year = $("#yearSelector").val();
	window.location.href = '${baseURL}/iucn/'+year;
}


</script>