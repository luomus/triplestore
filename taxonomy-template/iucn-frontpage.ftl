<#include "luomus-header.ftl">
<#include "iucn-macro.ftl">

<h1>Uhanalaisuusarviointi - ${selectedYear} <#if draftYear == selectedYear>(LUONNOS)</#if></h1>

<ul>
<li> <a href="${staticURL}/IUCN-käyttöohje.pdf" target="_blank">Vanha käyttöohje</a> (Päivitetty versio tulossa 2026) </li>
<li> <a href="${staticURL}/Elinympäristöluokkien_tulkintaohje_v1_1.pdf" target="_blank">Vanha elinympäristöluokkien tulkintaohje </a> </li>
<li> Elinympäristöluokkien tulkintaohje (Päivitetty versio tulossa 2026) </li>
<li> <a href="https://laji.fi/map" target="_blank">Laji.fi/map</a> - Arviointialueiden rajat, koordinaattien selvitys, pinta-alojen laskenta</li>
<li>Karttatyökalu - Esiintymisalueen koko/AOO; Levinneisyysalueen koko/EOO (Tulossa 2026-7)</p>
</ul>

<@toolbox/>		

<#--
<div style="border: 1px dotted green; background-color: rgb(200,255,200); font-size: 13px; margin: 1em; padding: 0.3em;">
	<h6>Uudistuksia xx.xx.2026</h6>
	<ul>
		<li>Ladattaessa arviointeja tiedostoon "Aloittamattomat" tulevat nyt taas mukaan.</li>
		<li>Parannettu tietojen sopivuutta Excelin kanssa ("-100" (min: tyhjä, max: 100)  ->  "-- 100" jne).</li>
		<li>Parannettu ohjetta Excel vientiin (solujen tyypiksi Text).</li>
	</ul>
</div>
-->

<table class="resourceListTable informalGroupsTable">
	<thead>
		<tr>
			<th>Eliöryhmä</th>
			<th>Tila</th>
			<th>Editorit</th>
			<#if user.isAdmin??><th></th></#if>
		</tr>
	</thead>
	<tbody>
		<#list taxonGroupRoots as rootQname>
			<#if (rootQname_index) % 2 == 0>
				<@printGroup taxonGroups[rootQname] 0 "odd" />
			<#else>
				<@printGroup taxonGroups[rootQname] 0 "even" />
			</#if>
		</#list>
	</tbody>
</table>


<#macro printGroup taxonGroup indent evenOdd>
	<tr class="indent_${indent} ${evenOdd}">
		<#if taxonGroupEditors[taxonGroup.qname.toString()]??>
			<td> 
				<span class="indent">&mdash;</span>
				<a href="${baseURL}/iucn/group/${taxonGroup.qname}/${selectedYear}">
					${taxonGroup.name.forLocale("fi")!""}
				</a>
			</td>
			<td class="taxonGroupStat" id="${taxonGroup.qname}">
				<@loadingSpinner "" />
			</td>
			<td class="groupEditors">
				<@editors taxonGroup.qname.toString() />
			</td>
		<#else>
			<td> 
				<span class="indent">&mdash;</span>
				${taxonGroup.name.forLocale("fi")!""}
			</td>
			<td> &nbsp; </td>
			<td class="groupEditors"> &nbsp; </td>
		</#if>
		<#if user.admin><td><a class="button" href="${baseURL}/iucn/editors/${taxonGroup.qname}">Modify editors (admin only)</a></td></#if>
	</tr>
	<#if !taxonGroupEditors[taxonGroup.qname.toString()]??>
		<#list taxonGroups?values as subGroupCandidate>
			<#if taxonGroup.hasSubGroup(subGroupCandidate.qname)>
				<@printGroup subGroupCandidate indent + 1 evenOdd />
			</#if>
		</#list>
	</#if>
</#macro>

<p class="info">Voit siirtyä arvioitavien lajien luetteloon klikkaamalla eliöryhmän nimeä.</p>

<#if user.iucnAdmin>
<hr />

<p><a href="${baseURL}/iucn-groups">Punaisen kirjan ryhmien tarkastelu ja muokkaus</a></p>

<hr />
<p>
	Lataa kaikki arvioinnit yhteen tiedostoon <a id="downloadAll" href="${baseURL}/iucn/download-all/" class="button">Lataa</a>
</p>
<p>Tuotetut tiedostot ilmestyvät alle. Palaa latauksen aloitettuasi n. 10 minuutin päästä tälle IUCN-osion etusivulle nähdäksesi tuotetut tiedostot.</p>

<#if downloads?has_content>
<ul>
<#list downloads as filename>
	<li><a href="${baseURL}/iucn/file/${filename}">${filename}</a></li>
</#list>
</ul>
</#if>

</#if>

<script>
$(function() {
	$(".taxonGroupStat").each(function() {
		var groupQname = $(this).attr("id");
		var groupStatElement = $(this);
		$.get("${baseURL}/api/iucn-stat/"+groupQname+"?year=${selectedYear}", function(data) {
			groupStatElement.html(data);
		});
	});
	$("#downloadAll").on('click', function() {
		return confirm('Haluatko varmasti ladata kaikkien arviointien tiedot?'); 
	});
});
</script>

<#include "luomus-footer.ftl">