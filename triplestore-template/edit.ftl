<#include "luomus-header.ftl">
<#include "macro.ftl">

<@editorTools />

<h1>Edit ${model.subject.qname}</h1>

<#if successMessage?has_content>
	<div class="successMessage">${successMessage}</div>
</#if>
<#if errorMessage?has_content>
	<div class="errorMessage">${errorMessage}</div>
</#if>

<form onsubmit="return false;">
<table id="resource" class="triplets">
	<caption>${model.type!"rdf:resource"}</caption>
	<thead>
		<tr>
			<th colspan="5" class="buttons">
				<button class="saveButton">Save</button>
				<button class="cancelButton">Cancel</button>
				<button class="deleteAllButton">Delete all</button>
			</th>
		</tr>
		<tr>
			<th style="display: none;">Order</th>
			<th>Predicate</th>
			<th>Range</th>
			<th>Object</th>
			<th></th>
		</tr>
	</thead>
	<tfoot>
		<tr>
			<th colspan="5">
				<div id="addNewPredicateTools">
					Add
					<select id="addNewPredicateSelect"> 
						<option value="">Select predicate</option>
						<#list properties.allProperties as property>
							<option value="${property.qname}">${property.qname}<#if property.label??> - ${(property.label.forLocale("fi")!property.label.forLocale("en")!"")?html}</#if></option>
						</#list>
					</select>
					<input type="text" id="addNewPredicateInput" placeholder="or type predicate here.." /> <button id="addNewPredicateButton">Add</button>
				</div>
			</th>
		</tr>
		<tr>
			<th colspan="5" class="buttons">
				<br />
				<button class="saveButton">Save</button>
				<button class="cancelButton">Cancel</button>
				<button class="deleteAllButton">Delete all</button>
			</th>
		</tr>
	</tfoot>
	<tbody>
		<#list model.statements as statement>
			<#if properties.hasProperty(statement.predicate.qname)>
				<#assign property = properties.getProperty(statement.predicate.qname) />
				<@printProperty property statement />
			<#else>
				<tr>
					<td style="display: none;">-1</td>
					<td>
						<a class="predicate" href="${baseURL}/editor/${statement.predicate.qname}">${statement.predicate.qname}</a><br />
						<span class="error">UNKNOWN PREDICATE FOR THIS TYPE</span>
					</td>
					<#if statement.objectResource??>
						<td> <span class="literal error">resource ??? </span> <span class="error">range not known</span> </td> 
						<td> <@resourceValue statement.objectResource.qname /> </td>
					<#else>
						<td> <span class="literal error">literal ??? </span> <span class="error">range not known</span> </td>
						<td> <@literalValue statement.objectLiteral.content statement.objectLiteral.langcode /> </td>
					</#if>
					<td><button class="deleteButton">Delete</button></td>
				</tr>				
			</#if>
		</#list>
		<#list properties.allProperties as property>
			<#if !model.hasStatements(property.qname)>
				<@printProperty property />
			</#if>
		</#list>
	</tbody>
</table>
</form>

<#macro printProperty property statement="NUL">
		<tr>
			<td style="display: none;">${property.order}</td>
			<td>
				<a class="predicate" href="${baseURL}/editor/${property.qname}">${property.qname}</a><br />
				<span class="label"><#if property.label??>${(property.label.forLocale("fi")!property.label.forLocale("en")!"")?html}</#if></span>
			</td>
			
			<#-- -- RANGE / VALUE CONFLICTS -- -->
			<#if 
				statement != "NUL" && 
				(
					(property.isLiteralProperty() && statement.objectResource??)
					||
					(!property.isLiteralProperty() && statement.objectLiteral??)
				)
			>
				<td>
					<#if statement.objectResource??>
						<span class="literal error">resource !!!</span> 
					<#else>
						<span class="literal error">literal !!!</span> 
					</#if>
					
					<#if property.hasRange()>
						<span class="error">SHOULD BE <a href="${baseURL}/editor/${property.range.qname}">${property.range.qname}</a></span>
					<#else>
						<span class="error">rdfs:range NOT DEFINED!!</span>
					</#if>
				</td>
				<td>
					<#if statement.objectResource??>
						<@resourceValue statement.objectResource.qname />
					<#else>
						<@literalValue statement.objectLiteral.content statement.objectLiteral.langcode />
					</#if>
				</td>
				
			<#-- -- LITERALS -- -->
			<#elseif property.isLiteralProperty()>
				<td>
					<span class="literal">literal</span>
					<#if property.hasRange()>
						<a href="${baseURL}/editor/${property.range.qname}">${property.range.qname}</a>
					<#else>
						<span class="error">rdfs:range NOT DEFINED!</span>
					</#if>
				</td>
				<td>
					<#if statement != "NUL">
						<#assign value = statement.objectLiteral.content />
						<#assign langcode = statement.objectLiteral.langcode />
					<#else>
						<#assign value = "" />
						<#assign langcode = "" />
					</#if>
					
					<#if property.isBooleanProperty()>
						<@booleanValue value langcode />
					<#else>
						<#if property.hasRange()>
							<@literalValue value langcode property.range.qname.toString() />
						<#else>
							<@literalValue value langcode />
						</#if>
					</#if>
				</td>	
			<#-- --RESOURCES -- -->	
			<#else> 
				<td>
					<span class="resource">resource</span> 
					<#if property.hasRange()>
						<a href="${baseURL}/editor/${property.range.qname}">${property.range.qname}</a>
					</#if>
				</td>
				<td>
					<#if statement != "NUL"> 
						<#assign value = statement.objectResource.qname />
					<#else>
						<#assign value = "" />
					</#if>
					<#if property.hasRangeValues()>
						<@resourceValueRangeSelect value property.range.values />
					<#else>
						<@resourceValue value />
					</#if>
				</td>
			</#if>
			<td><button class="deleteButton">Delete</button></td>
		</tr>
