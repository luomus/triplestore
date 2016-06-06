<#if stat.speciesOfGroupCount != 0>
	<#assign percent = (stat.readyCount/stat.speciesOfGroupCount*100)?floor>
<#else>
	<#assign percent = 0>
</#if>
<span class="stat ready">${stat.readyCount}</span> 
/
<span class="stat <#if (percent>=100)>ready<#else>total</#if>">${stat.speciesOfGroupCount}</span>
<span class="stat <#if (percent>=100)>ready<#else>total</#if>">${percent}%</span>

