<!--
	<h5 class="checklistHeader"><#if checklist??>${checklist.getFullname("en")}<#else>Orphan taxa without checklist</#if></h5>
-->
<div id="rootTree">
	<#assign roots = []>
	
	<#macro travelRootTree taxon>
		<#assign roots = roots + [ taxon ] />
		<#if taxon.hasParent()>
			<#assign parentRank = (taxon.parent.taxonRank.toString())!"norank">
			<#if parentRank != "MX.division" && parentRank != "MX.class">
				<@travelRootTree taxon.parent /> 
			</#if>
		</#if>
	</#macro>
	
	<#macro printTree taxon>
		<div class="rootTreeNode">
			<a href="${taxon.qname}" onclick="goToTaxon('${taxon.qname}'); return false;">
				<span class="scientificName <#if taxon.isSpecies()>speciesName</#if>">${taxon.scientificName!taxon.vernacularName.forLocale("en")!taxon.qname}</span>
			</a>
		</div>
	</#macro>
	
	<#if root.hasParent()><@travelRootTree root.parent /></#if>
	
	<#list roots?reverse as rootTreeTaxon>
		<@printTree rootTreeTaxon/>
	</#list>
	<div class="clear"></div>
</div>

<#if errorMessage?has_content>
	<div class="taxonTreeErrorMessage errorMessage">${errorMessage}</div>
</#if>

<div class="taxonLevel">
		
	<div class="taxonChilds rootTaxonChilds" <#if root.hasParent()>id="${root.parent.qname?replace(".","")}Children"<#else>id="rootTaxonContainer"</#if>>
		<ul class="childTaxonList">
			<li><@printTaxon root "rootTaxon" /></li>
		</ul>			
	</div>
</div>

