<#include "luomus-header.ftl">
<#include "iucn-macro.ftl">

<h1>Uhanalaisuusarviointi - ${selectedYear} <#if draftYear == selectedYear>(LUONNOS)</#if></h1>

<p><a href="${staticURL}/IUCN-käyttöohje.pdf" target="_blank">Lataa käyttöohje</a> (13.2. muutokset vielä päivittämättä)</p>
<p><a href="https://beta.laji.fi/map" target="_blank">Karttatyökaluun</a> (arviointialueiden rajat, koordinaattien selvitys)</p>

<@toolbox/>		

<div style="border: 1px dotted green; background-color: rgb(200,255,200); font-size: 13px; margin: 1em; padding: 0.3em;">
	<h6>Uudistuksia 6.4.2016</h6>
	<ul>
		<li>NA-pikanappi lisätty lajiluetteloon</li>
		
	</ul>
	
	<h6>Uudistuksia 20.3.2016</h6>
	<p>Linkki karttatyökaluun lisätty. Mahdollisuus nähdä valitun alueen pinta-ala on tulossa tänne ja havaintohaun viranomaispuolelle.</p>
	
	<h6>Uudistuksia 13.2.2017</h6>
	<ul>
	<li>Alueellisen uhanalaisuuden ilmoittaminen on muuttunut. Alueelliselle uhanalaisuudelle on nyt erilliset valinnat. Alueellisen uhanalaisuuden kentät saa näkyville painamalla "Määritä alueellinen uhanalaisuus" -painiketta.</li>
	<li>Lähteet -osio on jaettu kahteen erilaiseen tyyppiin; julkaisuihin ja muihin lähteisiin.</li>
	<li>Järjestelmä muistaa lajiluettelon halutun koon (jota säädetään luettelon alalaidasta). Lisätty seuraava ja edellinen -linkit.</li>
	<li>Arviointialueiden (1a-4d) kartta on saatavilla työkalusta kenttien ohessa.</li>
	<li>Uusi kenttä: Osuus globaalista populaatiosta.</li>
	<li><b>Paihoittelut uudistusten käyttöönoton aiheuttamasta katkosta palvelun toiminnassa. Katkot pyritään ajoittamaan virka-ajan (7-17) ulkopuolelle.</b></li>
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