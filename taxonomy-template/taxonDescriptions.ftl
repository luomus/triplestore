<#include "luomus-header.ftl">

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

<h6><#if checklist??>${checklist.getFullname("en")}<#else>Orphan taxa without checklist</#if></h6>

<h5 id="taxonEditHeader">
	<@printScientificNameAndAuthor taxon />
	<#if taxon.getVernacularName("fi")?has_content>
		&mdash; ${taxon.getVernacularName("fi")}
	</#if>
	&nbsp;
	${taxon.qname}
</h5>

<#assign locales = ["fi","sv","en"] />

<#list groups as group>
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
</#list>


<div class="clear"></div>






</div>


<script>
$(function() {
	$(".languageTabs").tabs();
	$("textarea").jqte({
		br: true,
		center: false,
		color: false,
		fsize: false,
		format: false,
		i: true,
		indent: false,
		link: true,
		linktypes: ['Link to web page','not in use','not in use'],
		left: false,
		ol: false,
		outdent: false,
		p: true,
		placeholder: false,
		remove: false,
		right: false,
		rule: false,
		source: true,
		sub: false,
		strike: false,
		sup: false,
		title: true,
		u: false,
		ul: false,
		unlink: true
	});
});
</script>


<#include "luomus-footer.ftl">



