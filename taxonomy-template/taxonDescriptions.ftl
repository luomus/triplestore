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
		<@taxonImageButton taxon/>
		<div>
			Change languages:
			<select id="descriptionLocalesSelector" multiple="multiple" class="chosen">
				<#list ["fi", "sv", "en", "ru"] as l>
					<option value="${l}" <#if locales?seq_contains(l)>selected="selected"</#if>>${l?upper_case}</option>
				</#list>
			</select>
		</div>
	</div>
</div>

<div id="editTaxonDescriptions">

<#if user.role != "DESCRIPTION_WRITER">
<p><a href="${baseURL}/${taxon.qname}">&laquo; To taxonomy tree</a> &mdash; <#if checklist??>${checklist.getFullname("en")}<#else>Orphan taxa without checklist</#if></p>
</#if>

<h5 id="taxonEditHeader">
	<@printScientificNameAndAuthor taxon />
	<#if taxon.vernacularName.forLocale("fi")?has_content>
		&mdash; ${taxon.vernacularName.forLocale("fi")}
	</#if>
	&nbsp;
	${taxon.qname}
</h5>

<#list groups as group>
	<#if groupsWithContent?seq_contains(group.qname.toString())>
		<@printGroup group "" group_index />
	</#if>
</#list>
<#list groups as group>
	<#if !groupsWithContent?seq_contains(group.qname.toString()) && !hiddenGroups?seq_contains(group.qname.toString())>
		<@printGroup group "initiallyClosed" group_index />
	</#if>
</#list>

<#macro printGroup group initiallyClosed index>
	<#assign headerLabel = "" />
	<#list locales as locale>
		<#assign headerLabel = headerLabel + (group.label.forLocale(locale)!group.label.forLocale("en")!group.qname) + " ("+locale+")" />
		<#if locale_has_next> <#assign headerLabel  = headerLabel + " &mdash; " /></#if>
	</#list>
	<div class="portlet">
	<div class="portlet-header ${initiallyClosed}">${headerLabel}</div>
	<div class="portlet-content">
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
							<#assign portletLabel = (property.label.forLocale(locale)!property.label.forLocale("en")!property.qname) + " ("+locale+")" />
							<@portletHeader portletLabel "" "locale___"+locale />
								<div class="content">
									<#if existingValue?has_content>
										${existingValue}
									<#else><p class="info emptyContent">Click to add content</p></#if>
								</div>
								<div class="textareaContainer hidden">
									<textarea class="hidden" name="${qname + "___" + locale}" id="${qname + "forlocale" + locale}" <@checkPermissions permissions />>${existingValue?html}</textarea>
									<button class="closeEditorButton doSave">Save & close</button> <button class="closeEditorButton noSave">Close without saving</button>
									<p class="info">You can provide links to other taxa by using syntaxt: <b>[MX.1234]</b> </p>
									<p class="info">In laji.fi portal the above is replaced by a link that has the taxon name.</p>
								</div>
							<@portletFooter />
						</td>
					</#list>
				</tr>
			</#list>
		</table>
	</div>
	</div>
</#macro>

<div class="clear"></div>




</div>


<script>

function initTinyMCE() {
	tinymce.init({
		plugins: "paste",
    	paste_as_text: true
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
			plugins: 'link code paste',
    		toolbar: 'italic bold | link unlink | removeformat | undo, redo | code',
    		paste_as_text: true,
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
	
	$('#descriptionLocalesSelector').on('change', function() {
		var url = window.location.href.split('?')[0];
		if (!$(this).val()) window.location.href = url;
		url += '?descriptionLocales=';
		$.each($(this).val(), function(index,value) {
			url += value+',';
		});
		window.location.href = url;
	});
	
});

function updateOriginal(e) {
	$(e).trigger('change');
}

</script>


<#include "luomus-footer.ftl">



