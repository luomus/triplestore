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
		<button id="imagesButton">Images</button>
	</div>
</div>

<div id="editTaxonDescriptions" class="ui-widget-content">

<h6><#if checklist??>${checklist.getFullname("en")}<#else>Orphan taxa without checklist</#if></h6>

<h5 id="taxonEditHeader">
	<@printScientificNameAndAuthor taxon />
	<#if taxon.vernacularName.forLocale("fi")?has_content>
		&mdash; ${taxon.vernacularName.forLocale("fi")}
	</#if>
	&nbsp;
	${taxon.qname}
</h5>

<#assign locales = ["fi","sv","en"] />

<#list groups as group>
	<#if groupsWithContent?seq_contains(group.qname.toString())>
		<@printGroup group "" group_index />
	</#if>
</#list>
<#list groups as group>
	<#if !groupsWithContent?seq_contains(group.qname.toString())>
		<@printGroup group "initiallyClosed" group_index />
	</#if>
</#list>

<#macro printGroup group initiallyClosed index>
	<#assign headerLabel = group.label.forLocale("fi") + " &mdash; " + group.label.forLocale("en") />
	<@portletHeader headerLabel initiallyClosed />
		<div class="languageTabs">
			<ul>
				<#list locales as locale>
					<li><a href="#group-${index}-${locale}">${locale?upper_case}</a></li>
				</#list>
			</ul>
			<#list locales as locale>
				<div id="group-${index}-${locale}">
					<#list variables[group.qname.toString()] as descriptionVariable>
						<#assign qname = descriptionVariable.qname.toString() />
						<#assign existingValue = "" />
						<#if taxon.descriptions.defaultContext??>
							<#assign existingValue = taxon.descriptions.defaultContext.getText(qname, locale)!"" />
						</#if>
						<@label qname "longtext" locale />
						<@longText qname + "___" + locale existingValue />
					</#list>
				</div>
			</#list>
		</div>
	<@portletFooter />	
</#macro>

<div class="clear"></div>




</div>


<script>
$(function() {
	
	$(".languageTabs").tabs();
	
	tinymce.init({
		plugins: 'link code',
    	selector: 'textarea',
    	menubar: false,
    	statusbar: false,
    	height: "210",
    	toolbar: 'italic bold | link unlink | removeformat | undo, redo | code',
    	setup: function(editor) {
    		checkIfDisabled(editor);
    		editor.on('change', function(e) {
      			editor.save();
      			updateOriginal(editor.getElement());
    		});
  		}
  	});
	
	$("#imagesButton").on('click', function() {
		var container = $('<div id="iframeContainer"><iframe src="${kotkaURL}/tools/taxon-images?taxonID=${taxon.qname}"></iframe></div>');
		$("body").append(container);
		var windowHeight = $(window).height();
        var dialogHeight = windowHeight * 0.9;
		container.dialog({
			title: 'Add/modify taxon images',
			autoOpen: true,
      		height: dialogHeight,
      		width: "95%",
      		modal: true,
      		buttons: {
        		"Close": function() {
          			container.dialog("close");
        		}
			},
      		close: function() {
				container.remove();
      		}
    	});
	});
});

function updateOriginal(e) {
	$(e).trigger('change');
}

function checkIfDisabled(editor) {
	if ($(editor.getElement()).prop('disabled') === true) {
		editor.settings.readonly = true;
	}
}

</script>


<#include "luomus-footer.ftl">



