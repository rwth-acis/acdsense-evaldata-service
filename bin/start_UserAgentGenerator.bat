cd %~dp0
cd ..
set BASE=%CD%
set CLASSPATH="%BASE%/lib/*"

java -cp %CLASSPATH% i5.las2peer.tools.UserAgentGenerator 8ang-04 "acdsenserwth" acdsense@dbis.rwth-aachen.de
pause
