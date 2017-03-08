<!--
	<h5 class="checklistHeader"><#if checklist??>${checklist.getFullname("en")}<#else>Orphan taxa without checklist</#if></h5>
-->
<div id="rootTree">
	<#macro rootTree taxon>
		<div class="rootTreeNode">
			<a href="${taxon.qname}" onclick="goToTaxon('${taxon.qname}'); return false;">
				<span class="scientificName <#if taxon.isSpecies()>speciesName</#if>">${taxon.scientificName!taxon.vernacularName.forLocale("en")!taxon.qname}</span>
			</a>
		</div>
		<#if taxon.hasParent()>
			<#assign parentRank = (taxon.parent.taxonRank.toString())!"norank">
			<#if parentRank != "MX.division" && parentRank != "MX.class">
				<@rootTree taxon.parent /> 
			</#if>
		</#if>
	</#macro>
	<#if root.hasParent()><@rootTree root.parent /></#if>
</div>
	
<div class="taxonLevel">
		
	<div class="taxonChilds rootTaxonChilds" <#if root.hasParent()>id="${root.parent.qname?replace(".","")}Children"<#else>id="rootTaxonContainer"</#if>>
		<ul class="childTaxonList">
			<li><@printTaxon root "rootTaxon" /></li>
		</ul>			
	</div>
</div>

