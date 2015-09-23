<#include "macro.ftl">

<@taxonChildsTools parent />
<ul class="childTaxonList">
	<#list children as taxon>
		<li><@printTaxon taxon /></li>		
	</#list>
</ul>
<@taxonChildsToolsBottom parent />
