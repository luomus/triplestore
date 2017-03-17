<#include "macro.ftl">

<ul class="synonyms taxonChilds">
	<#list taxa as taxon>
		<li><@printTaxon taxon "synonym" false false />	</li>		
	</#list>
</ul>

