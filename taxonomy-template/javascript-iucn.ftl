<script>

function changeYear() {
	var year = $("#yearSelector").val();
	var currentLocation = window.location.href;
	if (currentLocation.endsWith('/')) {
		currentLocation = currentLocation.substr(0, currentLocation.length - 1); 
	}
	var parts = currentLocation.split("/");
	var possibleYearPart = parts[parts.length-1];
	if (isNumeric(possibleYearPart)) {
		currentLocation = currentLocation.replace(possibleYearPart, "");
	}
	if (currentLocation.endsWith('/')) {
		currentLocation = currentLocation.substr(0, currentLocation.length - 1); 
	}
	window.location.href = currentLocation+'/'+year;
}

function isNumeric(num) {
    return !isNaN(num)
}
String.prototype.endsWith = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
};
</script>