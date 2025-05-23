<script>

function changeYear() {
	var year = $("#yearSelector").val();
	var currentLocation = window.location.href;
	var currentySelectedYear = '${selectedYear!""}';
	var newLocation = "";
	if (currentLocation.endsWith("/iucn") || currentLocation.endsWith("/iucn/")) {
		if (!currentLocation.endsWith("/")) {
			currentLocation = currentLocation + "/";
		}
		newLocation = currentLocation + year; 
	} else {
		newLocation = currentLocation.replace(currentySelectedYear, year);
	}
	window.location.href = newLocation;
}

function isNumeric(num) {
    return !isNaN(num)
}
String.prototype.endsWith = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
};
String.prototype.contains = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
};
</script>