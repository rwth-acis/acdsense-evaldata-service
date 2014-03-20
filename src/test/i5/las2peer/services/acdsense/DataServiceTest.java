package i5.las2peer.services.acdsense;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.webConnector.HttpErrorException;
import i5.las2peer.webConnector.TestClient;
import i5.las2peer.webConnector.WebConnector;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DataServiceTest {

	private static final String HTTP_ADDRESS = "http://127.0.0.1";
	private static final int HTTP_PORT = WebConnector.DEFAULT_HTTP_PORT;

	private static LocalNode node;
	private static WebConnector connector;
	private static ByteArrayOutputStream logStream;

	private static UserAgent testAgent;
	private static final String testPass = "adamspass";
	private static final String testServiceClass = "i5.las2peer.services.acdsense.DataService";

	private static String idprefix; // used for simulating unique XMPP ids
	private static JSONObject mJson; // used for temporary test metadata
	private static String mCsv; // used for temporary test metadata
	private static String mJsonId, mCsvId;
	private static JSONArray dJson, sJson,rJson; // used for temporary evaluation test data
	private static String dCsv, sCsv, rCsv; // used for temporary evaluation test (meta)data

	private static String ddCsv; // for testing duplicate client jids
	private static JSONArray ddJson; // for testing duplicate client jids
	
	private static TestClient c;

	@BeforeClass
	public static void startServer () throws Exception {

		idprefix = "id"+System.currentTimeMillis()+"-";
		// start Node
		node = LocalNode.newNode();
		node.storeAgent(MockAgentFactory.getAdam());

		node.launch();

		ServiceAgent testService = ServiceAgent.generateNewAgent(testServiceClass, "a pass");
		testService.unlockPrivateKey("a pass");

		node.registerReceiver(testService);

		// start connector

		logStream = new ByteArrayOutputStream();

		connector = new WebConnector(true,HTTP_PORT,false,1000, "./XMLCompatibility");
		connector.setSocketTimeout(10000);
		connector.setLogStream(new PrintStream ( logStream));
		connector.setCrossOriginResourceDomain("*");
		connector.setCrossOriginResourceSharing(true);
		connector.setLogStream(new PrintStream ( logStream));

		connector.start ( node );

		// eve is the anonymous agent!
		testAgent = MockAgentFactory.getAdam();

		c = new TestClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		c.setLogin(Long.toString(testAgent.getId()), testPass);


		System.out.println("Agent ID: " + testAgent.getId());
		System.out.println("Agent Password: " + testPass);

		generateTestDataPackages(10);

		/*
		System.out.println("Test Data JSON - send");
		System.out.println(sJson.toJSONString());
		System.out.println("Test Data JSON - receive");
		System.out.println(rJson.toJSONString());
		System.out.println("Test Data JSON - send");
		System.out.println(sCsv);
		System.out.println("Test Data JSON - receive");
		System.out.println(rCsv);
		 */

	}

	@Before
	public void deleteTests(){
		try{
			deleteTest(mJsonId);
		} catch (HttpErrorException e){
			// do nothing
		}

		try{
			deleteTest(mCsvId);
		} catch (HttpErrorException e){
			// do nothing
		}
	}

	@AfterClass
	public static void shutDownServer () throws Exception {

		connector.interrupt();
		connector.stop();

		node.shutDown();

		connector = null;
		node = null;

		LocalNode.reset();

		//System.out.println("Connector-Log:");
		//System.out.println("--------------");

		//System.out.println(logStream.toString());


	}

	@Test
	public void testTestManagementJSON() 
	{
		String testId = mJsonId;

		// create test JSON
		try {
			createTest("json",mJson.toJSONString());
		} catch (HttpErrorException e) {
			fail(e.getMessage());
		}

		// ... then get test meta as JSON to check if test was created successfully
		try
		{
			String result = getTest(testId,"json");
			assertTrue(result.length() > 0);
			Object po = JSONValue.parseWithException(result);
			assertTrue(po instanceof JSONObject);
			JSONObject pjo = (JSONObject) po; 
			System.out.println("Test Meta JSON:\n" + pjo.toJSONString());
		} catch(HttpErrorException e) {
			fail (e.getMessage());
		} catch (ParseException e) {
			fail (e.getMessage());
		}

		// ... then get test meta as CSV to check if test was created successfully
		try
		{
			String result = getTest(testId,"csv");
			assertTrue(result.length() > 0);
			System.out.println("Test Meta CSV:\n" +result);
		} catch(HttpErrorException e) {
			fail (e.getMessage());
		} 

		// ... then delete tests;
		try{
			deleteTest(mJsonId);
		} catch(HttpErrorException e) {
			fail (e.getMessage());
		}

		// ... then get test meta again; should result in 404
		try
		{
			c.sendRequest("GET", "tests/" + mJsonId + "?format=json", "");
			fail("GET on non-existing resource should result in 404!");
		} catch(HttpErrorException e) {
			assertTrue(e.getErrorCode() == 404);	
		}

		// ... then delete tests again; should result in 404
		try{
			deleteTest(mJsonId);
		} catch(HttpErrorException e) {
			assertTrue(e.getErrorCode() == 404);	
		}
	}

	@Test
	public void testNonExistingTestResources(){
		String nonExId = "nonexisting";
		// ... try to store send data for non-existing test; should result in 404
		try
		{
			c.sendRequest("GET", "tests/" + nonExId  + "/data?format=json","");
			fail("GET on non-existing test resource should result in 404!");
		} catch(HttpErrorException e) {
			e.printStackTrace();
			assertTrue(e.getErrorCode() == 404);	
		}

		// ... try to store receive data for non-existing test; should result in 404
		try
		{
			c.sendRequest("POST", "tests/" + nonExId  + "/data/receive?format=json", sJson.toJSONString());
			fail("POST on non-existing test resource should result in 404!");
		} catch(HttpErrorException e) {
			e.printStackTrace();
			assertTrue(e.getErrorCode() == 404);	
		}
	}

	@Test
	public void testDuplicateTest(){

		// create test JSON
		try {
			createTest("json",mJson.toJSONString());
		} catch (HttpErrorException e) {
			fail(e.getMessage());
		}

		// ... then create test again (should result in conflict 409)
		try {
			createTest("json",mJson.toJSONString());
			fail("Attempt to create existing test should result in conflict (409)");
		} catch (HttpErrorException e) {
			assertTrue(e.getErrorCode() == 409);
		}

		// ... then delete test;
		try{
			deleteTest(mJsonId);
		} catch(HttpErrorException e) {
			fail (e.getMessage());
		}
	}
	
	@Test
	public void testDuplicateDiscoCjid(){
		String testId = mJsonId;

		// create test JSON
		try {
			createTest("json",mJson.toJSONString());
		} catch (HttpErrorException e) {
			fail(e.getMessage());
		}

		// then add disco measurement results with duplicate client jids
		try {
			addTestDisco(testId, "json", ddJson.toJSONString());
			fail("Adding disco data containing duplicate client jids should result in conflict!");
		} catch (HttpErrorException e) {
			assertTrue(e.getErrorCode() == 409);
		}

		// ... then delete tests;
		try{
			deleteTest(testId);
		} catch(HttpErrorException e) {
			fail (e.getMessage());
		}
	}

	@Test
	public void testDuplicateDataItems(){
		String testId = mJsonId;

		// create test JSON
		try {
			createTest("json",mJson.toJSONString());
		} catch (HttpErrorException e) {
			fail(e.getMessage());
		}

		// ... then store send data...
		try
		{
			c.sendRequest("POST", "tests/" + testId + "/data/send?format=json", sJson.toJSONString());
		} catch(HttpErrorException e) {
			fail (e.getMessage());
		}

		// ...then store send data again; should result in conflict (409)
		try
		{
			c.sendRequest("POST", "tests/" + testId + "/data/send?format=json", rJson.toJSONString());
			fail("Attempt to create existing send data item should result in conflict (409)");
		} catch(HttpErrorException e) {
			assertTrue(e.getErrorCode() == 409);
		}

		// ... then delete test;
		try{
			deleteTest(testId);
		} catch(HttpErrorException e) {
			fail (e.getMessage());
		}
	}

	@Test
	public void testStoreDataJson(){

		String testId = mJsonId;

		// create test JSON
		try {
			createTest("json",mJson.toJSONString());
		} catch (HttpErrorException e) {
			fail(e.getMessage());
		}

		// ... then store send data...
		try
		{
			c.sendRequest("POST", "tests/" + testId + "/data/send?format=json", sJson.toJSONString());
		} catch(HttpErrorException e) {
			fail (e.getMessage());
		}

		// ...then store receive data
		try
		{
			c.sendRequest("POST", "tests/" + testId + "/data/receive?format=json", rJson.toJSONString());
		} catch(HttpErrorException e) {
			fail (e.getMessage());
		}

		// ...then get data for test
		try
		{
			String result = c.sendRequest("GET", "tests/"+testId + "/data?format=json", "");
			assertTrue(result.length() > 0);
			Object po = JSONValue.parseWithException(result);
			assertTrue(po instanceof JSONArray);
			JSONArray pja = (JSONArray) po; 
			//System.out.println("Data for test " + testId + ": " + pja.toJSONString());
		} catch(HttpErrorException e) {
			fail (e.getMessage());
		} catch (ParseException e) {
			fail (e.getMessage());
		}

		// ...then get all data
		try
		{
			String result = c.sendRequest("GET", "data?format=json", "");
			assertTrue(result.length() > 0);
			Object po = JSONValue.parseWithException(result);
			assertTrue(po instanceof JSONArray);
			JSONArray pja = (JSONArray) po; 
			//System.out.println("Complete data set: " + pja.toJSONString());
		} catch(HttpErrorException e) {
			fail (e.getMessage());
		} catch (ParseException e) {
			fail (e.getMessage());
		}

		// ... then delete test;
		try{
			deleteTest(mJsonId);
		} catch(HttpErrorException e) {
			fail (e.getMessage());
		}
	}

	@Test
	public void testStoreDataCSV(){

		// create test JSON
		try {
			createTest("csv",mCsv);
		} catch (HttpErrorException e) {
			fail(e.getMessage());
		}

		String testId = mCsvId;

		// ... then store send data...
		try
		{
			c.sendRequest("POST", "tests/" + testId + "/data/send?format=csv", sCsv);
		} catch(HttpErrorException e) {
			fail (e.getMessage());
		}

		// ...then store receive data
		try
		{
			c.sendRequest("POST", "tests/" + testId + "/data/receive?format=csv", rCsv);
		} catch(HttpErrorException e) {
			fail (e.getMessage());
		}

		// ...then get data for test
		try
		{
			String result = c.sendRequest("GET", "tests/"+testId + "/data?format=csv", "");
			assertTrue(result.length() > 0);
			//System.out.println("Data for test " + testId + ": " + result);
		} catch(HttpErrorException e) {
			fail (e.getMessage());
		}

		// ...then get all data
		try
		{
			String result = c.sendRequest("GET", "data?format=csv", "");
			assertTrue(result.length() > 0);
			//System.out.println("Complete data set: " + result);
		} catch(HttpErrorException e) {
			fail (e.getMessage());
		}

		// ... then delete tests;
		try{
			deleteTest(mCsvId);
		} catch(HttpErrorException e) {
			fail (e.getMessage());
		}
	}

	@Test
	public void testStoreDiscoJSON(){
		String testId = mJsonId;

		// create test JSON
		try {
			createTest("json",mJson.toJSONString());
		} catch (HttpErrorException e) {
			fail(e.getMessage());
		}

		// then add disco measurement results
		try {
			//System.out.println(dJson.toJSONString());
			addTestDisco(testId, "json", dJson.toJSONString());
		} catch (HttpErrorException e) {
			fail(e.getMessage());
		}

		// then get disco 
		try {
			String result = getTestDisco(testId, "json");
			//System.out.println(result);
		} catch (HttpErrorException e) {
			fail(e.getMessage());
		}

		// ... then delete tests;
		try{
			deleteTest(testId);
		} catch(HttpErrorException e) {
			fail (e.getMessage());
		}
	}

	@Test
	public void testStoreDiscoCSV(){
		String testId = mCsvId;

		// create test JSON
		try {
			createTest("csv",mCsv);
		} catch (HttpErrorException e) {
			fail(e.getMessage());
		}

		// then add disco measurement results
		try {
			//System.out.println(dJson.toJSONString());
			addTestDisco(testId, "csv", dCsv);
		} catch (HttpErrorException e) {
			fail(e.getMessage());
		}

		// then get disco 
		try {
			String result = getTestDisco(testId, "csv");
			//System.out.println(result);
		} catch (HttpErrorException e) {
			fail(e.getMessage());
		}

		// ... then delete tests;
		try{
			deleteTest(testId);
		} catch(HttpErrorException e) {
			fail (e.getMessage());
		}
	}

	private static void createTest(String format, String meta) throws HttpErrorException {
		c.sendRequest("POST", "tests?format="+format, meta);
	}

	private static void addTestDisco(String testId, String format, String disco) throws HttpErrorException {
		c.sendRequest("POST", "tests/"+testId+"/disco?format="+format, disco);
	}

	private static void deleteTest(String id) throws HttpErrorException {
		c.sendRequest("DELETE", "tests/"+id,"");
	}

	private static String getTest(String id, String format) throws HttpErrorException{
		String result = c.sendRequest("GET", "tests/" + id + "?format="+format, "");
		return result;
	}

	private static String getTestDisco(String testId, String format) throws HttpErrorException{
		String result = c.sendRequest("GET", "tests/" + testId + "/disco?format="+format, "");
		return result;
	}

	@SuppressWarnings("unchecked")
	private static void generateTestDataPackages(int elements) throws UnsupportedEncodingException{

		mJsonId = "s1000r10WFP";
		mCsvId = "s100r1JSO";

		mJson = generateTestMetaJSON(mJsonId);
		mCsv = generateTestMetaCSV(mCsvId);

		sJson = new JSONArray();
		sCsv = "id;time;fromjid;tojid;msg;msgsize;pldsize\n";

		rJson = new JSONArray();
		rCsv = "id;time;fromjid;tojid;msg;msgsize;pldsize\n";

		dJson = new JSONArray();
		ddJson = new JSONArray();
		
		dCsv = "cjid;time;dur\n";
		ddCsv =  "cjid;time;dur\n";

		String csvItem1 = generateTestDiscoItemCSV(mCsvId, "klamma@role.dbis.rwth-aachen.de");
		String csvItem2 = generateTestDiscoItemCSV(mCsvId, "renzel@role.dbis.rwth-aachen.de");
		String csvItem3 = generateTestDiscoItemCSV(mCsvId, "koren@role.dbis.rwth-aachen.de");
		
		dCsv += csvItem1 + csvItem2 + csvItem3;
		ddCsv = csvItem1 + csvItem1; // duplicate client jids
		
		JSONObject jsonItem1 = generateTestDiscoItemJSON(mJsonId, "klamma@role.dbis.rwth-aachen.de");
		JSONObject jsonItem2 = generateTestDiscoItemJSON(mJsonId, "renzel@role.dbis.rwth-aachen.de");
		JSONObject jsonItem3 = generateTestDiscoItemJSON(mJsonId, "koren@role.dbis.rwth-aachen.de");
		
		dJson.add(jsonItem1);
		dJson.add(jsonItem2);
		dJson.add(jsonItem3);
		
		ddJson.add(jsonItem1);
		ddJson.add(jsonItem1);
		
		for(int i=0;i<elements;i++){
			String id = idprefix+i;

			String sendCsv = generateTestDataItemCSV(id+"csv", true);
			sCsv += sendCsv;

			String receiveCsv = generateTestDataItemCSV(id+"csv", false);
			rCsv += receiveCsv;

			JSONObject sendJson = generateTestDataItemJSON(id+"json", true);
			sJson.add(sendJson);

			JSONObject receiveJson = generateTestDataItemJSON(id+"json", false);
			rJson.add(receiveJson);

		}

	}

	@SuppressWarnings("unchecked")
	private static JSONObject generateTestDataItemJSON(String id,boolean send) throws UnsupportedEncodingException{

		long time = System.currentTimeMillis();
		String roomjid = "office6237@ambience.role.dbis.rwth-aachen.de";
		String sensorroomnick = roomjid + "/temperature";
		String sensorjid = "temperature@role.dbis.rwth-aachen.de";
		String userjid = "renzel@role.dbis.rwth-aachen.de";
		String payload = "{\"sensorevent\":{\"type\":\"AMBIENT_TEMPERATURE\",\"values\":[18.69],\"timestamp\":\"2014-03-05T18:27:23+01:00\"}}";
		String msg = "<message to='renzel@role.dbis.rwth-aachen.de/home' type='groupchat' lang='en' from='office6237@ambience.role.dbis.rwth-aachen.de/temperature'><body>" + payload + "</body></message>";
		int payloadSize = (payload.getBytes("UTF-8")).length; // assume UTF-8 encoding, size in bytes
		int msgSize = (msg.getBytes("UTF-8")).length; // assume UTF-8 encoding, size in bytes

		// default: receive case (user receives sensor data from MUC room)
		String fromjid = sensorjid;
		String tojid = userjid;

		JSONObject obj=new JSONObject();

		if(send){
			fromjid = sensorjid;
			tojid = sensorroomnick;
		} else {
			fromjid = sensorroomnick;
			tojid = userjid;
		}

		obj.put("id",id); // standard XMPP stanza attribute "id"; used for merging send and receive data 
		obj.put("time",time); // system time of stanza processing completion on receiver client machine
		obj.put("fromjid",fromjid); // standard XMPP stanza attribute "from" (sensor JID)
		obj.put("tojid",tojid); // standard XMPP stanza attribute "to" (agent JID)
		obj.put("msg",msg); 
		obj.put("msgsize",msgSize);
		obj.put("pldsize",payloadSize);

		return obj;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject generateTestDiscoItemJSON(String testId, String cjid) throws UnsupportedEncodingException{

		Long time = System.currentTimeMillis();
		Long dur = 10000l;

		JSONObject obj=new JSONObject();

		obj.put("cjid",cjid); // client jid
		obj.put("time",time); // time of starting sensor discovery
		obj.put("dur",dur); // duration of sensor discovery

		return obj;
	}

	private static String generateTestDataItemCSV(String id,boolean send) throws UnsupportedEncodingException{
		long time = System.currentTimeMillis();
		String roomjid = "office6237@ambience.role.dbis.rwth-aachen.de";
		String sensorroomnick = roomjid + "/temperature";
		String sensorjid = "temperature@role.dbis.rwth-aachen.de";
		String userjid = "renzel@role.dbis.rwth-aachen.de";
		String payload = "{\"sensorevent\":{\"type\":\"AMBIENT_TEMPERATURE\",\"values\":[18.69],\"timestamp\":\"2014-03-05T18:27:23+01:00\"}}";
		String msg = "<message to='renzel@role.dbis.rwth-aachen.de/home' type='groupchat' lang='en' from='office6237@ambience.role.dbis.rwth-aachen.de/temperature'><body>" + payload + "</body></message>";
		int pldSize = (payload.getBytes("UTF-8")).length; // assume UTF-8 encoding, size in bytes
		int msgSize = (msg.getBytes("UTF-8")).length; // assume UTF-8 encoding, size in bytes

		// default: receive case (user receives sensor data from MUC room)
		String fromjid = sensorjid;
		String tojid = userjid;

		String result = "";
		if(send){
			fromjid = sensorjid;
			tojid = sensorroomnick;
		} else {
			fromjid = sensorroomnick;
			tojid = userjid;
		}

		result += id + ";" + time + ";" + fromjid + ";" + tojid + ";" + msg + ";" + msgSize + ";" + pldSize + "\n"; 
		return result;
	}

	private static String generateTestDiscoItemCSV(String testId, String cjid) throws UnsupportedEncodingException{
		Long time = System.currentTimeMillis();
		Long dur = 10000l;

		String result = cjid + ";" + time + ";" + dur + "\n"; 
		return result;
	}

	private static String generateTestMetaCSV(String id){

		String header = "id;smucs;rpersmuc;rtype;topo;stype\n";

		int smucs = 100;
		int rpersmuc = 1;
		String rtype = "J";
		String topo = "S";
		String stype = "O";

		return header + 
				id + ";" + smucs + ";" + rpersmuc + ";" + rtype + ";" + topo + ";" + stype + "\n"; 
	}

	@SuppressWarnings("unchecked")
	private static JSONObject generateTestMetaJSON(String id){

		int smucs = 1000;
		int rpersmuc = 10;
		String rtype = "W";
		String topo = "F";
		String stype = "P";

		JSONObject obj=new JSONObject();
		obj.put("id",id); 
		obj.put("smucs",smucs);
		obj.put("rpersmuc",rpersmuc);
		obj.put("rtype", rtype);
		obj.put("topo", topo); 
		obj.put("stype", stype);

		return obj;
	}

}
