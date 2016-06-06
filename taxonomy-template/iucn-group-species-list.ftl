<#include "luomus-header.ftl">
<#include "iucn-macro.ftl">

<h1>Uhanalaisuusarviointi - ${selectedYear} <#if draftYear == selectedYear>(LUONNOS)</#if></h1>
<@toolbox/>

<h2>${group.name.forLocale("fi")!""}</h2>
<p class="info">Uhanalaisuusarviojat: <@editors group /></p>


<table class="iucnSpeciesTable">
	<thead>
		<th>Tieteellinen nimi</th>
		<th>Suomenkielinen nimi</th>
		<th>Tila</th>
		<th>Muokattu</th>
		<th>Muokkaaja</th>
		<th>Luokka</th>
		<th>Indeksi</th>
	</thead>
	<tbody>
		<#list yearData.species as species>
			<tr class="iucnTaxonRow" id="${species.qname}">
				<@speciesRow species yearData.getEvaluation(species.qname) />
			</tr>
		</#list>
	</tbody>
</table>

<p class="info">Voit siirtyä katselemaan lajia klikkaamalla lajin nimeä.</p>

<script>
$(function() {

	$(".markNEButton").on('click', function() {
		var row = $(this).closest('tr');
		var speciesQname = row.attr('id');
		$.post('${baseURL}/api/iucn-mark-not-evaluated?speciesQname='+speciesQname+'&year=${selectedYear}&groupQname=${group.qname}', function(data) {
			row.fadeOut('slow', function () {
				row.html(data);
				row.fadeIn('slow');
			});
		});
	});
	
});
</script>

<#include "luomus-footer.ftl">