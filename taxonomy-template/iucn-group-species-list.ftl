<#include "luomus-header.ftl">
<#include "iucn-macro.ftl">

<a href="${baseURL}/iucn/${selectedYear}" class="goBack">Takaisin (IUCN-etusivu)</a>

<h1>Uhanalaisuusarviointi - ${selectedYear} <#if draftYear == selectedYear>(LUONNOS)</#if></h1>
<@toolbox/>

<h2>${group.name.forLocale("fi")!""}</h2>
<p class="info">Uhanalaisuusarviojat: <@editors group.qname.toString() /></p>


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
	<#if (pageCount > 1)>
		<tfoot>
			<tr>
				<td colspan="7">
					<span>
					Sivu
						<select id="pageSelector">
							<#list 1 .. pageCount as page>
								<#if page == currentPage>
									<option value="${page}" selected="selected">${page}</option>
								<#else>
									<option value="${page}">${page}</option>
								</#if>
							</#list>
						</select> 
					/ ${pageCount}
					</span>
					<span>
						Lajeja sivulla 
						<input size="5" maxlength="4" type="text" value="${pageSize}" id="pageSizeSelector" />
					</span>
				</td>
			</tr>
		</tfoo>
	</#if>
	<tbody>
		<#list targets as target>
			<tr class="iucnTaxonRow" id="${target.qname}">
				<@speciesRow target target.getEvaluation(selectedYear) />
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
	
	$("#pageSelector, #pageSizeSelector").on('change', function() {
		var url = window.location.href.split('?')[0];
		var defaultPageSize = ${defaultPageSize};
		var page = $("#pageSelector").val();
		var pageSize = $("#pageSizeSelector").val();
		url += "?page=" + page;
		if (pageSize != defaultPageSize) {
			url += "&pageSize=" + pageSize;
		}
		window.location.href = url;
	});
	
});
</script>

<#include "luomus-footer.ftl">