<#include "luomus-header.ftl">
<#include "iucn-macro.ftl">

<h1>Uhanalaisuusarviointi - ${selectedYear} <#if draftYear == selectedYear>(LUONNOS)</#if></h1>
<p><a href="${baseURL}/iucn-groups">Punaisen kirjan ryhmien tarkastelu ja muokkaus</a></p>
<p><a href="${staticURL}/IUCN-käyttöohje.pdf" target="_blank">Käyttöohje</a> (13.2.2017 jälkeiset muutokset vielä päivittämättä)</p>
<p><a href="${staticURL}/Elinympäristöluokkien_tulkintaohje_v1_1.pdf" target="_blank">Elinympöristöluokkien tulkintaohje </a></p>
<p><a href="https://laji.fi/map" target="_blank">Karttatyökaluun</a> (arviointialueiden rajat, koordinaattien selvitys, pinta-alan selvitys)</p>

<@toolbox/>		

<div style="border: 1px dotted green; background-color: rgb(200,255,200); font-size: 13px; margin: 1em; padding: 0.3em;">
	<h6>Uudistuksia 16.4.2018</h6>
	<ul>
		<li>Ladattaessa arviointeja tiedostoon "Aloittamattomat" tulevat nyt taas mukaan.</li>
		<li>Parannettu tietojen sopivuutta Excelin kanssa ("-100" (min: tyhjä, max: 100)  ->  "-- 100" jne).</li>
		<li>Parannettu ohjetta Excel vientiin (solujen tyypiksi Text).</li>
	</ul>
	<h6>Uudistuksia 27.-29.3.2018</h6>
	<ul>
		<li>Lomake varoittaa poistuttaessa jos on tallentamattomia muutoksia: Korjattu tämän toiminta kaikilla selaimilla myös RLI päivityksen ja kommenttejen jättämisen osalta. (27.3. "korjaus" rikkoi nämä)</li>
	</ul>	
	<h6>Uudistuksia 22.3.2018</h6>
	<ul>
		<li>Lomake varoittaa poistuttaessa jos on tallentamattomia muutoksia.</li>
		<li>Kriteereille A3, B1, B2 ja C2 ei vaadita enää uhanalaisuuden syytä pakollisena tietona.</li>
		<li>Muutoksen syy ei ole pakollinen jos NE-laji muuttuu muuhun luokkaan</li>
		<li>Taksonomisen tietokannasta tieto siellä määritellystä lajin asemasta/vakiintumisesta Suomessa näkyville tallennuslomakkeella.</li>
		<li>Korjattu lajiluettelon rajausta: Jos lajille on kopioitu vuoden 2010 tiedot mutta ei tehty muita tallennuksia, arvioinnille ei asettunut tilaksi "Aloitettu" joten lajiluettelossa rajaus aloitetuilla ei näyttänyt näitä arviointeja.</li>
		<li>Lisätty Excel-latauksen ohjeisiin kuinka tuoda kaikki sisältö tekstinä, ettei Excel muuta lukuja päivämääriksi yms.</li>
		<li>Excel-latauksen sarakejärjestyksiä on muutettu toivotusti. Loppuun on lisätty RLI-laskentaa varten lisää sarakkeita.</li>
	</ul>
</div>

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