<#include "luomus-header.ftl">
<#include "iucn-macro.ftl">

<a href="${baseURL}/iucn/${selectedYear}" class="goBack">Takaisin (IUCN-etusivu)</a>

<h1>Uhanalaisuusarviointi - ${selectedYear} <#if draftYear == selectedYear>(LUONNOS)</#if></h1>
<@toolbox/>

<div style="max-width:600px;">
<p style="float: right" class="info">Uhanalaisuusarvioijat: <@editors group.qname.toString() /></p>
<h2>${group.name.forLocale("fi")!""}</h2>
</div>


<div style="float: left; margin: 2em;">
<form action="${baseURL}/iucn/group/${group.qname}/${selectedYear}" method="get" id="filters"">
<fieldset style="max-width:600px;">
	<legend>Järjestys</legend>
	<select id="orderInput" name="orderBy" class="chosen">
		<option value="taxonomic" <#if !orderBy?? || orderBy = "taxonomic">selected="selected"</#if>>Taksonominen</option>
		<option value="alphabetic" <#if orderBy?? && orderBy = "alphabetic">selected="selected"</#if>>Aakkosjärjestys</option>
	</select>
	<br /><br />
</fieldset>
<fieldset style="max-width:600px;">
	<legend>Rajaa</legend>
	<label>Tilalla</label> 
		<select name="state" multiple="multiple" class="chosen" data-placeholder="Valitse">
			<option value=""></option>
			<option value="ready" <#if states?? && states?seq_contains("ready")>selected="selected"</#if>>Valmiit</option>
			<option value="readyForComments" <#if states?? && states?seq_contains("readyForComments")>selected="selected"</#if>>Valmiit kommentoitavaksi</option>
			<option value="started" <#if states?? && states?seq_contains("started")>selected="selected"</#if>>Aloitetut</option>
			<option value="notStarted" <#if states?? && states?seq_contains("notStarted")>selected="selected"</#if>>Aloittamattomat</option>
		</select>
	<br />
	<label>Luokalla ${selectedYear}</label>
		<select name="redListStatus" multiple="multiple" class="chosen" data-placeholder="Valitse">
			<option value=""></option>
			<#list statusProperty.range.values as value>
				<option value="${value.qname}" <#if redListStatuses?? && redListStatuses?seq_contains(value.qname)>selected="selected"</#if>>${value.label.forLocale("fi")}</option>
			</#list>
		</select>
	<br />
	<label>Edellisen arvioinnin luokalla</label>
		<select name="prevRedListStatus" multiple="multiple" class="chosen" data-placeholder="Valitse">
			<option value=""></option>
			<#list statusProperty.range.values as value>
				<option value="${value.qname}" <#if prevRedListStatuses?? && prevRedListStatuses?seq_contains(value.qname)>selected="selected"</#if>>${value.label.forLocale("fi")}</option>
			</#list>
		</select>
	<br />
	<label>Taksonomisesti</label>
		<input name="taxon" value="${taxon!""}"></input>
	<br />
	<p>
	<label>&nbsp;</label>
	<button>Rajaa</button>
	<button id="clearButton">Tyhjennä</button>
	</p>
	<#if filterError??>
		<p class="errorMessage">${filterError}</p>
	</#if>
</fieldset>
</form>
</div>

<div style="float: left; margin: 2em;">
<h6>Uusimmat kommentit</h6>
<#if remarks?has_content>
<table class="iucnSpeciesTable" style="font-size: 80%;">
	<tr>
		<th>Laji</th>
		<th>Pvm</th>
		<th>Kommentoija</th>
		<th>Kommentti</th>
	</tr>
	<#list remarks?reverse as remark>
		<tr>
			<td><a href="${baseURL}/iucn/species/${remark.target.qname}">${remark.target.scientificName!target.qname}</a></td>
			<td>${remark.date!""}</td>
			<td>${remark.personName!""}</td>
			<td>${remark.shortenedRemark?html}</td>
		</tr>
	</#list>
</table>
<#else>
	<span class="info">Ei kommentteja</span>
</#if>
</div>
<div class="clear"></div>


<div id="download">
	<button><img src="${staticURL}/img/CSV.png" alt="CSV"/>Lataa rajattujen lajien arvioinnit tiedostoon</button>
	<a target="_blank" href="https://beta.laji.fi/about/1068">Lue kuinka viet tiedostot Exceliin</a>
</div>
<div class="clear"></div>

<span id="species"></span>


