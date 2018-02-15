<#include "luomus-header.ftl">
<#include "iucn-macro.ftl">

<h1>Uhanalaisuusarviointi - ${selectedYear} <#if draftYear == selectedYear>(LUONNOS)</#if></h1>

<p><a href="${staticURL}/IUCN-käyttöohje.pdf" target="_blank">Käyttöohje</a> (13.2.2017 jälkeiset muutokset vielä päivittämättä)</p>
<p><a href="${staticURL}/Elinympäristöluokkien_tulkintaohje_v1_1.pdf" target="_blank">Elinympöristöluokkien tulkintaohje </a></p>
<p><a href="https://laji.fi/map" target="_blank">Karttatyökaluun</a> (arviointialueiden rajat, koordinaattien selvitys, pinta-alan selvitys)</p>

<@toolbox/>		

<div style="border: 1px dotted green; background-color: rgb(200,255,200); font-size: 13px; margin: 1em; padding: 0.3em;">
	<h6>Uudistuksia 15.2.2018</h6>
	<ul>
		<li>Lomake varoittaa poistuttaessa jos on tallentamattomia muutoksia.</li>
	</ul>
	<h6>Uudistuksia 24.1.2018</h6>
	<ul>
		<li>Muutettu luokkien esittämistä lajiluettelossa, lajin arviointisivulla ja tiedostolatauksessa: Otetaan huomioon luokan ylentäminen ja alentaminen.</li>
		<li>Näytetään kaikentyyppiset synonyymit lajin tietojen ohessa luettelossa, arviointisivulla ja tiedostolatauksessa (aiemmin näytettiin vain aidot synonyymit).</li>
		<li>Korjattu virhe jonka vuoksi uhanalaisuuden syy/uhkatekijät järjestys meni sekaisin. Tämä tapahtui (ainakin) jos muokattiin arviointia jossa oli jo ennestään useampi kuin yksi syy ja lisättiin uusia syitä. Lajeja joita tämä koskee on 91. Näistä on lähetetty tieto asianomaisille arvioijille.</li>
		<li>Korjattu virhe DD-luokan arviointien muokkauksessa: "DD-luokituksen syy" -kenttä ei tullut näkyville.</li>
	</ul>
</div>

<table class="resourceListTable informalGroupsTable">
	<thead>
		<tr>
			<th>Eliöryhmä</th>
			<th>Tila</th>
			<th>Uhanalaisuusarvioijat</th>
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

<script>
$(function() {
	$(".taxonGroupStat").each(function() {
		var groupQname = $(this).attr("id");
		var groupStatElement = $(this);
		$.get("${baseURL}/api/iucn-stat/"+groupQname+"?year=${selectedYear}", function(data) {
			groupStatElement.html(data);
		});
	});
});
</script>

<#include "luomus-footer.ftl">