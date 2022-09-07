
CREATE OR REPLACE FORCE EDITIONABLE VIEW "RDF_TAXON_SEARCH" ("CHECKLIST", "QNAME", "NAME", "SCIENTIFICNAME", "AUTHOR", "TAXONRANK", "NAMETYPE", "CASEDNAME", "NAMELANGUAGE") AS 
  SELECT
    checklist.objectname           AS checklist,
    names.subjectname              AS qname,
    REPLACE(REPLACE(REPLACE(UPPER(names.resourceliteral), '×', ''), 'Æ', 'AE'), 'Ë', 'E') AS name,
    scientificname.resourceliteral AS scientificName,
    author.resourceliteral         AS author,
    taxonrank.objectname           AS taxonrank,
    names.predicatename            AS nametype,
    names.resourceliteral   	   AS casedname,
    names.langcodefk               AS namelanguage
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
    'MX.colloquialVernacularName',
    'MX.birdlifeCode',
    'MX.euringCode',
    'MX.tradeName'
  );

  
CREATE OR REPLACE FORCE EDITIONABLE VIEW "RDF_TAXON_SEARCH_SYNONYMS_V2" ("CHECKLIST", "QNAME", "NAME", "SCIENTIFICNAME", "AUTHOR", "TAXONRANK", "NAMETYPE", "CASEDNAME", "NAMELANGUAGE") AS 
  SELECT checklist.objectname              AS checklist,
    qname.subjectname                      AS qname,
    REPLACE(REPLACE(REPLACE(UPPER(nameOfSynonyms.resourceliteral), '×', ''), 'Æ', 'AE'), 'Ë', 'E') AS name,
    scientificname.resourceliteral         AS scientificName,
    author.resourceliteral                 AS author,
    taxonrank.objectname                   AS taxonrank,
    synonymqname.predicatename             AS nametype,
    nameOfSynonyms.resourceliteral		   AS casedname,
    null                                   AS namelanguage
 from rdf_statementview qname
  join rdf_statementview checklist on (qname.subjectname = checklist.subjectname and checklist.predicatename = 'MX.nameAccordingTo')
  LEFT JOIN rdf_statementview scientificname ON (scientificname.subjectname = qname.subjectname AND scientificname.predicatename = 'MX.scientificName')
  LEFT JOIN rdf_statementview author         ON (author.subjectname = qname.subjectname AND author.predicatename = 'MX.scientificNameAuthorship')
  LEFT JOIN rdf_statementview taxonrank      ON (taxonrank.subjectname = qname.subjectname AND taxonrank.predicatename = 'MX.taxonRank')
  JOIN rdf_statementview synonymqname        ON (synonymqname.subjectname = qname.subjectname AND synonymqname.predicatename IN (
  'MX.hasBasionym','MX.hasSynonym','MX.hasObjectiveSynonym','MX.hasSubjectiveSynonym','MX.hasHomotypicSynonym','MX.hasHeterotypicSynonym','MX.hasMisappliedName','MX.hasAlternativeName','MX.hasMisspelledName','MX.hasOrthographicVariant','MX.hasUncertainSynonym'
  ))
  JOIN rdf_statementview nameOfSynonyms      ON ( nameOfSynonyms.predicatename = 'MX.scientificName' AND nameOfSynonyms.subjectname = synonymqname.objectname )
  WHERE qname.predicatename  = 'rdf:type'
  AND   qname.objectname     = 'MX.taxon';
  


select * from  TAXON_SEARCH_MATERIALIZED_V2 fetch first 1 rows only;
drop materialized view TAXON_SEARCH_MATERIALIZED_V2;

CREATE MATERIALIZED VIEW "TAXON_SEARCH_MATERIALIZED_V2" ("CHECKLIST", "QNAME", "NAME", "SCIENTIFICNAME", "AUTHOR", "TAXONRANK", "NAMETYPE", "CASEDNAME", "NAMELANGUAGE")
  BUILD IMMEDIATE
  REFRESH COMPLETE ON DEMAND START WITH sysdate+0 NEXT SYSDATE + 1/2
  AS 
  select DISTINCT * from (
  	select * from rdf_taxon_search 
  	union
  	select * from rdf_taxon_search_synonyms_v2
  );

CREATE INDEX ix_taxon_search ON taxon_search_materialized_v2 (checklist, name);
CREATE INDEX ix_taxon_search_reverse ON taxon_search_materialized_v2 (checklist, REVERSE(name));
CREATE INDEX ix_taxon_search_2 ON taxon_search_materialized_v2 (COALESCE(checklist, '.'), name);
CREATE INDEX ix_taxon_search_reverse_2 ON taxon_search_materialized_v2 (COALESCE(checklist, '.'), REVERSE(name));

grant select on TAXON_SEARCH_MATERIALIZED_V2 to ltkm_api;
grant select on TAXON_SEARCH_MATERIALIZED_V2 to ltkm_taxonviewer;
grant select on TAXON_SEARCH_MATERIALIZED_V2 to LTKM_KOTKA;