<#if targets?has_content>
<table class="iucnSpeciesTable">
	<thead>
		<th>Lahko, Heimo</th>
		<th>Tieteellinen nimi</th>
		<th>Suomenkielinen nimi</th>
		<th>Tila <#if permissions>/ Pikatoiminnot</#if></th>
		<th>Muokattu</th>
		<th>Muokkaaja</th>
		<th>Luokka ${selectedYear}</th>
		<th class="redListIndexTableField">Indeksi ${selectedYear}</th>
		<th>Edellinen luokka</th>
		<th>Komm.</th>
	</thead>
	<#if (pageCount > 1)>
		<tfoot>
			<tr>
				<td colspan="7">
					<#if currentPage != 1>
					<span><a href="?page=${currentPage-1}#species">&laquo; Edellinen</a></span>
					|
					</#if> 
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
					<#if pageCount != currentPage>
					|
					<span><a href="?page=${currentPage+1}#species">Seuraava &raquo;</a></span>
					</#if>
					|
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
				<@speciesRow target selectedYear />
			</tr>
		</#list>
	</tbody>
</table>

<p class="info">Voit siirtyä katselemaan lajia klikkaamalla lajin nimeä.</p>
<#else>
	<p>Ei osumia</p>
</#if>

<div id="NAForm" class="evaluationEdit">
	<table>
		<@typeOfOccurrenceInFinland />
	</table>
	<p><button>Merkitse NA-luokkaan</button></p>
</div>

<div id="LCForm" class="evaluationEdit">
	<p class="info">Huomaa, että kun käytät tätä pikatoimintoa, mitään tietoja ei kopioida edellisestä arvioinnista!</p>
	<table>
		<@iucnHabitatFields />
	</table>
	<p><button>Merkitse LC-luokkaan</button></td></p>
</div>

<@propertyCommentsScript />

<script>
window.onpageshow = function(event) {
	if (event.persisted) {
    	window.location.reload() 
	}
};

$(function() {
	$("#NAForm, #LCForm").find('select').chosen();
	$("#NAForm, #LCForm").dialog({width: 700, height: 400, modal: true, autoOpen: false });
	
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
	$(".markNAButton").on('click', function() {
		var row = $(this).closest('tr');
		var speciesQname = row.attr('id');
		$("#NAForm").find('select').val('').trigger("chosen:updated");
		$("#NAForm").find('button').unbind('click').on('click', function() {
			var typeOfOccurrenceInFinland = $("#NAForm").find('select').first().val();
			var req = '${baseURL}/api/iucn-mark-not-applicable?speciesQname='+speciesQname+'&year=${selectedYear}&groupQname=${group.qname}&typeOfOccurrenceInFinland='+typeOfOccurrenceInFinland;
			$.post(req, function(data) {
				$("#NAForm").dialog("close");
				row.fadeOut('slow', function () {
					row.html(data);
					row.fadeIn('slow');
				});
			});
		});
		$("#NAForm").dialog("open");
	});
	$(".markLCButton").on('click', function() {
		var row = $(this).closest('tr');
		var speciesQname = row.attr('id');
		$("#LCForm").find('select').val('').trigger("chosen:updated");
		$("#LCForm").find('button').unbind('click').on('click', function() {
			var habitat = $("#LCForm").find('select').eq(0).val();
			var habitatSpecificTypes = $("#LCForm").find('select').eq(1).val();
			var req = '${baseURL}/api/iucn-mark-least-concern?speciesQname='+speciesQname+'&year=${selectedYear}&groupQname=${group.qname}';
			if (habitat) {
				req += '&habitat=' + habitat;
			} else {
				alert('Ensisijainen elinympäristö on ilmoitettava');
				return;
			}
			if (habitatSpecificTypes) {
				for (var i in habitatSpecificTypes) {
					var type = habitatSpecificTypes[i];
					if (type) {
						req += '&habitatSpecificType=' + type;
					} 
				}
			}
			$.post(req, function(data) {
				$("#LCForm").dialog("close");
				row.fadeOut('slow', function () {
					row.html(data);
					row.fadeIn('slow');
				});
			});
		});
		$("#LCForm").dialog("open");
	});
	
	$("#pageSelector, #pageSizeSelector").on('change', function() {
		var defaultPageSize = ${defaultPageSize};
		var page = $("#pageSelector").val();
		var pageSize = $("#pageSizeSelector").val();
		url = "?page=" + page;
		if (pageSize != defaultPageSize) {
			url += "&pageSize=" + pageSize;
		}
		window.location.href = url+"#species";
	});
	
	$("#clearButton").on('click', function() {
    	window.location.href = '${baseURL}/iucn/group/${group.qname}/${selectedYear}?clearFilters=true';
    	return false;
	});
	
	$("#orderInput").on('change', function() {
		$("#filters").submit();
	});
	
	$(".remarks").tooltip();
	
	$("#download button").on('click', function() {
		if (window.location.href.indexOf("?") != -1) {
			window.location.href = window.location.href + "&download=true";
		} else {
			window.location.href = window.location.href + "?download=true";
		}
	});
});
function hideRedListIndexTableFields() {
	<#if selectedYear == draftYear>
		$(".redListIndexTableField").hide();
	<#else>
		// nop
	</#if>
}

hideRedListIndexTableFields();
</script>

<#include "luomus-footer.ftl">