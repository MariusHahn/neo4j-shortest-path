DROP TABLE result;
CREATE TABLE IF NOT EXISTS result(
    `from` INTEGER,
    `to` INTEGER,
    dijkstraLength INTEGER,
    chLength INTEGER,
    weight REAL,
    dijkstraTime INTEGER,
    chTime INTEGER
);
.import --csv contractionHierarchies/measureQueryPerformance.csv result
SELECT * FROM result;
SELECT *, dijkstraTime*1.0/result.chTime FROM result WHERE dijkstraTime >= chTime
ORDER BY dijkstraTime*1.0/result.chTime DESC , chLength DESC ;
SELECT count(*) FROM result WHERE dijkstraTime >= chTime
ORDER BY dijkstraTime*1.0/result.chTime DESC , chLength DESC ;

SELECT dijkstraLength, avg(dijkstraTime), avg(chTime), avg(dijkstraTime)*1.0/ avg(chTime)
FROM result
GROUP BY dijkstraLength
ORDER BY avg(dijkstraTime)*1.0/ avg(chTime) DESC;

SELECT max(dijkstraTime), min(dijkstraTime), avg(dijkstraTime), max(chTime), min(chTime), avg(chTime)
FROM result
WHERE dijkstraTime > 0;

SELECT AVG((result.dijkstraTime - sub.a) * (result.dijkstraTime - sub.a)) as var from result,
                                                          (SELECT AVG(dijkstraTime) AS a FROM result) AS sub;
SELECT count(*) FROM result;