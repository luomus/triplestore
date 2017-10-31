
CREATE OR REPLACE FORCE EDITIONABLE VIEW "LTKM_LUONTO"."RDF_TAXON_SEARCH" ("CHECKLIST", "QNAME", "NAME", "SCIENTIFICNAME", "AUTHOR", "TAXONRANK", "NAMETYPE", "CASEDNAME") AS 
  SELECT
    checklist.objectname           AS checklist,
    names.subjectname              AS qname,
    UPPER(names.resourceliteral)   AS name,
    scientificname.resourceliteral AS scientificName,
    author.resourceliteral         AS author,
    taxonrank.objectname           AS taxonrank,
    names.predicatename            AS nametype,
    names.resourceliteral   	   AS casedname
  FROM rdf_statementview names
  JOIN rdf_statementview type                ON (names.subjectname = type.subjectname and type.predicatename = 'rdf:type' and type.objectname = 'MX.taxon')
  LEFT JOIN rdf_statementview scientificname ON (names.subjectname = scientificname.subjectname AND scientificname.predicatename = 'MX.scientificName')
  LEFT JOIN rdf_statementview author         ON (names.subjectname = author.subjectname AND author.predicatename = 'MX.scientificNameAuthorship')
  LEFT JOIN rdf_statementview checklist      ON (names.subjectname = checklist.subjectname AND checklist.predicatename = 'MX.nameAccordingTo' )
  LEFT JOIN rdf_statementview taxonrank      ON (names.subjectname = taxonrank.subjectname AND taxonrank.predicatename  = 'MX.taxonRank' )
  WHERE names.resourceliteral IS NOT NULL
  AND names.predicatename IN ( 
    'MX.scientificName',
    'MX.vernacularName',
    'MX.alternativeVernacularName',
    'MX.obsoleteVernacularName',
    'MX.birdlifeCode',
    'MX.euringCode',
    'MX.tradeName'
  );

  
CREATE OR REPLACE FORCE EDITIONABLE VIEW "LTKM_LUONTO"."RDF_TAXON_SEARCH_SYNONYMS" ("CHECKLIST", "QNAME", "NAME", "SCIENTIFICNAME", "AUTHOR", "TAXONRANK", "NAMETYPE", "CASEDNAME") AS 
  SELECT checklist.objectname              AS checklist,
    qname.subjectname                      AS qname,
    UPPER(nameOfSynonyms.resourceliteral)  AS name,
    scientificname.resourceliteral         AS scientificName,
    author.resourceliteral                 AS author,
    taxonrank.objectname                   AS taxonrank,
    qnameOfSynonyms.predicatename          AS nametype,
    nameOfSynonyms.resourceliteral		   AS casedname
  FROM rdf_statementview qname 
  LEFT JOIN rdf_statementview checklist      ON (checklist.subjectname  = qname.subjectname AND checklist.predicatename = 'MX.nameAccordingTo')
  LEFT JOIN rdf_statementview scientificname ON (scientificname.subjectname = qname.subjectname AND scientificname.predicatename = 'MX.scientificName')
  LEFT JOIN rdf_statementview author         ON (author.subjectname = qname.subjectname AND author.predicatename = 'MX.scientificNameAuthorship')
  LEFT JOIN rdf_statementview taxonrank      ON (taxonrank.subjectname = qname.subjectname AND taxonrank.predicatename = 'MX.taxonRank')
  JOIN rdf_statementview circumscription     ON (circumscription.subjectname = qname.subjectname AND circumscription.predicatename = 'MX.circumscription')
  JOIN rdf_statementview qnameOfSynonyms     ON (qnameOfSynonyms.predicatename IN (
  		'MX.circumscription', 'MX.misappliedCircumscription', 'MX.basionymCircumscription', 'MX.misspelledCircumscription', 'MX.uncertainCircumscription' 
  	) AND qnameOfSynonyms.objectname = circumscription.objectname AND qnameOfSynonyms.subjectname != qname.subjectname)
  JOIN rdf_statementview nameOfSynonyms      ON ( nameOfSynonyms.predicatename = 'MX.scientificName' AND nameOfSynonyms.subjectname = qnameOfSynonyms.subjectname )
  WHERE qname.predicatename  = 'rdf:type'
  AND   qname.objectname     = 'MX.taxon';
  
  
drop materialized view TAXON_SEARCH_MATERIALIZED;
CREATE MATERIALIZED VIEW "LTKM_LUONTO"."TAXON_SEARCH_MATERIALIZED" ("CHECKLIST", "QNAME", "NAME", "SCIENTIFICNAME", "AUTHOR", "TAXONRANK", "NAMETYPE", "CASEDNAME")
  BUILD IMMEDIATE
  REFRESH COMPLETE ON DEMAND START WITH sysdate+0 NEXT SYSDATE + 1/48
  AS 
  select DISTINCT * from (
  	select * from rdf_taxon_search 
  	union
  	select * from rdf_taxon_search_synonyms
);
COMMENT ON MATERIALIZED VIEW "LTKM_LUONTO"."TAXON_SEARCH_MATERIALIZED" IS 'snapshot table for snapshot LTKM_LUONTO.TAXON_SEARCH_MATERIALIZED';
    
create index ix_taxon_search_checklistname on taxon_search_materialized (checklist, name);
grant select on TAXON_SEARCH_MATERIALIZED to ltkm_api;
grant select on TAXON_SEARCH_MATERIALIZED to ltkm_taxonviewer;
grant select on TAXON_SEARCH_MATERIALIZED to LTKM_KOTKA;

  