<#if response?? && response.rootNode.hasChildNodes()>
	<#list response.rootNode.childNodes as matches>
		<h3>${text[matches.name]}</h3>
		<#list matches.childNodes as match>
			<p>
				<a href="${taxonpageBaseLinkURL}/${match.name}">${match.contents}</a>
				<#if match.hasAttribute("scientificName")>
					&nbsp; - &nbsp; ${match.getAttribute("scientificName")}<#if match.hasAttribute("scientificNameAuthorship")>,</#if>
				</#if>
				<#if match.hasAttribute("scientificNameAuthorship")>
					${match.getAttribute("scientificNameAuthorship")}
				</#if> 
				[<#if match.hasAttribute("taxonRank")>${match.getAttribute("taxonRank")?replace("MX.", "")}<#else>unranked</#if>]
			</p>
		</#list>
	</#list>
<#else>
	<p>No results!</p>
</#if>