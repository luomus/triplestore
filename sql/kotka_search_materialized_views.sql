
CREATE MATERIALIZED VIEW RDF_TAXON_NAMES_MATERIALIZED
  BUILD IMMEDIATE
  REFRESH COMPLETE ON DEMAND START WITH sysdate+0 NEXT SYSDATE + 1/2
  AS  
  	SELECT a.subjectname AS taxon_qname,
    b.resourceliteral  AS scientificname,
    c.objectname       AS rank,
    d.objectname       AS source,
    e.resourceliteral  AS finnishname,
    f.resourceliteral  AS swedishname
  	FROM rdf_statementview a
  	LEFT JOIN rdf_statementview b ON a.subjectname = b.subjectname AND b.predicatename = 'MX.scientificName'
  	LEFT JOIN rdf_statementview c ON a.subjectname = c.subjectname AND c.predicatename = 'MX.taxonRank'
  	LEFT JOIN rdf_statementview d ON a.subjectname = d.subjectname AND d.predicatename = 'MX.nameAccordingTo'
  	LEFT JOIN rdf_statementview e ON a.subjectname = e.subjectname AND e.predicatename = 'MX.vernacularName' AND e.langcodefk = 'fi'
  	LEFT JOIN rdf_statementview f ON a.subjectname  = f.subjectname AND f.predicatename  = 'MX.vernacularName' AND f.langcodefk = 'sv'
  	WHERE a.predicatename = 'rdf:type'
  	AND a.objectname      = 'MX.taxon'
;
grant select on RDF_TAXON_NAMES_MATERIALIZED to LTKM_KOTKA;

CREATE OR REPLACE VIEW RDF_TAXON_NAMES
  AS 
  SELECT taxon_qname, scientificname, rank, source, finnishname, swedishname FROM rdf_taxon_names_materialized
;
grant select on RDF_TAXON_NAMES to LTKM_KOTKA;







CREATE MATERIALIZED VIEW RDF_TAXON_PARENT_MATERIALIZED 
  BUILD IMMEDIATE
  REFRESH COMPLETE ON DEMAND START WITH sysdate+0 NEXT SYSDATE + 1/2
  AS
	SELECT subjectname As taxon_qname,objectname AS parent_qname
	FROM rdf_statementview
	WHERE predicatename = 'MX.isPartOf'CREATE INDEX IX_TAXON_PARENT_MATERIALIZED ON RDF_TAXON_PARENT_MATERIALIZED (TAXON_QNAME ASC, PARENT_QNAME ASC)
;
CREATE INDEX IX_TAXON_PARENT_MATERIALIZED ON RDF_TAXON_PARENT_MATERIALIZED (TAXON_QNAME, PARENT_QNAME);






CREATE MATERIALIZED VIEW RDF_TAXON_PARENTS_MATERIALIZED 
  BUILD IMMEDIATE
  REFRESH COMPLETE ON DEMAND START WITH sysdate+0 NEXT SYSDATE + 1/2
  AS 
