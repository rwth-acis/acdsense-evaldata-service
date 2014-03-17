ACDSense Evaluation Data Service REST API
==

The ACDSense Evaluation Data Service is a simple RESTful Web service to store and retrieve ACDSense evaluation data. The service organizes evaluation data in tests, where each test manages test metadata, discovery data and send/receive data. 
In the following we describe the conceptual data model and all resources in detail, including normative examples.

Conceptual Data Model
--

__Test Metadata__

Each test defines the following metadata:

|attribute | desription | example|
|--- | --- | --- |
|*id* | test identifier | `s10r100JSO` |
|*smucs* | number of sensor mucs involved in test | `10` |
|*rpersmuc* |  number of receivers per sensor muc | `100` |
|*rtype* | type of receiver being one of `J` (Java native XMPP), `B` (Web app XMPP over BOSH), `W` (Web app XMPP over WebSocket| `J`|
|*topo* | test network topology being one of `S` (single-server) or `F` (federated)| `S` |
|*stype* | server type being one of `O` (Openfire), `E` (ejabberd), `P` (Prosody) | `O` |
---

__Test Discovery Data__

Each test discovery data item contains the following information:

|attribute | desription | example|
|--- | --- | --- |
|*testid* | test identifier | `s10r100JSO` |
|*cjid* | number of sensor mucs involved in test | `10` |
|*time* | timepoint of starting sensor MUC discovery in milliseconds since Jan 1, 1970 | `1394048559684007` |
|*dur* | duration of full sensor MUC discovery at client in milliseconds | `1000`|
---

__Test Send/Receive Data__

Both send and receive data items contain the following information:

|attribute | desription | example|
|--- | --- | --- |
|*testid* | test identifier | `s10r1JSO` |
|*id* | message identifier; standard XMPP stanza attribute *'id'* | `id007` |
|*time* | timepoint of sending/receiving message stanza in milliseconds since Jan 1, 1970 | `1394048559684007` |
|*fromjid* | message sender JID; standard XMPP stanza attribute *'from'* | `renzel@role.dbis.rwth-aachen.de` |
|*tojid* | message receiver JID; standard XMPP stanza attribute *'to'* | `renzel@role.dbis.rwth-aachen.de` |
|*msg* | complete message including all protocol overhead | (native XMPP:stanza; XMPP over BOSH: HTTP request/response; XMPP over WebSocket: data frame)|
|*msgsize* | complete message size in number of bytes | `268`| 
|*pldsize* | payload size in number of bytes | `103`|
---

Authentication
--
All resources of the ACDSense Evaluation Data Service described in the following he use the HTTP Basic Authentication scheme (cf. RFC 2617).

Tests Resource
--
__URI: /tests__

>__POST__ - create new test by specifying test metadata.
>> Query Parameters
>>> * format - specify metadata format as `csv` or `json`.

>> Example 1 (CSV)
>>
```javascript
POST /tests?format=json 
id;smucs;rpersmuc;rtype;topo;stype
s100r10JSO;100;10;J;S;O
```
>> Example 2 (JSON)
>>
```javascript
POST /tests?format=json
{
        "id":"s100r10JSO",
        "smucs":100,
        "rpersmuc": 10,
        "rtype":"J"
        "topo":"S",
        "stype":"O"
}
```

Test Resource
--
__URI: /tests/{id}__
>Path Parameters
>> * id: test identifier

>__GET__ - get test metadata in CSV or JSON.
>> Query Parameters
>>> * format - specify data format as `csv` or `json`.

>> Example 1 (CSV)
>>
```javascript
GET /tests/s100r10JSO?format=csv
id;smucs;rpersmuc;rtype;topo;stype
s100r10JSO;100;10;J;S;O
```
>> Example 2 (JSON)
>>
```javascript
GET /tests/s100r10JSO?format=json
{
        "id":"s100r10JSO",
        "smucs":100,
        "rpersmuc": 10,
        "rtype":"J"
        "topo":"S",
        "stype":"O"
}
```

>__DELETE__ - delete test including all contained metadata, discovery and send/receive data. __Use with caution!__
>> Example
>>
```javascript
DELETE /tests/s100r10JSO
```

Test Discovery Resource
--
__URI: /tests/{id}/disco__
>Path Parameters
>> * id: test identifier

>__POST__ - store test discovery data in CSV or JSON. All client JIDs must be unique within test scope.
>> Query Parameters
>>> * format - specify data format as `csv` or `json`.

>> Example 1 (CSV)
>>
```javascript
POST /tests/s100r10JSO/disco?format=json 
cjid;time;dur
renzel@role.dbis.rwth-aachen.de;1394105593800;100
koren@role.dbis.rwth-aachen.de;1394105594533;123
```
>> Example 2 (JSON)
>>
```javascript
POST /tests/s100r10JSO/disco?format=json
[
    {
        "cjid":"renzel@role.dbis.rwth-aachen.de",
        "time":1394105593800,
        "dur": 100,
    },
    {
        "cjid":"koren@role.dbis.rwth-aachen.de",
        "time":1394105593800,
        "dur": 100,
    }
]
```

>__GET__ - retrieve test discovery data in CSV or JSON. All client JIDs must be unique within test scope.
>> Query Parameters
>>> * format - specify data format as `csv` or `json`.

>> Example 1 (CSV)
>>
```javascript
GET /tests/s100r10JSO/disco?format=json 
cjid;time;dur
renzel@role.dbis.rwth-aachen.de;1394105593800;100
koren@role.dbis.rwth-aachen.de;1394105594533;123
```
>> Example 2 (JSON)
>>
```javascript
GET /tests/s100r10JSO/disco?format=json
[
    {
        "cjid":"renzel@role.dbis.rwth-aachen.de",
        "time":1394105593800,
        "dur": 100,
    },
    {
        "cjid":"koren@role.dbis.rwth-aachen.de",
        "time":1394105593800,
        "dur": 100,
    }
]
```

Test Data Resource
--
__URI: /tests/{id}/data__
>Path Parameters
>> * id: test identifier

>__GET__ - get joined send/receive data set for test in CSV or JSON
>> Query Parameters
>>> * format - specify data format as `csv` or `json`.

>> Example 1 (CSV)
>>
```javascript
GET /tests/s100r10JSO/data?format=csv
testid,id;sendtime;sendfromjid;sendtojid;sendmsg;sendmsgsize;sendpldsize;receivetime;receivefromjid;receivetojid;receivemsg;receivemsgsize;receivepldsize
s100r10JSO;id1394105591394-0csv;1394105593800;temperature@role.dbis.rwth-aachen.de;office6237@ambience.role.dbis.rwth-aachen.de/temperature;<MESSAGE_CONTENT>;268;103;1394105593900;office6237@ambience.role.dbis.rwth-aachen.de/temperature;renzel@role.dbis.rwth-aachen.de;<MESSAGE_CONTENT>;268;103
... (further data rows)
```
>> Example 2 (JSON)
>>
```javascript
GET /tests/s100r10JSO/data?format=json
[
   {
       "testid":"s100r10JSO", 
       "id":"id1394105591394-0csv",
       "sendtime":1394105593800,
       "sendfromjid":"temperature@role.dbis.rwth-aachen.de",
       "sendtojid":"office6237@ambience.role.dbis.rwth-aachen.de\/temperature",
       "sendmsg":"<MESSAGE_CONTENT>",
       "sendmsgsize":268,
       "sendpldsize":103,
       "receivetime":1394105593900,
       "receivefromjid":"office6237@ambience.role.dbis.rwth-aachen.de\/temperature",
       "receivetojid":"renzel@role.dbis.rwth-aachen.de",
       "receivemsg":"<MESSAGE_CONTENT>",
       "receivemsgsize":268,
       "receivepldsize":103
   },
    ... (further data items)
]
```

Test Send Data Resource
--
__URI: /tests/{id}/data/send__
>Path Parameters
>> * id: test identifier

>__POST__ - store test send data package encoded in CSV or JSON.
>> Query Parameters
>>> * format - specify data format as `csv` or `json`.

>> Example 1 (CSV)
>>
```javascript
POST /tests/s100r10JSO/data/send?format=json
id;time;fromjid;tojid;msg;msgsize;pldsize
id1394048557414-0csv;1394048559683007;temperature@role.dbis.rwth-aachen.de;office6237@ambience.role.dbis.rwth-aachen.de/temperature;<MESSAGE_CONTENT>;268;103
... (further data rows)
```
>> Example 2 (JSON)
>>
```javascript
POST /tests/s100r10JSO/data/send?format=json
[
    {
        "id":"id1394048557414-0json",
        "time":1394048559684890,
        "fromjid":"temperature@role.dbis.rwth-aachen.de",
        "tojid":"office6237@ambience.role.dbis.rwth-aachen.de\/temperature",
        "msg":"<MESSAGE_CONTENT>",
        "pldsize":103,
        "msgsize":268
    },
    ... (further data items)
]
```

Test Send Receive Resource
--
__URI: /tests/{id}/data/receive__
>Path Parameters
>> * id: test identifier

>__POST__ - store receive data package encoded in CSV or JSON.
>> Query Parameters
>>> * format - specify data format as `csv` or `json`.

>> Example 1 (CSV)
>>
```javascript
POST /tests/s100r10JSO/data/receive?format=json 
id;time;fromjid;tojid;msg;msgsize;pldsize
id1394048557414-0csv;1394048559683045;office6237@ambience.role.dbis.rwth-aachen.de/temperature;renzel@role.dbis.rwth-aachen.de;<MESSAGE_CONTENT>;268;103
... (further data rows)
```
>> Example 2 (JSON)
>>
```javascript
POST /tests/s100r10JSO/data/receive?format=json
[
    {
        "id":"id1394048557414-0json",
        "time":1394048560000321,
        "fromjid":"office6237@ambience.role.dbis.rwth-aachen.de\/temperature",
        "tojid":"renzel@role.dbis.rwth-aachen.de"
        "msg":"<MESSAGECONTENT>",
        "pldsize":103,
        "msgsize":268
    },
    ... (further data items)
]
```

Send/Receive Dataset Resource
--
__URI: /data__

>__GET__ - get full send/receive data set in CSV or JSON
>> Query Parameters
>>> * format - specify data format as `csv` or `json`.

Discovery Dataset Resource
--
__URI: /disco__

>__GET__ - get full discovery data set in CSV or JSON
>> Query Parameters
>>> * format - specify data format as `csv` or `json`.