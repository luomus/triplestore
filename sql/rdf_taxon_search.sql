
  
--drop materialized view TAXON_SEARCH_MATERIALIZED;
--CREATE MATERIALIZED VIEW "LTKM_LUONTO"."TAXON_SEARCH_MATERIALIZED" ("CHECKLIST", "QNAME", "NAME", "SCIENTIFICNAME", "AUTHOR", "TAXONRANK", "NAMETYPE", "CASEDNAME")
  BUILD IMMEDIATE
  REFRESH COMPLETE ON DEMAND START WITH sysdate+0 NEXT SYSDATE + 1/2
  AS 
  select DISTINCT * from (
  	select * from rdf_taxon_search 
  	union
  	select * from rdf_taxon_search_synonyms
);
COMMENT ON MATERIALIZED VIEW "LTKM_LUONTO"."TAXON_SEARCH_MATERIALIZED" IS 'snapshot table for snapshot LTKM_LUONTO.TAXON_SEARCH_MATERIALIZED';
    
CREATE INDEX ix_taxon_search ON taxon_search_materialized (checklist, name);
CREATE INDEX ix_taxon_search_reverse ON taxon_search_materialized (checklist, REVERSE(name));
CREATE INDEX ix_taxon_search_2 ON taxon_search_materialized (COALESCE(checklist, '.'), name);
CREATE INDEX ix_taxon_search_reverse_2 ON taxon_search_materialized (COALESCE(checklist, '.'), REVERSE(name));

grant select on TAXON_SEARCH_MATERIALIZED to ltkm_api;
grant select on TAXON_SEARCH_MATERIALIZED to ltkm_taxonviewer;
grant select on TAXON_SEARCH_MATERIALIZED to LTKM_KOTKA;

  