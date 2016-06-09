<#include "luomus-header.ftl">
<#include "iucn-macro.ftl">

<a href="${baseURL}/iucn/${selectedYear}" class="goBack">Takaisin (IUCN-etusivu)</a>

<h1>Uhanalaisuusarviointi - ${selectedYear} <#if draftYear == selectedYear>(LUONNOS)</#if></h1>
<@toolbox/>

<div style="max-width:600px;">
<p style="float: right" class="info">Uhanalaisuusarviojat: <@editors group.qname.toString() /></p>
<h2>${group.name.forLocale("fi")!""}</h2>
</div>

<form action="${baseURL}/iucn/group/${group.qname}/${selectedYear}" method="get" id="filters"">
<fieldset style="max-width:600px;">
	<legend>Rajaa</legend>
	<button id="clearButton" style="float: right;">Tyhjennä</button>
	<button style="float: right;">Rajaa</button>
	<label>Tilalla</label> 
		<select name="state" multiple="multiple" class="chosen" data-placeholder="Valitse">
			<option value=""></option>
			<option value="ready" <#if states?? && states?seq_contains("ready")>selected="selected"</#if>>Valmiit</option>
			<option value="started" <#if states?? && states?seq_contains("started")>selected="selected"</#if>>Aloitetut</option>
			<option value="notStarted" <#if states?? && states?seq_contains("notStarted")>selected="selected"</#if>>Aloittamattomat</option>
		</select>
	<br />
	<label>Luokalla</label>
		<select name="redListStatus" multiple="multiple" class="chosen" data-placeholder="Valitse">
			<option value=""></option>
			<#list statusProperty.range.values as value>
				<option value="${value.qname}" <#if redListStatuses?? && redListStatuses?seq_contains(value.qname)>selected="selected"</#if>>${value.label.forLocale("fi")}</option>
			</#list>
		</select>
	<br />
	<label>Taksonomisesti</label>
		<input name="taxon" value="${taxon!""}"></input>
	<br />
	<#if filterError??>
		<p class="errorMessage">${filterError}</p>
	</#if>
</fieldset>
</form>

<#if targets?has_content>
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
<#else>
	<p>Ei osumia</p>
</#if>

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
		<#if redListStatuses??>
			<#list redListStatuses as status>
				url += "&redListStatus=${status}";
			</#list>
		</#if>
		<#if states??>
			<#list states as state>
				url += "&state=${state}";
			</#list>
		</#if>
		<#if taxon?has_content>
			url += "&taxon=${taxon}";
		</#if>
		window.location.href = url;
	});
	
	$("#clearButton").on('click', function() {
    	$("#filters").find('input, select').each(function() {
    		$(this).val('');
    	});
	    $("#filters").find('select').each(function() {
    	    $(this).trigger('chosen:updated');
    	});
    	return false;
	});

});
</script>

<#include "luomus-footer.ftl">