
	<h5 class="checklistHeader"><#if checklist??>${checklist.getFullname("en")}<#else>Orphan taxa without checklist</#if></h5>

	<div class="taxonLevel">
	
		<#if root.hasParent()>
			<div><a href="#" onclick="goUp(); return false;">Go up</a></div>
		</#if>
		<div class="taxonChilds rootTaxonChilds" <#if root.hasParent()>id="${root.parent.qname?replace(".","")}Children"<#else>id="rootTaxonContainer"</#if>>
			<ul class="childTaxonList">
				<li><@printTaxon root "rootTaxon" /></li>
			</ul>			
		</div>
	</div>

