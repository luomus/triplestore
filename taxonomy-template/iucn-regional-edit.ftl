<#include "luomus-header.ftl">
<#include "iucn-macro.ftl">

<#list target.groups as groupQname>
	<a href="${baseURL}/iucn/group/${groupQname}/${selectedYear}" class="goBack">
		Takaisin (${taxonGroups[groupQname].name.forLocale("fi")!""})
	</a><br />
</#list>

<@toolbox/>		

<div id="evaluationEdit">

<h1>Alueellinen uhanalaisuus - ${selectedYear}</h1>

<h2><a href="#"><@printScientificNameAndAuthor taxon /> ${taxon.vernacularName.forLocale("fi")!""}</a></h2>


<#if successMessage?has_content>
	<p class="successMessage">${successMessage?html}</p>
</#if>
<#if errorMessage?has_content>
	<div class="errorMessage">
		<h4>Tietoja ei tallennettu</h4>
		${errorMessage}
	</div>
	<script>
		var erroreousFields = [];
		<#list erroreousFields as erroreousField>
			erroreousFields.push('${erroreousField}');
		</#list>
	</script>
</#if>

<div id="evaluationMeta">



<#if editHistory?has_content>
	<div class="taxonInfo">
		<h6>Tallennushistoria</h6>
		<table class="iucnSpeciesTable">
			<tr>
				<th>Kommentti</th>
				<th>Pvm</th>
				<th>Muokkaaja</th>
			</tr>
			<#list editHistory.entries as entry>
				<tr>
					<td>${(entry.notes?html)!""}</td>
					<td>${entry.date!""}</td>
					<td>
						<#if entry.editorQname??>
							${persons[entry.editorQname].fullname?html}
						</#if>
					</td>
				</tr>
			</#list>
		</table>
	</div>
</#if>

<div class="taxonInfo">
	<h6>Arviointihistoria</h6>
	<table class="iucnSpeciesTable">
		<tr>
			<th>Vuosi</th>
			<th>Luokka</th>
			<th>RLI</th>
		</tr>
		<#list evaluationYears as year>
			<#if target.hasEvaluation(year)>
				<#assign yearEval = target.getEvaluation(year)> 
				<tr>
					<td><a href="${baseURL}/iucn/species/${target.qname}/${year}">${year}</a></td>
					<td>
						<@iucnStatus yearEval />
		    		</td>
					<td>
						<@iucnIndexCorrectedStatus yearEval />
					</td>
				</tr>
			<#else>
				<tr>
					<td>${year}</td>
					<td>-</td>
					<td>-</td>
				</tr>
			</#if>
		</#list>
	</table>
</div>


</div>
<div class="clear"></div>

<hr />


<#if evaluation?? && user?? && user.admin>
<h5>${(evaluation.id)}</h5>
</#if>


<#if permissions>


<form id="evaluationEditForm" action="${baseURL}/iucn/regional/${taxon.qname}/${selectedYear}" method="post" onsubmit="return false;">
<input type="hidden" name="evaluationId" value="${(evaluation.id)!""}" />
<input type="hidden" name="MKV.evaluatedTaxon" value="${taxon.qname}" />
<input type="hidden" name="MKV.evaluationYear" value="${selectedYear}" />

</#if>

<table class="evaluationEdit regionalEvaluationEdit">
	<thead>
		<tr>
			<th>Muuttuja</th>
			<th>
				<#if comparison??>
					${comparison.evaluationYear} tiedot
				<#else>
					Ei edellisiä tietoja
				</#if>
			</th>
			<th>${selectedYear} tiedot</th>
		</tr>
	</thead>
	<tbody>
	
	<@iucnSection "Esiintymisalueet ja alueellinen uhanalaisuus <span> &mdash; Täytettävä luokille NT-CR&nbsp;&nbsp; Alueellinen uhanalaisuus vain luokille LC ja NT</span> &nbsp;&nbsp; <a href=\"${staticURL}/img/Aluellisen arvioinnin tarkastelualueet.jpg\" target=\"_staticmap\">&raquo; Alueet</a> " />
	<#list areas?keys as areaQname>
		<@iucnOccurrence areaQname />
	</#list>
	
	</tbody>
</table>

<#if permissions>
	<div class="submitButtonContainer">
		<button class="submitButton" id="readyButton" class="ready">Valmis</button>
		<button class="submitButton" id="cancelButton">Peruuta kaikki muutokset</button>
	</div>
</form>
</#if>

</div>

<script>

$(function() {

 	$("#readyButton").on('click', function() {
 		$("#saveButton, #readyButton, #readyForCommentsButton").prop("disabled", 'disabled');
 		document.getElementById("evaluationEditForm").submit();
 	});
 	
 	$("#cancelButton").on('click', function() {
 		if (confirm("Peruuta muutokset?")) {
 			location.href = location.href;
 		}
 	});
 	
 	$('#evaluationEditForm').find('input,select').keydown(function(event){
        if ( event.keyCode == 13 ){
            event.preventDefault();
        }
    });

});

$(function() {
    if (typeof erroreousFields !== 'undefined') {
    	for (i in erroreousFields) {
    		var erroreousField = erroreousFields[i];
    		$('input[name*="'+erroreousField+'"], select[name*="'+erroreousField+'"]').addClass('validationError').each(function() {
    			$(this).parent().find('div').find('span').addClass('validationError');
    		});
    	}
    }
});
</script>

<#include "luomus-footer.ftl">