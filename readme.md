Taxon Search-API
================
## GET /triplestore/taxon-search

For example /triplestore/taxon-search?q=susi&onlyExact=true&format=json&v=2

### Parameters

* q - search word
* limit
* format - json, jsonp, xml (default xml)
* v - version, default 1, for json there is also v=2 available
* requiredInformalTaxonGroup - one or more informal taxon group Qnames (comma separated or multiple parameters)
* onlyExact - true|false, only exact matches (default false)
* onlySpecies - true|false, only species or subspecies etc (default false)
* onlyFinnish - true|false, only finnish taxa (default false)

Returns HTTP 200 on success, HTTP 500 on failure.

Triplestore-API
===============

Triplestore-API provides a way to query and modify contents of LuOnto database. For more general information about LuOnto database, see different documentation. 

### Common parameters

For all requests defined below (except for taxon-search), you can use these common parameters:

* format - RDFXMLABBREV, RDFXML, JSON_RDFXMLABBREV, JSON_RDFXML (default is RDFXMLABBREV)
* limit - integer (there is no hard coded maximum; default is 1000)
* offset - integer 

## GET /triplestore/{Qname}[+{Qname}]

* Get information about one resource (for example triplestore/MX.1
* Get information about many resources with one call (for example triplestore/MX.1+MX.2+MX.3)
* If result type parameter is given, will include additional resources to the response as described

### Parameters

* resulttype
	* NORMAL - default
	* CHILDREN - includes immediate children and children of children and so on; follows MZ.isPartOf predicate or any predicate that is subproperty of MZ.isPartOf (rdfs:subPropertyOf)
	* TREE - exactly the same as CHILDREN for format rdfxml, but for format rdfxmlabbrev direction is reversed: CHILDREN resulttype includes isPartOf predicate as it is and the resource that a child isPartOf goes deeper in the result tree. For result type TREE the parent element is almost the root element and children are added deeper using MZ.hasPart predicate. (Note: not isPartOf but hasPart!).
	* CHAIN - taxonomy chain; exists for backwards compability; exactly the same as CHILDREN, as it follows MX.isPartOf predicate which is subPropetyOf MZ.isPartOf 

## GET /triplestore/search

* Search for resources using any combination of subjects, predicates and objects

### Parameters

* subject
* predicate
* objectresource
* objectliteral
* object - objectresource OR objectliteral; defining the type is more performant, so use objectresource or objectliteral parameter if the type of the object is known
* type  - rdf:type=xxxx
 
(no support for context yet)

The query is an AND search, when several subjects or predicates or objects are used as search parameters.

There are no resulttype parameters.

## DELETE /triplestore/{Qname}

Deletes a resouce. 

* On success returns HTTP 200 and empty rdf-document. 
* If resource did not exist, will return HTTP 200 and empty rdf-document. 
* On error returns HTTP 500.

## POST|PUT /triplestore/{Qname}

Inserts/updates one resource.

###Parameters

* data as body content or 'data' parameter; format: rdfxml or rdfxmlabbrev 

OR to insert/update/delete one predicate

* predicate_qname - required
* objectresource - can not give objectresource AND objectliteral
* objectliteral - can not give objectresource AND objectliteral
* langcode - can only be given when adding objectliteral
* context_qname - default is default context (null context)

Will delete existing predicate, object, langcode, context -statements and replace with the given statement. So if there are for example statements:

* JA.1	rdfs:label	"foobar1"	"fi"
* JA.1	rdfs:label	"foobar2"	"fi"

And parameters predicate=rdfs:label, literal="foofoo", langcode="fi" are given, there will be only one statement:

* JA.1	rdfs:label	"foofoo"	"fi"

But if there are following statements

* JA.1	rdfs:label	"foo"	"fi"
* JA.1	rdfs:label	"foo"	"sv"

And parameters predicate=rdfs:label, literal="foofoo", langcode="fi" are given, there will be these two statements:

* JA.1	rdfs:label	"foofoo"	"fi"
* JA.1	rdfs:label	"foo"	"sv"
 
Similarly, if there are statements from different context than the given context, those statements are not affected.

To delete a literal statement, give empty resourceliteral ("") as parameter. This (altough not very neat) also deletes objectresources of that predicate.

* On success Returns HTTP 200 with the modified resource in the desired format
* On error returns HTTP 500


## GET /triplestore/uri/{namespace}

Get sequence next value for namespace (for example "JA")

Returns HTTP 200 on success, HTTP 500 on failure.

## GET /triplestore/properties/

Returns all properties (type=rdf:Property) and their ranges ("deep search") in JSON_RDFXML format, HTTP 500 on failure.


##Installation

1. Create LuOnto DB user, grant needed privileges (see below)
2. Add triplestore-v2.properties to  <catalina.base>/app-conf
3. Place ojdbc6.jar to <catalina.base>/lib
4. Add http basic authentication permissions to tomcat-users.xml
5. Restart tomcat
6. Deploy triplestore.war


###Grants
~~~
grant connect to ;
grant execute on luonto.addresource to ;
grant execute on luonto.addstatement to ;
grant execute on luonto.addstatementl to ;
grant select,delete on luonto.rdf_statementview to ;
grant select,delete on luonto.rdf_statement to ;
grant select on luonto.rdf_resource to ;
grant select on rdf_statement_history to ;

grant select on luonto.rdf_XX_seq to ;
grant select on luonto.rdf_XX_seq to ; -- All used sequences..
~~~

###Example configuration file
~~~
SystemID = triplestore
SystemQname = 

DevelopmentMode = YES
StagingMode = NO
ProductionMode = NO

BaseURL = http://localhost:8081/triplestore
StaticURL = http://localhost:8081/triplestore/static
LoginURL = http://localhost:8081/triplestore/login

#All other folders are relative to basefolder
BaseFolder = c:/apache-tomcat

LanguageFileFolder = /webapps/triplestore/locale
TemplateFolder     = /webapps/triplestore/triplestore-template
LogFolder          = /application-logs/triplestore


LanguageFiles = locale
SupportedLanguages = en

ErrorReporting_SMTP_Host = localhost
ErrorReporting_SMTP_Username =
ErrorReporting_SMTP_Password =
ErrorReporting_SMTP_SendTo = 
ErrorReporting_SMTP_SendFrom = 
ErrorReporting_SMTP_Subject = Triplestore Dev Error Report

DBdriver = oracle.jdbc.OracleDriver
DBurl = 
DBusername = 
DBpassword = 
LuontoDbName = 

TriplestoreSelf_Username = 
TriplestoreSelf_Password = 

taxonomyEditorBaseURL = ...
lajiETLBaseURL = .../console
~~~


Triplestore Editor
==================

Editor responds from /triplestore/editor

##Installation

Included in Triplestore-API, same configuration file.


Taxon Editor
===============

Taxon editor responds from /triplestore/taxonomy-editor
(Production in taxoneditor.laji.fi)

##Installation

1. Included in Triplestore-API, but requires it's own configuration file
2. Add triplestore-v2-taxonomyeditor.properties to  <catalina.base>/app-conf
3. Production enviroment may require allowing cross domain access to Taxon Editor fonts


###Example configuration file
~~~
SystemID = taxonomy-editor
SystemQname = 
LajiAuthURL = 

DevelopmentMode = YES
StagingMode = NO
ProductionMode = NO

BaseURL = http://localhost:8081/triplestore/taxonomy-editor
StaticURL = http://localhost:8081/triplestore/static
LoginURL = http://localhost:8081/triplestore/taxonomy-editor/login

#All other folders are relative to basefolder
BaseFolder = c:/apache-tomcat

LanguageFileFolder = /webapps/triplestore/locale
TemplateFolder     = /webapps/triplestore/taxonomy-template
LogFolder          = /application-logs/triplestore/taxonomy
ReportFolder       = /application-out/iucn-downloads

LanguageFiles = locale
SupportedLanguages = en

ErrorReporting_SMTP_Host = localhost
ErrorReporting_SMTP_Username =
ErrorReporting_SMTP_Password =
ErrorReporting_SMTP_SendTo =
ErrorReporting_SMTP_SendFrom = 
ErrorReporting_SMTP_Subject = Triplestore Taxon Editor Dev Error Report

DBdriver = oracle.jdbc.OracleDriver
DBurl = 
DBusername = 
DBpassword = 
LuontoDbName = 

#Used for taxon images
KotkaURL = https://kotkatest.luomus.fi

#Used by luomus-commons Taxonomy DAO Base Imple
TriplestoreURL = http://localhost:8081/triplestore
TriplestoreUsername = 
TriplestorePassword = 

#Used in Taxon editor for critical data resolving
DwURL = https://../taxon-use

#Used by IUCN editor
TaxonomyAPIURL = .. api/taxa

~~~