SELECT p1.taxon_qname,p1.parent_qname,1 AS step
FROM   rdf_taxon_parent_materialized p1
UNION
SELECT p1.taxon_qname,p2.parent_qname,2 AS step
FROM   rdf_taxon_parent_materialized p1
JOIN   rdf_taxon_parent_materialized p2 ON p1.parent_qname = p2.taxon_qname
UNION
SELECT p1.taxon_qname,p3.parent_qname,3 AS step
FROM   rdf_taxon_parent_materialized p1
JOIN   rdf_taxon_parent_materialized p2 ON p1.parent_qname = p2.taxon_qname
JOIN   rdf_taxon_parent_materialized p3 ON p2.parent_qname = p3.taxon_qname
UNION
SELECT p1.taxon_qname,p4.parent_qname,4 AS step
FROM   rdf_taxon_parent_materialized p1
JOIN   rdf_taxon_parent_materialized p2 ON p1.parent_qname = p2.taxon_qname
JOIN   rdf_taxon_parent_materialized p3 ON p2.parent_qname = p3.taxon_qname
JOIN   rdf_taxon_parent_materialized p4 ON p3.parent_qname = p4.taxon_qname
UNION
SELECT p1.taxon_qname,p5.parent_qname,5 AS step
FROM   rdf_taxon_parent_materialized p1
JOIN   rdf_taxon_parent_materialized p2 ON p1.parent_qname = p2.taxon_qname
JOIN   rdf_taxon_parent_materialized p3 ON p2.parent_qname = p3.taxon_qname
JOIN   rdf_taxon_parent_materialized p4 ON p3.parent_qname = p4.taxon_qname
JOIN   rdf_taxon_parent_materialized p5 ON p4.parent_qname = p5.taxon_qname
UNION
SELECT p1.taxon_qname,p6.parent_qname,6 AS step
FROM   rdf_taxon_parent_materialized p1
JOIN   rdf_taxon_parent_materialized p2 ON p1.parent_qname = p2.taxon_qname
JOIN   rdf_taxon_parent_materialized p3 ON p2.parent_qname = p3.taxon_qname
JOIN   rdf_taxon_parent_materialized p4 ON p3.parent_qname = p4.taxon_qname
JOIN   rdf_taxon_parent_materialized p5 ON p4.parent_qname = p5.taxon_qname
JOIN   rdf_taxon_parent_materialized p6 ON p5.parent_qname = p6.taxon_qname
UNION
SELECT p1.taxon_qname,p7.parent_qname,7 AS step
FROM   rdf_taxon_parent_materialized p1
JOIN   rdf_taxon_parent_materialized p2 ON p1.parent_qname = p2.taxon_qname
JOIN   rdf_taxon_parent_materialized p3 ON p2.parent_qname = p3.taxon_qname
JOIN   rdf_taxon_parent_materialized p4 ON p3.parent_qname = p4.taxon_qname
JOIN   rdf_taxon_parent_materialized p5 ON p4.parent_qname = p5.taxon_qname
JOIN   rdf_taxon_parent_materialized p6 ON p5.parent_qname = p6.taxon_qname
JOIN   rdf_taxon_parent_materialized p7 ON p6.parent_qname = p7.taxon_qname
UNION
SELECT p1.taxon_qname,p8.parent_qname,8 AS step
FROM   rdf_taxon_parent_materialized p1
JOIN   rdf_taxon_parent_materialized p2 ON p1.parent_qname = p2.taxon_qname
JOIN   rdf_taxon_parent_materialized p3 ON p2.parent_qname = p3.taxon_qname
JOIN   rdf_taxon_parent_materialized p4 ON p3.parent_qname = p4.taxon_qname
JOIN   rdf_taxon_parent_materialized p5 ON p4.parent_qname = p5.taxon_qname
JOIN   rdf_taxon_parent_materialized p6 ON p5.parent_qname = p6.taxon_qname
JOIN   rdf_taxon_parent_materialized p7 ON p6.parent_qname = p7.taxon_qname
JOIN   rdf_taxon_parent_materialized p8 ON p7.parent_qname = p8.taxon_qname
UNION
SELECT p1.taxon_qname,p9.parent_qname,9 AS step
FROM   rdf_taxon_parent_materialized p1
JOIN   rdf_taxon_parent_materialized p2 ON p1.parent_qname = p2.taxon_qname
JOIN   rdf_taxon_parent_materialized p3 ON p2.parent_qname = p3.taxon_qname
JOIN   rdf_taxon_parent_materialized p4 ON p3.parent_qname = p4.taxon_qname
JOIN   rdf_taxon_parent_materialized p5 ON p4.parent_qname = p5.taxon_qname
JOIN   rdf_taxon_parent_materialized p6 ON p5.parent_qname = p6.taxon_qname
JOIN   rdf_taxon_parent_materialized p7 ON p6.parent_qname = p7.taxon_qname
JOIN   rdf_taxon_parent_materialized p8 ON p7.parent_qname = p8.taxon_qname
JOIN   rdf_taxon_parent_materialized p9 ON p8.parent_qname = p9.taxon_qname
UNION
SELECT p1.taxon_qname,p10.parent_qname,10 AS step
FROM   rdf_taxon_parent_materialized p1
JOIN   rdf_taxon_parent_materialized p2 ON p1.parent_qname = p2.taxon_qname
JOIN   rdf_taxon_parent_materialized p3 ON p2.parent_qname = p3.taxon_qname
JOIN   rdf_taxon_parent_materialized p4 ON p3.parent_qname = p4.taxon_qname
JOIN   rdf_taxon_parent_materialized p5 ON p4.parent_qname = p5.taxon_qname
JOIN   rdf_taxon_parent_materialized p6 ON p5.parent_qname = p6.taxon_qname
JOIN   rdf_taxon_parent_materialized p7 ON p6.parent_qname = p7.taxon_qname
JOIN   rdf_taxon_parent_materialized p8 ON p7.parent_qname = p8.taxon_qname
JOIN   rdf_taxon_parent_materialized p9 ON p8.parent_qname = p9.taxon_qname
JOIN   rdf_taxon_parent_materialized p10 ON p9.parent_qname = p10.taxon_qname
UNION
SELECT p1.taxon_qname,p11.parent_qname,11 AS step
FROM   rdf_taxon_parent_materialized p1
JOIN   rdf_taxon_parent_materialized p2 ON p1.parent_qname = p2.taxon_qname
JOIN   rdf_taxon_parent_materialized p3 ON p2.parent_qname = p3.taxon_qname
JOIN   rdf_taxon_parent_materialized p4 ON p3.parent_qname = p4.taxon_qname
JOIN   rdf_taxon_parent_materialized p5 ON p4.parent_qname = p5.taxon_qname
JOIN   rdf_taxon_parent_materialized p6 ON p5.parent_qname = p6.taxon_qname
JOIN   rdf_taxon_parent_materialized p7 ON p6.parent_qname = p7.taxon_qname
JOIN   rdf_taxon_parent_materialized p8 ON p7.parent_qname = p8.taxon_qname
JOIN   rdf_taxon_parent_materialized p9 ON p8.parent_qname = p9.taxon_qname
JOIN   rdf_taxon_parent_materialized p10 ON p9.parent_qname = p10.taxon_qname
JOIN   rdf_taxon_parent_materialized p11 ON p10.parent_qname = p11.taxon_qname
UNION
SELECT p1.taxon_qname,p12.parent_qname,12 AS step
FROM   rdf_taxon_parent_materialized p1
JOIN   rdf_taxon_parent_materialized p2 ON p1.parent_qname = p2.taxon_qname
JOIN   rdf_taxon_parent_materialized p3 ON p2.parent_qname = p3.taxon_qname
JOIN   rdf_taxon_parent_materialized p4 ON p3.parent_qname = p4.taxon_qname
JOIN   rdf_taxon_parent_materialized p5 ON p4.parent_qname = p5.taxon_qname
JOIN   rdf_taxon_parent_materialized p6 ON p5.parent_qname = p6.taxon_qname
JOIN   rdf_taxon_parent_materialized p7 ON p6.parent_qname = p7.taxon_qname
JOIN   rdf_taxon_parent_materialized p8 ON p7.parent_qname = p8.taxon_qname
JOIN   rdf_taxon_parent_materialized p9 ON p8.parent_qname = p9.taxon_qname
JOIN   rdf_taxon_parent_materialized p10 ON p9.parent_qname = p10.taxon_qname
JOIN   rdf_taxon_parent_materialized p11 ON p10.parent_qname = p11.taxon_qname
JOIN   rdf_taxon_parent_materialized p12 ON p11.parent_qname = p12.taxon_qname
UNION
SELECT p1.taxon_qname,p13.parent_qname,13 AS step
FROM   rdf_taxon_parent_materialized p1
JOIN   rdf_taxon_parent_materialized p2 ON p1.parent_qname = p2.taxon_qname
JOIN   rdf_taxon_parent_materialized p3 ON p2.parent_qname = p3.taxon_qname
JOIN   rdf_taxon_parent_materialized p4 ON p3.parent_qname = p4.taxon_qname
JOIN   rdf_taxon_parent_materialized p5 ON p4.parent_qname = p5.taxon_qname
JOIN   rdf_taxon_parent_materialized p6 ON p5.parent_qname = p6.taxon_qname
JOIN   rdf_taxon_parent_materialized p7 ON p6.parent_qname = p7.taxon_qname
JOIN   rdf_taxon_parent_materialized p8 ON p7.parent_qname = p8.taxon_qname
JOIN   rdf_taxon_parent_materialized p9 ON p8.parent_qname = p9.taxon_qname
JOIN   rdf_taxon_parent_materialized p10 ON p9.parent_qname = p10.taxon_qname
JOIN   rdf_taxon_parent_materialized p11 ON p10.parent_qname = p11.taxon_qname
JOIN   rdf_taxon_parent_materialized p12 ON p11.parent_qname = p12.taxon_qname
JOIN   rdf_taxon_parent_materialized p13 ON p12.parent_qname = p13.taxon_qname
UNION
SELECT p1.taxon_qname,p14.parent_qname,14 AS step
FROM   rdf_taxon_parent_materialized p1
JOIN   rdf_taxon_parent_materialized p2 ON p1.parent_qname = p2.taxon_qname
JOIN   rdf_taxon_parent_materialized p3 ON p2.parent_qname = p3.taxon_qname
JOIN   rdf_taxon_parent_materialized p4 ON p3.parent_qname = p4.taxon_qname
JOIN   rdf_taxon_parent_materialized p5 ON p4.parent_qname = p5.taxon_qname
JOIN   rdf_taxon_parent_materialized p6 ON p5.parent_qname = p6.taxon_qname
JOIN   rdf_taxon_parent_materialized p7 ON p6.parent_qname = p7.taxon_qname
JOIN   rdf_taxon_parent_materialized p8 ON p7.parent_qname = p8.taxon_qname
JOIN   rdf_taxon_parent_materialized p9 ON p8.parent_qname = p9.taxon_qname
JOIN   rdf_taxon_parent_materialized p10 ON p9.parent_qname = p10.taxon_qname
JOIN   rdf_taxon_parent_materialized p11 ON p10.parent_qname = p11.taxon_qname
JOIN   rdf_taxon_parent_materialized p12 ON p11.parent_qname = p12.taxon_qname
JOIN   rdf_taxon_parent_materialized p13 ON p12.parent_qname = p13.taxon_qname
JOIN   rdf_taxon_parent_materialized p14 ON p13.parent_qname = p14.taxon_qname
UNION
SELECT p1.taxon_qname,p15.parent_qname,15 AS step
FROM   rdf_taxon_parent_materialized p1
JOIN   rdf_taxon_parent_materialized p2 ON p1.parent_qname = p2.taxon_qname
JOIN   rdf_taxon_parent_materialized p3 ON p2.parent_qname = p3.taxon_qname
JOIN   rdf_taxon_parent_materialized p4 ON p3.parent_qname = p4.taxon_qname
JOIN   rdf_taxon_parent_materialized p5 ON p4.parent_qname = p5.taxon_qname
JOIN   rdf_taxon_parent_materialized p6 ON p5.parent_qname = p6.taxon_qname
JOIN   rdf_taxon_parent_materialized p7 ON p6.parent_qname = p7.taxon_qname
JOIN   rdf_taxon_parent_materialized p8 ON p7.parent_qname = p8.taxon_qname
JOIN   rdf_taxon_parent_materialized p9 ON p8.parent_qname = p9.taxon_qname
JOIN   rdf_taxon_parent_materialized p10 ON p9.parent_qname = p10.taxon_qname
JOIN   rdf_taxon_parent_materialized p11 ON p10.parent_qname = p11.taxon_qname
JOIN   rdf_taxon_parent_materialized p12 ON p11.parent_qname = p12.taxon_qname
JOIN   rdf_taxon_parent_materialized p13 ON p12.parent_qname = p13.taxon_qname
JOIN   rdf_taxon_parent_materialized p14 ON p13.parent_qname = p14.taxon_qname
JOIN   rdf_taxon_parent_materialized p15 ON p14.parent_qname = p15.taxon_qname
UNION
SELECT p1.taxon_qname,p16.parent_qname,16 AS step
FROM   rdf_taxon_parent_materialized p1
JOIN   rdf_taxon_parent_materialized p2 ON p1.parent_qname = p2.taxon_qname
JOIN   rdf_taxon_parent_materialized p3 ON p2.parent_qname = p3.taxon_qname
JOIN   rdf_taxon_parent_materialized p4 ON p3.parent_qname = p4.taxon_qname
JOIN   rdf_taxon_parent_materialized p5 ON p4.parent_qname = p5.taxon_qname
JOIN   rdf_taxon_parent_materialized p6 ON p5.parent_qname = p6.taxon_qname
JOIN   rdf_taxon_parent_materialized p7 ON p6.parent_qname = p7.taxon_qname
JOIN   rdf_taxon_parent_materialized p8 ON p7.parent_qname = p8.taxon_qname
JOIN   rdf_taxon_parent_materialized p9 ON p8.parent_qname = p9.taxon_qname
JOIN   rdf_taxon_parent_materialized p10 ON p9.parent_qname = p10.taxon_qname
JOIN   rdf_taxon_parent_materialized p11 ON p10.parent_qname = p11.taxon_qname
JOIN   rdf_taxon_parent_materialized p12 ON p11.parent_qname = p12.taxon_qname
JOIN   rdf_taxon_parent_materialized p13 ON p12.parent_qname = p13.taxon_qname
JOIN   rdf_taxon_parent_materialized p14 ON p13.parent_qname = p14.taxon_qname
JOIN   rdf_taxon_parent_materialized p15 ON p14.parent_qname = p15.taxon_qname
JOIN   rdf_taxon_parent_materialized p16 ON p15.parent_qname = p16.taxon_qname
UNION
SELECT p1.taxon_qname,p17.parent_qname,17 AS step
FROM   rdf_taxon_parent_materialized p1
JOIN   rdf_taxon_parent_materialized p2 ON p1.parent_qname = p2.taxon_qname
JOIN   rdf_taxon_parent_materialized p3 ON p2.parent_qname = p3.taxon_qname
JOIN   rdf_taxon_parent_materialized p4 ON p3.parent_qname = p4.taxon_qname
JOIN   rdf_taxon_parent_materialized p5 ON p4.parent_qname = p5.taxon_qname
JOIN   rdf_taxon_parent_materialized p6 ON p5.parent_qname = p6.taxon_qname
JOIN   rdf_taxon_parent_materialized p7 ON p6.parent_qname = p7.taxon_qname
JOIN   rdf_taxon_parent_materialized p8 ON p7.parent_qname = p8.taxon_qname
JOIN   rdf_taxon_parent_materialized p9 ON p8.parent_qname = p9.taxon_qname
JOIN   rdf_taxon_parent_materialized p10 ON p9.parent_qname = p10.taxon_qname
JOIN   rdf_taxon_parent_materialized p11 ON p10.parent_qname = p11.taxon_qname
JOIN   rdf_taxon_parent_materialized p12 ON p11.parent_qname = p12.taxon_qname
JOIN   rdf_taxon_parent_materialized p13 ON p12.parent_qname = p13.taxon_qname
JOIN   rdf_taxon_parent_materialized p14 ON p13.parent_qname = p14.taxon_qname
JOIN   rdf_taxon_parent_materialized p15 ON p14.parent_qname = p15.taxon_qname
JOIN   rdf_taxon_parent_materialized p16 ON p15.parent_qname = p16.taxon_qname
JOIN   rdf_taxon_parent_materialized p17 ON p16.parent_qname = p17.taxon_qname
UNION
SELECT p1.taxon_qname,p18.parent_qname,18 AS step
FROM   rdf_taxon_parent_materialized p1
JOIN   rdf_taxon_parent_materialized p2 ON p1.parent_qname = p2.taxon_qname
JOIN   rdf_taxon_parent_materialized p3 ON p2.parent_qname = p3.taxon_qname
JOIN   rdf_taxon_parent_materialized p4 ON p3.parent_qname = p4.taxon_qname
JOIN   rdf_taxon_parent_materialized p5 ON p4.parent_qname = p5.taxon_qname
JOIN   rdf_taxon_parent_materialized p6 ON p5.parent_qname = p6.taxon_qname
JOIN   rdf_taxon_parent_materialized p7 ON p6.parent_qname = p7.taxon_qname
JOIN   rdf_taxon_parent_materialized p8 ON p7.parent_qname = p8.taxon_qname
JOIN   rdf_taxon_parent_materialized p9 ON p8.parent_qname = p9.taxon_qname
JOIN   rdf_taxon_parent_materialized p10 ON p9.parent_qname = p10.taxon_qname
JOIN   rdf_taxon_parent_materialized p11 ON p10.parent_qname = p11.taxon_qname
JOIN   rdf_taxon_parent_materialized p12 ON p11.parent_qname = p12.taxon_qname
JOIN   rdf_taxon_parent_materialized p13 ON p12.parent_qname = p13.taxon_qname
JOIN   rdf_taxon_parent_materialized p14 ON p13.parent_qname = p14.taxon_qname
JOIN   rdf_taxon_parent_materialized p15 ON p14.parent_qname = p15.taxon_qname
JOIN   rdf_taxon_parent_materialized p16 ON p15.parent_qname = p16.taxon_qname
JOIN   rdf_taxon_parent_materialized p17 ON p16.parent_qname = p17.taxon_qname
JOIN   rdf_taxon_parent_materialized p18 ON p17.parent_qname = p18.taxon_qname
UNION
SELECT p1.taxon_qname,p19.parent_qname,19 AS step
FROM   rdf_taxon_parent_materialized p1
JOIN   rdf_taxon_parent_materialized p2 ON p1.parent_qname = p2.taxon_qname
JOIN   rdf_taxon_parent_materialized p3 ON p2.parent_qname = p3.taxon_qname
JOIN   rdf_taxon_parent_materialized p4 ON p3.parent_qname = p4.taxon_qname
JOIN   rdf_taxon_parent_materialized p5 ON p4.parent_qname = p5.taxon_qname
JOIN   rdf_taxon_parent_materialized p6 ON p5.parent_qname = p6.taxon_qname
JOIN   rdf_taxon_parent_materialized p7 ON p6.parent_qname = p7.taxon_qname
JOIN   rdf_taxon_parent_materialized p8 ON p7.parent_qname = p8.taxon_qname
JOIN   rdf_taxon_parent_materialized p9 ON p8.parent_qname = p9.taxon_qname
JOIN   rdf_taxon_parent_materialized p10 ON p9.parent_qname = p10.taxon_qname
JOIN   rdf_taxon_parent_materialized p11 ON p10.parent_qname = p11.taxon_qname
JOIN   rdf_taxon_parent_materialized p12 ON p11.parent_qname = p12.taxon_qname
JOIN   rdf_taxon_parent_materialized p13 ON p12.parent_qname = p13.taxon_qname
JOIN   rdf_taxon_parent_materialized p14 ON p13.parent_qname = p14.taxon_qname
JOIN   rdf_taxon_parent_materialized p15 ON p14.parent_qname = p15.taxon_qname
JOIN   rdf_taxon_parent_materialized p16 ON p15.parent_qname = p16.taxon_qname
JOIN   rdf_taxon_parent_materialized p17 ON p16.parent_qname = p17.taxon_qname
JOIN   rdf_taxon_parent_materialized p18 ON p17.parent_qname = p18.taxon_qname
JOIN   rdf_taxon_parent_materialized p19 ON p18.parent_qname = p19.taxon_qnameCREATE INDEX IX_MV_TAXON_PARENTS1 ON RDF_TAXON_PARENTS_MATERIALIZED (PARENT_QNAME ASC, TAXON_QNAME ASC) 
;
CREATE INDEX IX_MV_TAXON_PARENTS1 ON RDF_TAXON_PARENTS_MATERIALIZED (PARENT_QNAME, TAXON_QNAME);




 
CREATE OR REPLACE VIEW RDF_TAXON_CHILDREN (TAXON_QNAME, STEP, CHILD_QNAME, SCIENTIFICNAME, RANK, SOURCE, FINNISHNAME, SWEDISHNAME) AS 
  SELECT a.parent_qname AS taxon_qname,-1 AS step,a.taxon_qname AS child_qname,b.scientificname,b.rank,b.source,
  b.finnishname,b.swedishname
  FROM rdf_taxon_parent_materialized a
  LEFT JOIN rdf_taxon_names_materialized b ON a.taxon_qname = b.taxon_qname;

