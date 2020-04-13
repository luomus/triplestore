CREATE MATERIALIZED VIEW "LTKM_LUONTO"."RDF_CLASSVIEW_MATERIALIZED" ("RESOURCEID", "RESOURCENAME", "RESOURCECOUNT")
  TABLESPACE "USERS"
  BUILD IMMEDIATE
  REFRESH COMPLETE ON DEMAND START WITH sysdate+0 NEXT (SYSDATE+1/2)
  AS SELECT
  a.resourceid,
  a.resourcename,
  COALESCE(d.resourcecount,0) AS resourcecount
FROM rdf_statement x
JOIN rdf_resource a ON x.subjectfk = a.resourceid
JOIN rdf_resource b ON x.predicatefk = b.resourceid AND b.resourceid = 1
JOIN rdf_resource c ON x.objectfk    = c.resourceid AND c.resourceid = 25
LEFT JOIN
  (
  SELECT objectfk,
  COUNT(*) AS resourcecount
  FROM rdf_statement x
  WHERE predicatefk = 1
  GROUP BY objectfk
  ) d ON x.subjectfk = d.objectfk
;
grant select on RDF_CLASSVIEW_MATERIALIZED to ltkm_api;