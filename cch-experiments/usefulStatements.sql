CREATE TABLE IF NOT EXISTS "measure"(
  "from" INTEGER,
  "to" INTEGER,
  "cchTime" INTEGER,
  "weight" INTEGER,
  "cchHops" INTEGER,
  "cchExpanded" INTEGER,
  "cchLoadInvocations" INTEGER,
  "dijkstraHops" INTEGER,
  "dijkstraTime" INTEGER,
  "DijkstraExpandedNodes" INTEGER
);

.mode csv
.import m1.csv measure
SELECT * FROM measure LIMIT 10;

SELECT AVG(cchTime), AVG(dijkstraTime) FROM measure WHERE 0 < weight AND weight <= 10; 
SELECT AVG(cchTime), AVG(dijkstraTime) FROM measure WHERE 10 < weight AND weight <= 100; 
SELECT AVG(cchTime), AVG(dijkstraTime) FROM measure WHERE 100 < weight AND weight <= 1000; 
SELECT AVG(cchTime), AVG(dijkstraTime) FROM measure WHERE 1000 < weight AND weight <= 10000; 
SELECT AVG(cchTime), AVG(dijkstraTime) FROM measure WHERE 10000 < weight AND weight <= 100000; 
SELECT AVG(cchTime), AVG(dijkstraTime) FROM measure WHERE 100000 < weight AND weight <= 1000000; 
SELECT AVG(cchTime), AVG(dijkstraTime) FROM measure WHERE 1000000 < weight AND weight <= 10000000; 
SELECT AVG(cchTime), AVG(dijkstraTime) FROM measure WHERE 10000000 < weight AND weight <= 100000000; 


SELECT AVG(cchTime), AVG(dijkstraTime), AVG(cchHops) FROM measure WHERE 0 < dijkstraHops AND dijkstraHops <= 10; 
SELECT AVG(cchTime), AVG(dijkstraTime), AVG(cchHops) FROM measure WHERE 10 < dijkstraHops AND dijkstraHops <= 100; 
SELECT AVG(cchTime), AVG(dijkstraTime), AVG(cchHops) FROM measure WHERE 100 < dijkstraHops AND dijkstraHops <= 200; 
SELECT AVG(cchTime), AVG(dijkstraTime), AVG(cchHops) FROM measure WHERE 200 < dijkstraHops AND dijkstraHops <= 300; 
SELECT AVG(cchTime), AVG(dijkstraTime), AVG(cchHops) FROM measure WHERE 300 < dijkstraHops AND dijkstraHops <= 400; 
SELECT AVG(cchTime), AVG(dijkstraTime), AVG(cchHops) FROM measure WHERE 400 < dijkstraHops AND dijkstraHops <= 500; 
SELECT AVG(cchTime), AVG(dijkstraTime), AVG(cchHops) FROM measure WHERE 500 < dijkstraHops AND dijkstraHops <= 600; 
SELECT AVG(cchTime), AVG(dijkstraTime), AVG(cchHops) FROM measure WHERE 600 < dijkstraHops AND dijkstraHops <= 700; 
SELECT AVG(cchTime), AVG(dijkstraTime), AVG(cchHops) FROM measure WHERE 700 < dijkstraHops AND dijkstraHops <= 800; 
SELECT AVG(cchTime), AVG(dijkstraTime), AVG(cchHops) FROM measure WHERE 800 < dijkstraHops AND dijkstraHops <= 900; 
SELECT AVG(cchTime), AVG(dijkstraTime), AVG(cchHops) FROM measure WHERE 900 < dijkstraHops AND dijkstraHops <= 1000; 


SELECT ROUND(AVG(cchTime), 0), ROUND(AVG(dijkstraTime),0), ROUND(AVG(cchExpanded),2) FROM measure WHERE 0 < DijkstraExpandedNodes AND DijkstraExpandedNodes <= 10; 
SELECT ROUND(AVG(cchTime), 0), ROUND(AVG(dijkstraTime),0), ROUND(AVG(cchExpanded),2) FROM measure WHERE 10 < DijkstraExpandedNodes AND DijkstraExpandedNodes <= 100; 
SELECT ROUND(AVG(cchTime), 0), ROUND(AVG(dijkstraTime),0), ROUND(AVG(cchExpanded),2) FROM measure WHERE 100 < DijkstraExpandedNodes AND DijkstraExpandedNodes <= 200; 
SELECT ROUND(AVG(cchTime), 0), ROUND(AVG(dijkstraTime),0), ROUND(AVG(cchExpanded),2) FROM measure WHERE 200 < DijkstraExpandedNodes AND DijkstraExpandedNodes <= 300; 
SELECT ROUND(AVG(cchTime), 0), ROUND(AVG(dijkstraTime),0), ROUND(AVG(cchExpanded),2) FROM measure WHERE 300 < DijkstraExpandedNodes AND DijkstraExpandedNodes <= 400; 
SELECT ROUND(AVG(cchTime), 0), ROUND(AVG(dijkstraTime),0), ROUND(AVG(cchExpanded),2) FROM measure WHERE 400 < DijkstraExpandedNodes AND DijkstraExpandedNodes <= 500; 
SELECT ROUND(AVG(cchTime), 0), ROUND(AVG(dijkstraTime),0), ROUND(AVG(cchExpanded),2) FROM measure WHERE 500 < DijkstraExpandedNodes AND DijkstraExpandedNodes <= 600; 
SELECT ROUND(AVG(cchTime), 0), ROUND(AVG(dijkstraTime),0), ROUND(AVG(cchExpanded),2) FROM measure WHERE 600 < DijkstraExpandedNodes AND DijkstraExpandedNodes <= 700; 
SELECT ROUND(AVG(cchTime), 0), ROUND(AVG(dijkstraTime),0), ROUND(AVG(cchExpanded),2) FROM measure WHERE 700 < DijkstraExpandedNodes AND DijkstraExpandedNodes <= 800; 
SELECT ROUND(AVG(cchTime), 0), ROUND(AVG(dijkstraTime),0), ROUND(AVG(cchExpanded),2) FROM measure WHERE 800 < DijkstraExpandedNodes AND DijkstraExpandedNodes <= 900; 
SELECT ROUND(AVG(cchTime), 0), ROUND(AVG(dijkstraTime),0), ROUND(AVG(cchExpanded),2) FROM measure WHERE 9000 < DijkstraExpandedNodes AND DijkstraExpandedNodes <= 10000; 

