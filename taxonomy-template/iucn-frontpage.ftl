<#include "luomus-header.ftl">
<#include "iucn-macro.ftl">

<h1>Uhanalaisuusarviointi - ${selectedYear} <#if draftYear == selectedYear>(LUONNOS)</#if></h1>

<p><a href="${staticURL}/IUCN-käyttöohje.pdf" target="_blank">Lataa käyttöohje</a> (13.2.2017 jälkeiset muutokset vielä päivittämättä)</p>
<p><a href="${staticURL}/Elinympäristöluokkien_tulkintaohje_v1_1.pdf" target="_blank">Lataa elinympöristöluokkien tulkintaohje </a></p>
<p><a href="https://laji.fi/map" target="_blank">Karttatyökaluun</a> (arviointialueiden rajat, koordinaattien selvitys, pinta-alan selvitys)</p>

<@toolbox/>		

<div style="border: 1px dotted red;  font-size: 15px; margin: 1em; padding: 0.3em;">
IUCN EDITOR ON SULJETTU! Palvelusta löytyi vakava ongelma ja korjauksen ajaksi editor on poissa käytöstä. 
Ongelma liittyy kommenttien tekoon ja elinympäristötiedon katoamiseen. Dataa ei tulla menettämään vaan kaikki palautetaan historiasta.
</div>

<div style="border: 1px dotted green; background-color: rgb(200,255,200); font-size: 13px; margin: 1em; padding: 0.3em;">
	<h6>Uudistuksia 30.10. ja 1.11.2017</h6>
	<ul>
		<li><b>Korjattu bugi jossa arviointien kommentit katosivat</b> arvioinnin tallennuksen yhteydessä. Tästä syystä kadonneet 17 kommenttia on palautettu historiasta.</li>
		<li>Nyt myös arvioijat voivat jättää kommentteja arvioinnin yläosasta löytyvän kommentointitoiminnon avulla.</li>
		<li>Lisätty uusi tila arvioinnille: "Valmis kommentoitavaksi". Tallennusosiossa on nyt kolme nappia: "Tallenna", "Valmis kommentoitavaksi" ja "Valmis". 
		Kaikki napit edelleenkin tietenkin tekevät myös tallennuksen. "Tallenna" -nappi siirtää arivoinnin tilaan "Aloitettu".</li>
		<li>B- ja D-kriteereiden ja populaation pirstoutumisen ohjeistusta on tarkennettu.</li>
		<li>CSV-download kenttien järjestystä on muutettu toivomusten mukaiseksi</li>
		<li>Tunnettu ongelma: Toistaiseksi tuntematon kriteeriyhdistelmä (siis esim D2bvc(i-xx) tai vastaavaa) aiheuttaa automaattisten tarkistusten kaatumisen.
			Järjestelmän lähettämää virheviestiä on parannettu, joten seuraavan kerran kun ongelma tapahtuu pääsemme paremmin syyn jäljille. 
		</li>
	</ul>
</div>
<div style="border: 1px dotted green; font-size: 13px; margin: 1em; padding: 0.3em;">
	<h6>Uudistuksia 6.-13.4.2017</h6>
	<ul>
		<li>Elinympäristöjen tulkintaohje on saatavilla arviointilomakkeelta elinympäristöjen ilmoittamisen ohesta ja tämän sivun yläosasta.</li>
		<li>Lajiluettelosta on mahdollista ladata CSV-muodossa arviointien tiedot.</li>
		<li>Korjattu vika joka yö tapahtuvassa järjestelmän uudelleenlatauksessa, josta johtuen arvioinnit eivät näkyneet lajiluettelossa.</li>
		<li>NA-pikanappi lisätty lajiluetteloon.</li>
		<li>LC-pikanappi lisätty lajiluetteloon. (Käytetään vain jos ei haluta ilmoittaaa muuta tietoa kuin luokka ja ensisijainen elinympäristö.)</li>
		<li>Lajiluettelon rajaus edellisen arvioinnin luokan mukaan. (Vinkki: Kätevä tapa saada esille vain NE,NA tai LC lajit "alta pois" läpi käytäväksi.)</li>
	</ul>
	
	<h6>Uudistuksia 20.3.2017</h6>
	<p>Linkki karttatyökaluun lisätty.</p>
	
	<h6>Uudistuksia 13.2.2017</h6>
	<ul>
	<li>Alueellisen uhanalaisuuden ilmoittaminen on muuttunut. Alueelliselle uhanalaisuudelle on nyt erilliset valinnat. Alueellisen uhanalaisuuden kentät saa näkyville painamalla "Määritä alueellinen uhanalaisuus" -painiketta.</li>
	<li>Lähteet -osio on jaettu kahteen erilaiseen tyyppiin; julkaisuihin ja muihin lähteisiin.</li>
	<li>Järjestelmä muistaa lajiluettelon halutun koon (jota säädetään luettelon alalaidasta). Lisätty seuraava ja edellinen -linkit.</li>
	<li>Arviointialueiden (1a-4d) kartta on saatavilla työkalusta kenttien ohessa.</li>
	<li>Uusi kenttä: Osuus globaalista populaatiosta.</li>
	</ul>
</div>
<#--
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
-->

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