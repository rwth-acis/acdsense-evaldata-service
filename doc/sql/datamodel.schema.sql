-- DATABASE SCHEMA DEFINITION FOR ACDSENSE EVALUATION SCENARIO

-- -----------------------------------------
-- Create User acdsense and grant privileges
-- -----------------------------------------
-- create user acdsense identified by 'acdsense'; 

-- grant usage on *.* to acdsense@localhost identified by 'dito'; 
-- grant all privileges on acdsense.* to acdsense@localhost; 

drop schema if exists acdsense;
create schema if not exists acdsense default character set utf8 collate utf8_general_ci;
use acdsense;

drop view if exists dataset;
drop table if exists receive;
drop table if exists send;
drop table if exists testmeta;

-- -----------------------------------------------------
-- Definition table testmeta
--
-- - id - Unique identifier
-- - smucs - Number of sensor MUCs
-- - rspersmuc - Number of receivers per sensor MUC
-- - rtype - Receiver type ( N - native XMPP, B - Web app via JavaScript and XMPP over BOSH, W - Web App via JavaScript and XMPP over WebSocket)
-- - topo - Topology of test network (S - single server, F - federated)
-- - stype - Type of server receiver is connected to (O - Openfire, P - Prosody, E - ejabberd)
-- -----------------------------------------------------
create table testmeta (
	id varchar(128) not null,
	smucs int not null,
	rpersmuc int not null,
	rtype char(1) not null,
	topo char(1) not null,
	stype char(1) not null,
	primary key (id)
);

-- -----------------------------------------------------
-- Definition table testmeta
--
-- - testid - Unique test identifier (cf. table testmeta)
-- - cjid - Client JID
-- - time - time of sending disco query in milliseconds since Jan 1, 1970
-- - dur - duration of processing disco query
-- -----------------------------------------------------
create table disco (
	testid varchar(128) not null,
	cjid varchar(128) not null,
	time long not null,
	dur long not null,
	unique key (cjid),
	constraint disco_fk
		foreign key (testid)
		references testmeta(id)
		on delete cascade
);

-- -----------------------------------------------------
-- Definition table send
--
-- id - message identifier; standard XMPP stanza attribute 'id' (example: id007)
-- time - fractional UNIX timestamp encoding time of sending/receiving message stanza in millisecond precision (example: 1394048559684.007)
-- fromjid - message sender JID; standard XMPP stanza attribute 'from' (example: renzel@role.dbis.rwth-aachen.de)
-- tojid - message receiver JID; standard XMPP stanza attribute 'to' (example: renzel@role.dbis.rwth-aachen.de)
-- msg - complete message including all overhead (native XMPP:stanza; XMPP over BOSH: HTTP request/response; XMPP over WebSocket: data frame)
-- msgsize - complete message size in number of bytes (example: 268)
-- pldsize - payload size in number of bytes (103)
-- -----------------------------------------------------
create table send (
	testid varchar(128) not null,
	id varchar(128) not null,
	time long not null,
	fromjid varchar(256) not null,
	tojid varchar(256) not null,
	msg text not null,
	msgsize int not null,
	pldsize int not null,
	primary key (id),
	constraint testmeta_fk
		foreign key (testid)
		references testmeta(id)
		on delete cascade
);

-- -----------------------------------------------------
-- Definition table receive
--
-- (see table send for attribute descriptions)
-- -----------------------------------------------------
create table receive (
	id varchar(128) not null,
	time long not null,
	fromjid varchar(256) not null,
	tojid varchar(256) not null,
	msg text,
	msgsize int not null,
	pldsize int not null,
	constraint sendfk
		foreign key (id)
		references send (id)
		on delete cascade
);

-- ----------------------------------------------------
-- Definition
-- -------------------------------------------------------
-- Definition view 'dataset'
-- 
-- convenience view returning the complete dataset as a join of tables 'send' and 'receive'.
-- Join attribute is the message id, which allows to match sent and received stanzas.
-- -------------------------------------------------------
create view acdsense.dataset as
select s.testid, s.id, s.time as send_time, s.fromjid as send_fromjid, s.tojid as send_tojid, s.msg as send_msg, s.msgsize as send_msgsize, s.pldsize as send_pldsize, 
r.time as receive_time, r.fromjid as receive_fromjid, r.tojid as receive_tojid, r.msg as receive_msg, r.msgsize as receive_msgsize, r.pldsize as receive_pldsize 
from acdsense.send as s join acdsense.receive as r on (s.id = r.id);