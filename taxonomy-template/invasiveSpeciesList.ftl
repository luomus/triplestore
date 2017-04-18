<#include "luomus-header.ftl">

<h1>Vieraslajit</h1>

<p>Tällä sivulla voidaan tarkastella ovatko kaikki historiallisesti vieraslajeiksi määritellyt 
lajit yhä "tallella" taksonomiassa ja lajien tietojen täydellisyyttä.</p>

<table id="invasiveSpecies">
	<thread>
		<tr>
			<th>Id</th>
			<th>Tieteellinen nimi</th>
			<th>Kansankielinen</th>
			<th>Taso</th>
			<th>Laji.fi vieraslajimääritelmän mukainen</th>
			<th>Vieraslajit.fi näkyvyys</th>
			<th>Hallinnollinen asema</th>
			<th>Portaalin ryhmät</th>
			<th>Vakiintuneisuus</th>
			<th>Checklist ok?</th>
			<th>Puussa ok?</th>
			<th>Lajikortti?</th>
			<th>Lajikortin tuottajat</th>
		</tr>
	</thead>
	<tbody>
		<#list species as taxon>
			<tr>
				<td>${taxon.qname}</td>
				<td><@printScientificNameAndAuthor taxon/> <#if (taxon.scientificName!"") == "MISSING!"><span class="error">MISSING TAXON!</span></#if></td>
				<td>${taxon.vernacularName.forLocale("fi")!""}</td>
				<td>${(taxon.taxonRank?replace("MX.",""))!""}</td>
				<td><#if taxon.invasiveSpecies>Kyllä<#else><span class="error">Ei</span></#if></td>
				<td><#if taxon.invasiveSpeciesCategory?? && taxon.invasiveSpeciesMainGroups?has_content>Kyllä<#else><span class="error">Ei</span></#if></td>
				<td>
					<#list taxon.administrativeStatuses as status>
						${(adminStatuses.getValueFor(status.toString()).label.forLocale("fi"))!status}
					</#list>
				</td>
				<td>
					<#list taxon.invasiveSpeciesMainGroups as group>
						<#if group == "HBE.MG10"><span style="color: grey;"></#if>
							${(groups.getValueFor(group.toString()).label.forLocale("fi"))!group}
						<#if group == "HBE.MG10"></span></#if>
						<#if group_has_next><br /></#if>
					</#list>
				</td>
				<td>
					<#if taxon.invasiveSpeciesEstablishment??>
						${(establishments.getValueFor(taxon.invasiveSpeciesEstablishment.toString()).label.forLocale("fi"))!taxon.invasiveSpeciesEstablishment}
					</#if>
				</td>
				<td><#if taxon.checklist??>Kyllä<#else><span class="error">Ei</span></#if></td>
				<td><#if taxon.hasParent()??>Kyllä<#else><span class="error">Ei</span></#if></td>
				<td><#if taxon.descriptions.defaultContext??>Kyllä<#else><span class="error">Ei</span></#if></td>
				<td>${(taxon.descriptions.defaultContext.getText("MX.speciesCardAuthors","fi"))!""}</td>
			</tr>
		</#list>
	</tbody>
</table>

<#include "luomus-footer.ftl">