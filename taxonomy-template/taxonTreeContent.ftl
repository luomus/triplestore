<!--
	<h5 class="checklistHeader"><#if checklist??>${checklist.getFullname("en")}<#else>Orphan taxa without checklist</#if></h5>
-->
<div id="rootTree">
	<#macro rootTree taxon>
		<#if taxon.hasParent()>
			<div class="rootTreeNode">
				<a href="#" onclick="goToTaxon('${taxon.parent.qname}'); return false;">
					<span class="scientificName <#if taxon.isSpecies()>speciesName</#if>">${taxon.scientificName!taxon.vernacularName.forLocale("en")!taxon.qname}</span>
				</a>
			</div>
			<#if !taxon.parent.taxonRank?? || taxon.parent.taxonRank.toString() != "MX.class">
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