</#macro>

<div id="currentDataModel">
	<pre>${modelRdfXml?html}</pre>
</div>

<script>
$(function() {

	sortTable();
	
	$(".chosen").chosen();
	
	$(window).on('unload', function() { // Firefox bug workaround https://bugzilla.mozilla.org/show_bug.cgi?id=46845
		$('form').trigger('reset');
	});
	
	$('body').on('click', ".changeObjectResourceButton", function() {
		$(this).closest('td').find('.objectResourceLink').fadeOut('100');
		$(this).fadeOut('100', function() {
			$(this).closest('td').find('.objectResource').fadeIn('100');
		});
	});
	
	$(".deleteButton").on('click', function() {
		$(this).closest('tr').hide(300, function() { $(this).remove(); });
	});
	$(".deleteAllButton").on('click', function() {
		if (!confirm('Are you sure you want to delete all predicates?')) return false;
		$("#resource").find('tbody').find('tr').remove();
		submit();
	});
	$(".cancelButton").on('click', function() {
		if (!confirm('Are you sure you want to cancel all changes?')) return false;
		blockingLoader();
		location.reload(true);
	});
	$(".saveButton").on('click', function() {
		submit();
	});
	$("#addNewPredicateSelect").on('change', function() {
		var selectedPredicateQname = $(this).val();
		$(this).val('');
		var predicateRowToClone;
		$("#resource").find('tbody').find('tr').each(function() {
			if ($(this).find('.predicate').text() === selectedPredicateQname) {
				predicateRowToClone = $(this);
				return false;
			}
		});
		if (predicateRowToClone === null) { alert('Predicate not found.. something is wrong!'); return; }
		var cloned = predicateRowToClone.clone();
		cloned.find(':input').val('');
		cloned.addClass('newRow');
		$("#resource").find('tbody').append(cloned);
		cloned.find('.changeObjectResourceButton').click();
	});
	$("#addNewPredicateButton").on('click', function() {
		var typedPredicateName = $("#addNewPredicateInput").val();
		$.get('${baseURL}/editor/new-predicate/'+typedPredicateName, function(response) {
			if (response.indexOf("Error") === 0) {
				alert(response);
			} else {
				var newPredicate = $(response);
				newPredicate.addClass('newRow');
				newPredicate.find('button').button().on('click', function() {
					$(this).closest('tr').hide(300, function() { $(this).remove(); });
				});
				$("#resource").find('tbody').append(newPredicate);
			}
		});
	});
});

function submit() {
	blockingLoader();
	var data = buildData();
	
	$.post('${baseURL}/editor/validate/${model.subject.qname}', JSON.stringify(data), function(response) {
  		if (response.hasErrors) {
  			showErrors(response.errors);
  			removeBlockingLoader();
  		} else {
  			submitData(data);
  		}
	}, "json");
}

function showErrors(errors) {
	$(".errorMessage").remove();
	var errorContainer = $('<div class="errorMessage"></div>');
	for (i in errors) {
		var error = errors[i];
		errorContainer.append('<p>'+error+'</p>');
	}
	errorContainer.insertAfter('h1');
	 $('html, body').animate({
        scrollTop: 0
    }, 500);
}

function submitData(data) {
 	var submissionForm = $('<form>', {
    	'action': '${baseURL}/editor/${model.subject.qname}',
    	'method': 'POST'
    }).append($('<input>', {
    	'name': 'data',
    	'value': JSON.stringify(data),
    	'type': 'hidden'
    }));
    $('body').append(submissionForm);
	submissionForm.submit();
}

function buildData() {
	var data = {};
	data.predicates = [];
	$("#resource").find('tbody').find('tr').each(function() {
		var predicate = $(this).find('.predicate').text();
		var objectResource = $(this).find('.objectResource').val();
		var objectLiteral = $(this).find('.objectLiteral').val();
		var langcode = $(this).find('.langcode').val();
		
		if (given(objectResource)) {
			data.predicates.push({
				'predicate': predicate,
				'objectResource': objectResource,
			});
		}
		if (given(objectLiteral)) {
			data.predicates.push({
				'predicate': predicate,
				'objectLiteral': objectLiteral,
				'langcode': langcode
			});
		}
	});
	return data;
}

function given(s) {
	return (typeof s !== 'undefined') && s;
}

function blockingLoader() {
	var blocker = $('<div class="blocker">Please wait...</div>');
	$('body').append(blocker);
}

function removeBlockingLoader() {
	$('.blocker').remove();
}

function sortTable() {
    var table = $("#resource");
    var rows = table.find('tbody').find('tr').toArray().sort(comparer());
    for (var i = 0; i < rows.length; i++) {
    	table.append(rows[i]);
    }
}
function comparer() {
   	return function(a, b) {
       	var valA = getCellValue(a, 0);
       	var valB = getCellValue(b, 0);
       	var c = valA - valB;
       	if (c != 0) return c;
       	valA = getCellValue(a, 1);
       	valB = getCellValue(b, 1);
       	return valA.localeCompare(valB);
   	}
}	
function getCellValue(row, index) { 
	return $(row).children('td').eq(index).html(); 
}
</script>

<#include "luomus-footer.ftl">