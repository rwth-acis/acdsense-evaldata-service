cd ..
set BASE=%CD%
set CLASSPATH="%BASE%/lib/*;"

java -cp %CLASSPATH% i5.las2peer.tools.L2pNodeLauncher -s 9001 - uploadStartupDirectory startService('i5.las2peer.services.acdsense.DataService','SampleServicePass') startConnector('i5.las2peer.webConnector.WebConnector','8080') interactive

pause