CREATE OR REPLACE VIEW RDF_TAXON_PARENTS (TAXON_QNAME, STEP, PARENT_QNAME, SCIENTIFICNAME, RANK, SOURCE, FINNISHNAME, SWEDISHNAME) AS 
  SELECT a.taxon_qname,a.step,a.parent_qname,b.scientificname,b.rank,b.source,b.finnishname,b.swedishname
  FROM rdf_taxon_parents_materialized a
  LEFT JOIN rdf_taxon_names_materialized b ON a.parent_qname = b.taxon_qname;

CREATE OR REPLACE VIEW RDF_TAXON_PARENTS_CHILDREN (TAXON_QNAME, STEP, STEP_QNAME, SCIENTIFICNAME, RANK, SOURCE, FINNISHNAME, SWEDISHNAME) AS 
  SELECT taxon_qname,step,parent_qname AS step_qname,scientificname,rank,source,finnishname,swedishname
  FROM rdf_taxon_parents
  UNION
  SELECT taxon_qname,0 AS step,taxon_qname,scientificname,rank,source,finnishname,swedishname
  FROM rdf_taxon_names_materialized
  UNION
  SELECT taxon_qname,step,child_qname AS step_qname,scientificname,rank,source,finnishname,swedishname
  FROM rdf_taxon_children;
grant select on RDF_TAXON_PARENTS_CHILDREN to LTKM_KOTKA;




        