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

<div id="editTaxonDescriptions">


<p><a href="${baseURL}/${taxon.qname}">&laquo; To taxonomy tree</a> &mdash; <#if checklist??>${checklist.getFullname("en")}<#else>Orphan taxa without checklist</#if></p>

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
	<#assign headerLabel = group.label.forLocale("fi") + " &mdash; " + group.label.forLocale("sv") + " &mdash; " + group.label.forLocale("en") />
	<@portletHeader headerLabel initiallyClosed />
		<table>
			<#list variables[group.qname.toString()] as descriptionVariable>
				<#assign qname = descriptionVariable.qname.toString() />
				<#assign property = properties.getProperty(qname)>
				<tr>
					<#list locales as locale>
						<#assign existingValue = "" />
						<#if taxon.descriptions.defaultContext??>
							<#assign existingValue = taxon.descriptions.defaultContext.getText(qname, locale)!"" />
						</#if>
						<td>
							<h2>${property.label.forLocale(locale)!field}</h2>
							<div class="content">
								<#if existingValue?has_content>
									${existingValue}
								<#else><p class="info emptyContent">Click to add content</p></#if>
							</div>
							<div class="textareaContainer hidden">
								<textarea class="hidden" name="${qname + "___" + locale}" id="${qname + "forlocale" + locale}" <@checkPermissions permissions />>${existingValue?html}</textarea>
								<button class="closeEditorButton doSave">Save & close</button> <button class="closeEditorButton noSave">Close without saving</button>
							</div>
						</td>
					</#list>
				</tr>
			</#list>
		</table>
	<@portletFooter />
</#macro>

<div class="clear"></div>




</div>


<script>

function initTinyMCE() {
	tinymce.init({
		
  	});
 }

$(function() {
	
	var originalContents = {};
	
	$('.content').on('click', function() {
		$('body').addClass('noscroll');
		var container = $(this).parent().find('.textareaContainer'); 
		container.show();
		
		var textarea = container.find('textarea').first();
		var id = textarea.attr('id');
		if (!originalContents[id]) {
			originalContents[id] = textarea.val();
		}
		textarea.tinymce({
    		skin_url: '${staticURL}/tinymce-4.7.1/skins/lightgray',
			plugins: 'link code',
    		toolbar: 'italic bold | link unlink | removeformat | undo, redo | code',
    		menubar: false,
    		statusbar: false,
    		height: "360"
  		});
	});
	
	$('.closeEditorButton').on('click', function() {
		var doSave = $(this).hasClass('doSave');
		var container = $(this).closest('.textareaContainer');
		var textarea = container.find('textarea').first();
		var id = textarea.attr('id');
		
		if (doSave) {
			originalContents[id] = textarea.val();
			container.parent().find('.content').html(textarea.val());
		} else {
			textarea.val(originalContents[id]);
		}
		
		container.hide();
		$('body').removeClass('noscroll');
		$(".saveButton").remove();
		
		return doSave;  
	});
	
	$("#imagesButton").on('click', function() {
		var container = $('<div id="iframeContainer"><iframe src="${kotkaURL}/tools/taxon-images?taxonID=${taxon.qname}&amp;personToken=${user.personToken}"></iframe></div>');
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

</script>


<#include "luomus-footer.ftl">



