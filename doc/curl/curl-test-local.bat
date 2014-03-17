REM ACDSense - delete full dataset by deleting all tests
curl -v -X "DELETE" http://localhost:8080/tests/s10r10WFP --user -7862164820900576677:dspgtud
pause

curl -v -X "DELETE" http://localhost:8080/tests/s100r1JSO --user -7862164820900576677:dspgtud
pause

REM ACDSense - create test for demonstrating CSV operations (test id: s100r1JSO)
curl -v -X POST http://localhost:8080/tests?format=csv --user -7862164820900576677:dspgtud --data-binary @meta.csv -H 'Content-type:text/csv'
pause

REM ACDSense - upload one-element disco for demonstrating CSV operations
curl -v -X POST http://localhost:8080/tests/s100r1JSO/disco?format=csv --user -7862164820900576677:dspgtud --data-binary @disco.csv -H 'Content-type:text/csv'
pause

REM ACDSense - get disco set for given test as CSV (written to file testdataset.csv)
curl -v -X GET http://localhost:8080/tests/s100r1JSO/disco?format=csv --user -7862164820900576677:dspgtud
pause

REM ACDSense - get data set for given test as CSV (written to file testdataset.csv)
curl -v -X GET http://localhost:8080/tests/s100r1JSO?format=csv --user -7862164820900576677:dspgtud
pause

REM ACDSense - upload one-element send data package as CSV (only works once with specified id (primary key!); change id for subsequent requests)
curl -v -X POST http://localhost:8080/tests/s100r1JSO/data/send?format=csv --user -7862164820900576677:dspgtud --data-binary @data.csv -H 'Content-type:text/csv; charset=utf-8'
pause

REM ACDSense - upload one-element receive data package as CSV (expects data from file data.csv)
curl -v -X POST http://localhost:8080/tests/s100r1JSO/data/receive?format=csv --user -7862164820900576677:dspgtud --data-binary @data.csv -H 'Content-type:text/csv; charset=utf-8'
pause

REM ACDSense - get data set for given test as CSV (written to file testdataset.csv)
curl -v -X GET http://localhost:8080/tests/s100r1JSO/data?format=csv --user -7862164820900576677:dspgtud > test-dataset.csv
pause

REM ACDSense - create test for demonstrating JSON operations
curl -v -X POST http://localhost:8080/tests?format=json --user -7862164820900576677:dspgtud -H "Content-Type: application/json" -d "{\"id\":\"s10r10WFP\",\"smucs\":10,\"rpersmuc\":10,\"rtype\":\"W\",\"topo\":\"F\",\"stype\":\"P\"}"
pause

REM ACDSense - upload one-element disco for demonstrating JSON operations
curl -v -X POST http://localhost:8080/tests/s100r1JSO/disco?format=json --user -7862164820900576677:dspgtud -H "Content-Type: application/json" -d "[{\"testid\":\"s10r10WFP\",\"cjid\":\"renzel@role.dbis.rwth-aachen.de\",\"time\":1239082345554,\"dur\":1000}]"
pause

REM ACDSense - get disco set for given test as JSON
curl -v -X GET http://localhost:8080/tests/s100r1JSO/disco?format=json --user -7862164820900576677:dspgtud
pause

REM ACDSense - upload one-element send data package as JSON (only works once with specified id (primary key!); change id for subsequent requests)
curl -v -X POST http://localhost:8080/tests/s10r10WFP/data/send?format=json --user -7862164820900576677:dspgtud -H "Content-Type: application/json" -d "[{\"id\":\"1005\",\"time\":\"1239082345554\",\"fromjid\":\"temperature@role.dbis.rwth-aachen.de\",\"pldsize\":103,\"msgsize\":268,\"msg\":\"msg\",\"tojid\":\"office6237@ambience.role.dbis.rwth-aachen.de\/temperature\"}]"
pause

REM ACDSense - upload one-element receive data package as JSON
curl -v -X POST http://localhost:8080/tests/s10r10WFP/data/receive?format=json --user -7862164820900576677:dspgtud -H "Content-Type: application/json" -d "[{\"id\":\"1005\",\"time\":\"1239087038044\",\"fromjid\":\"office6237@ambience.role.dbis.rwth-aachen.de\/temperature\",\"pldsize\":103,\"msgsize\":268,\"msg\":\"msg\",\"tojid\":\"renzel@role.dbis.rwth-aachen.de\"}]"
pause

REM ACDSense - get data set for given test as JSON (written to file testdataset.json)
curl -v -X GET http://localhost:8080/tests/s10r10WFP/data?format=json --user -7862164820900576677:dspgtud > test-dataset.json
pause

REM ACDSense - get full data set of all tests as CSV (written to file dataset.csv)
curl -v -X GET http://localhost:8080/data?format=csv --user -7862164820900576677:dspgtud > dataset.csv
pause

REM ACDSense - get full dataset of all tests as JSON (written to file dataset.json)
curl -v -X GET http://localhost:8080/data?format=json --user -7862164820900576677:dspgtud > dataset.json
pause


