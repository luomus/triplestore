<!DOCTYPE html>

<#include "macro.ftl">

<!--[if lt IE 9 ]> <body class="oldie"> <![endif]-->
<!--[if (gt IE 9)|!(IE)]><!--> <html lang="${locale}"> <!--<![endif]-->

<head>
	<meta charset="utf-8" />

	<!-- Set the viewport width to device width for supporting devices (iOS, Android etc) -->
	<meta name="viewport" content="initial-scale=1.0" />

	<title>Taxon Editor | Edit taxon descriptions | LUOMUS</title>
	<link href="${staticURL}/favicon.ico?${staticContentTimestamp}" type="image/ico" rel="shortcut icon" />

	<script src="${staticURL}/jquery-1.9.1.js"></script>
	
	<link href="${staticURL}/jquery-ui/css/cupertino/jquery-ui-1.10.3.custom.min.css" rel="stylesheet" /> 
	<script src="${staticURL}/jquery-ui/js/jquery-ui-1.10.2.min.js"></script>
	
	<script src="${staticURL}/jquery.validate.min.js?${staticContentTimestamp}"></script>

	<script src="${staticURL}/jquery.cookie.js?${staticContentTimestamp}"></script>
	
	<script src="${staticURL}/jsPlumb/jquery.jsPlumb-1.5.4-min.js?${staticContentTimestamp}"></script>
	
	<script src="${staticURL}/freetile/jquery.freetile.min.js?${staticContentTimestamp}"></script>
	
	<script src="${staticURL}/jquery-placeholder/jquery.placeholder.js?${staticContentTimestamp}"></script>
	
	<script src="${staticURL}/chosen/chosen.jquery.min.js?${staticContentTimestamp}"></script>
	<link href="${staticURL}/chosen/chosen.min.css?${staticContentTimestamp}" rel="stylesheet" />
	
	<script src="${staticURL}/collapse/collapse.js?${staticContentTimestamp}"></script>
	<link href="${staticURL}/collapse/collapse.css?${staticContentTimestamp}" rel="stylesheet" />
	
	
	<script src="${staticURL}/jquery-switchButton/jquery.switchButton.js?${staticContentTimestamp}"></script>
	<link href="${staticURL}/jquery-switchButton/jquery.switchButton.css?${staticContentTimestamp}" rel="stylesheet" />
	
	<link href="${staticURL}/luomus.css?${staticContentTimestamp}" rel="stylesheet" />
	<link href="${staticURL}/triplestore.css?${staticContentTimestamp}" rel="stylesheet" />
	<link href="${staticURL}/taxonomy.css?${staticContentTimestamp}" rel="stylesheet" />
	<link href="${staticURL}/taxonTrees.css?${staticContentTimestamp}" rel="stylesheet" />
	
	<#include "javascript.ftl">
	<#include "javascript-taxonTree.ftl">
	<#include "javascript-taxonEdit.ftl">
	
</head>

<body>


<div id="toolbox" class="ui-widget ui-corner-all">
	<div id="toolBoxContent" class="ui-widget-content">
		<div id="taxonSearch">
			Change taxon:
			<form onsubmit="searchTaxon(this); return false;" class="taxonomySearchForm" taxonpageBaseLinkType="taxonDescriptions">
				<input type="text" placeholder="Type name of a taxon"/> <input type="submit" value="Search" />
				<div class="taxonomySearchResultContainer" style="display: none;">&nbsp;</div>
			</form>
		</div>
	</div>
</div>
		
		
<div id="editTaxonDescriptions" class="ui-widget-content">


<h5 id="taxonEditHeader">
	<@printScientificNameAndAuthor taxon />
	<#if taxon.getVernacularName("fi")?has_content>
		&mdash; ${taxon.getVernacularName("fi")}
	</#if>
	&nbsp;
	${taxon.qname}
</h5>

<#assign locales = ["fi","sv","en"] />

<#--		TODO			<@label "MX.speciesCardAuthors" />
					<@input "MX.speciesCardAuthors___fi" "on" taxon.basicDescriptionTexts.getDefaultContextText("MX.speciesCardAuthors", "fi") /> -->


<#list groups as group>
<div class="column">
	<@portletHeader group.label.forLocale("en") "initiallyClosed" />
		<div class="languageTabs">
			<ul>
				<#list locales as locale>
					<li><a href="#group-${group_index}-${locale}">${locale?upper_case}</a></li>
				</#list>
			</ul>
			<#list locales as locale>
				<div id="group-${group_index}-${locale}">
					<#list variables[group.qname.toString()] as descriptionVariable>
						<#assign qname = descriptionVariable.qname.toString() /> 
						<@label qname "longtext" />
						<@longText qname + "___" + locale taxon.basicDescriptionTexts.getDefaultContextText(qname, locale) />
					</#list>
				</div>
			</#list>
		</div>
	<@portletFooter />	
</div>
</#list>


<div class="clear"></div>






</div>


<script>
$(function() {
	$(".languageTabs").tabs();
});
</script>


</body>
</html>


