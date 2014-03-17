package i5.las2peer.services.acdsense;

import i5.las2peer.api.Service;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.ContentParam;
import i5.las2peer.restMapper.annotations.DELETE;
import i5.las2peer.restMapper.annotations.GET;
import i5.las2peer.restMapper.annotations.POST;
import i5.las2peer.restMapper.annotations.PUT;
import i5.las2peer.restMapper.annotations.Path;
import i5.las2peer.restMapper.annotations.PathParam;
import i5.las2peer.restMapper.annotations.QueryParam;
import i5.las2peer.restMapper.annotations.Version;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

@Version("0.1")
public class DataService extends Service {

	private Connection connection;
	private PreparedStatement sendDataInsertStatement, receiveDataInsertStatement, testMetaInsertStatement, testDiscoInsertStatement;
	private PreparedStatement getDataQueryStatement, getTestDataQueryStatement, getTestMetaQueryStatement, getTestDiscoQueryStatement;
	private PreparedStatement deleteTestMetaStatement;

	private String jdbcDriverClassName;
	private String jdbcUrl, jdbcSchema;
	private String jdbcLogin, jdbcPass;

	public DataService(){
		// set values from configuration file
		this.setFieldValues();

		this.monitor = true;

		try {
			initDatabaseConnection();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@GET
	@Path("/info")
	public HttpResponse getInfo(){
		return new HttpResponse("ACDSense Evaluation Data Collection Service",200);
	}

	@GET
	@Path("/auth")
	public HttpResponse auth(){
		return new HttpResponse("authenticated",200);
	}

	@POST
	@Path("/tests")
	public HttpResponse createTest(@ContentParam String c, @QueryParam(value="format",defaultValue="csv") String format){
		try{
			
			if("json".equals(format)){
				
				storeTestMetaJSON(c);
			} else {
				storeTestMetaCSV(c);
			}

			HttpResponse response = new HttpResponse("",200);
			return response;

		} catch (Exception e){
			e.printStackTrace();
			if(e.getMessage().startsWith("Duplicate")){
				String message = e.getMessage().substring(e.getMessage().indexOf("'")+1,e.getMessage().lastIndexOf("' for key"));
				return new HttpResponse("Test " + message + "already exists!",409);
			}
			return new HttpResponse(e.getMessage(),400);
		}
	}

	@GET
	@Path("/tests/{id}")
	public HttpResponse getTest(@PathParam("id") String id, @QueryParam(value="format",defaultValue="csv") String format){
		String result = "";
		String cType = "text/plain";

		try{
			
			if(!existsTest(id)){
				return new HttpResponse("Test " + id + " does not exist!",404);
			}
			
			getTestMetaQueryStatement.clearParameters();
			getTestMetaQueryStatement.setString(1, id);

			ResultSet rs = getTestMetaQueryStatement.executeQuery();

			if("json".equals(format)){
				JSONObject o = getTestMetaJSON(rs);
				cType = "application/json";
				result = o.toJSONString();
			} else {
				result = getTestMetaCSV(rs);
				cType = "text/csv";
			}

			HttpResponse response = new HttpResponse(result,200);
			response.setHeader("Content-type", cType);
			return response;

		} catch(Exception e){
			e.printStackTrace();
			return new HttpResponse(e.getMessage(),400);
		}
	}

	@GET
	@Path("/tests/{id}/data")
	public HttpResponse getTestData(@PathParam("id") String id, @QueryParam(value="format",defaultValue="csv") String format){

		String result = "";
		String cType = "text/plain";

		try{
			
			if(!existsTest(id)){
				return new HttpResponse("Test " + id + " does not exist!",404);
			}
			getTestDataQueryStatement.clearParameters();
			getTestDataQueryStatement.setString(1, id);
			ResultSet rs = getTestDataQueryStatement.executeQuery();
			if("json".equals(format)){
				result = getDataJSON(rs);
				cType = "application/json";
			} else {
				result = getDataCSV(rs);
				cType = "text/csv";
			}

			HttpResponse response = new HttpResponse(result,200);
			response.setHeader("Content-type", cType);
			return response;

		} catch(Exception e){
			e.printStackTrace();
			return new HttpResponse(e.getMessage(),400);
		}
	}

	@DELETE
	@Path("/tests/{id}")
	public HttpResponse deleteTest(@PathParam("id") String id){
		try {
			
			deleteTestMetaStatement.clearParameters();
			deleteTestMetaStatement.setString(1, id);
			int i = deleteTestMetaStatement.executeUpdate();
			connection.commit();
			if(i==1){
				return new HttpResponse("",200);
			} else {
				return new HttpResponse("",404);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new HttpResponse(e.getMessage(),400);
		}
	}

	@GET
	@Path("/data")
	public HttpResponse getData(@QueryParam(value="format",defaultValue="csv") String format){

		String result = "";
		String cType = "text/plain";

		try{
			ResultSet rs = getDataQueryStatement.executeQuery();
			if("json".equals(format)){
				result = getDataJSON(rs);
				cType = "application/json";
			} else {
				result = getDataCSV(rs);
				cType = "text/csv";
			}

			HttpResponse response = new HttpResponse(result,200);
			response.setHeader("Content-type", cType);
			return response;

		} catch(Exception e){
			e.printStackTrace();
			return new HttpResponse(e.getMessage(),400);
		}
	}

	@POST
	@Path("/tests/{id}/data/send")
	public HttpResponse storeSendDataPackage(@PathParam("id") String testId, @ContentParam String c, @QueryParam(value="format",defaultValue="csv") String format){

		try{
			
			if(!existsTest(testId)){
				return new HttpResponse("Test " + testId + " does not exist!",404);
			}
			
			if("json".equals(format)){
				storeDataPackageJSON(testId,c,true);
			} else {
				storeDataPackageCSV(testId,c,true);
			}

			HttpResponse response = new HttpResponse("",200);
			return response;

		} catch (Exception e){
			if(e.getMessage().startsWith("Duplicate")){
				String message = e.getMessage().substring(e.getMessage().indexOf("'")+1,e.getMessage().lastIndexOf("' for key"));
				return new HttpResponse("Send data item " + message + "already exists!",409);
			} else {
				e.printStackTrace();
				return new HttpResponse(e.getMessage(),400);
			}
		}

	}

	@POST
	@Path("/tests/{id}/data/receive")
	public HttpResponse storeReceiveDataPackage(@PathParam("id") String testId, @ContentParam String c, @QueryParam(value="format",defaultValue="csv") String format){

		try{
			
			if(!existsTest(testId)){
				return new HttpResponse("Test " + testId + " does not exist!",404);
			}
			
			if("json".equals(format)){
				storeDataPackageJSON(testId,c,false);
			} else {
				storeDataPackageCSV(testId,c,false);
			}
			HttpResponse response = new HttpResponse("",200);
			return response;

		} catch (Exception e){
			if(e.getMessage().startsWith("Duplicate")){
				String itemId = e.getMessage().substring(e.getMessage().indexOf("'")+1,e.getMessage().lastIndexOf("' for key"));
				return new HttpResponse("Receive data item " + itemId + "already exists!",409);
			} else {
				e.printStackTrace();
				return new HttpResponse(e.getMessage(),400);
			}
		}
	}

	@POST
	@Path("/tests/{id}/disco")
	public HttpResponse storeTestDisco(@PathParam("id") String testId, @ContentParam String c, @QueryParam(value="format",defaultValue="csv") String format){

		try{
			
			if(!existsTest(testId)){
				return new HttpResponse("Test " + testId + " does not exist!",404);
			}
			
			if("json".equals(format)){
				storeTestDiscoJSON(c);
			} else {
				storeTestDiscoCSV(c);
			}
			HttpResponse response = new HttpResponse("",200);
			return response;

		} catch (Exception e){
			if(e.getMessage().startsWith("Duplicate")){
				String itemId = e.getMessage().substring(e.getMessage().indexOf("'")+1,e.getMessage().lastIndexOf("' for key"));
				return new HttpResponse("Duplicate item for client JID " + itemId + "!",409);
			} else {
				e.printStackTrace();
				return new HttpResponse(e.getMessage(),400);
			}
		}
	}

	@GET
	@Path("/tests/{id}/disco")
	public HttpResponse getTestDisco(@PathParam("id") String testId, @QueryParam(value="format",defaultValue="csv") String format){
		String result = "";
		String cType = "text/plain";

		try{
			
			if(!existsTest(testId)){
				return new HttpResponse("Test " + testId + " does not exist!",404);
			}
			
			getTestDiscoQueryStatement.clearParameters();
			getTestDiscoQueryStatement.setString(1, testId);
			ResultSet rs = getTestDiscoQueryStatement.executeQuery();

			if("json".equals(format)){
				result = getTestDiscoJSON(rs);
				cType = "application/json";
			} else {
				result = getTestDiscoCSV(rs);
				cType = "text/csv";
			}

			HttpResponse response = new HttpResponse(result,200);
			response.setHeader("Content-type", cType);
			return response;

		} catch(Exception e){
			e.printStackTrace();
			return new HttpResponse(e.getMessage(),400);
		}
	}
	
	// ---------------------- PRIVATE HELPER METHODS ----------------------

	private String getTestDiscoCSV(ResultSet rs) throws SQLException{

		String csv = "testid;cjid;time;dur\n"; 

		if(rs == null){
			return csv;
		}

		while(rs.next()){
			String testId = rs.getString("testid");
			String cjid = rs.getString("cjid");
			Long time =  rs.getLong("time");
			String dur = rs.getString("dur");

			csv += testId + ";" + cjid + ";" + time + ";" + dur + "\n";
		}
		return csv;
	}

	@SuppressWarnings("unchecked")
	private String getTestDiscoJSON(ResultSet rs) throws SQLException{

		JSONArray a = new JSONArray();

		if(rs == null){
			return a.toJSONString();
		}

		while(rs.next()){
			JSONObject o = new JSONObject();

			String testId = rs.getString("testid");
			String cjid = rs.getString("cjid");
			Long time =  rs.getLong("time");
			Long dur = rs.getLong("dur");

			o.put("testid", testId);
			o.put("cjid", cjid);
			o.put("time", time);
			o.put("dur", dur);

			a.add(o);
		}

		return a.toJSONString();
	}
	
	private void storeTestDiscoCSV(String content) throws SQLException{
		BufferedReader br = null;
		try { 

			br = new BufferedReader(new StringReader(content));
			String line = "";
			String firstline = br.readLine();

			if(firstline == null){
				throw new IllegalArgumentException("Invalid data format detected! CSV must have at least one line.");
			}

			if(!"testid;cjid;time;dur".equals(firstline)){
				throw new IllegalArgumentException("Invalid data format detected! CSV must have header line 'id;cjid;time;dur'.");
			}

			while ((line = br.readLine()) != null) {
				// tokenize CSV line
				String[] tokens = line.split(";");

				if(tokens.length !=4){
					throw new IllegalArgumentException("Invalid data format detected! Each line in CSV must have 4 tokens, separated by semicolons (';').");
				}

				String testId = tokens[0];
				String cjid = tokens[1];

				Long time;
				try {
					time = Long.parseLong(tokens[2]);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Invalid data format detected! Time must be specified in milliseconds since Jan 1, 1970.");
				}
				
				Long dur;
				try {
					dur = Long.parseLong(tokens[3]);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Invalid data format detected! Duration must be specified in milliseconds");
				}
				
				testDiscoInsertStatement.clearParameters();

				testDiscoInsertStatement.setString(1, testId);
				testDiscoInsertStatement.setString(2, cjid);
				testDiscoInsertStatement.setLong(3, time);
				testDiscoInsertStatement.setLong(4, dur);

				testDiscoInsertStatement.executeUpdate();
				
			}

			//finally commit transaction
			connection.commit();
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void storeTestDiscoJSON(String content) throws SQLException{
		JSONArray a;

		try {
			a = (JSONArray) JSONValue.parseWithException(content);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Invalid data format detected! Data must be JSON-formatted. " + e.getMessage());	
		}

		JSONObject o = (JSONObject) a.get(0);

		if(o.get("testid") == null || o.get("cjid") == null || o.get("time") == null || o.get("dur") == null){
			throw new IllegalArgumentException("Invalid data format detected! Each data object must have fields testid, cjid, time and dur.");
		}

		for(int i = 0; i<a.size(); i++){

			JSONObject obj = (JSONObject) a.get(i);
			
			testDiscoInsertStatement.clearParameters();
			testDiscoInsertStatement.setString(1, (String) obj.get("testid"));
			testDiscoInsertStatement.setString(2, (String) obj.get("cjid"));
			testDiscoInsertStatement.setLong(3, (Long) obj.get("time"));
			testDiscoInsertStatement.setLong(4, (Long) obj.get("dur"));

			testDiscoInsertStatement.executeUpdate();
		}
		
		connection.commit();

	}

	private void storeTestMetaJSON(String content) throws SQLException{

		JSONObject o;

		try {
			o = (JSONObject) JSONValue.parseWithException(content);

			if(o.get("id") == null || o.get("smucs") == null || o.get("rpersmuc") == null || o.get("rtype") == null || o.get("topo") == null || o.get("stype") == null){
				throw new IllegalArgumentException("Invalid data format detected! Each data object must have fields id, smucs,rpersmuc,rtype,topo and stype.");
			}

			testMetaInsertStatement.clearParameters();
			testMetaInsertStatement.setString(1, (String) o.get("id"));
			testMetaInsertStatement.setLong(2, (Long) o.get("smucs"));
			testMetaInsertStatement.setLong(3, (Long) o.get("rpersmuc"));
			testMetaInsertStatement.setString(4, (String) o.get("rtype"));
			testMetaInsertStatement.setString(5, (String) o.get("topo"));
			testMetaInsertStatement.setString(6, (String) o.get("stype"));

			testMetaInsertStatement.executeUpdate();

		} catch (ParseException e) {
			throw new IllegalArgumentException("Invalid data format detected! Data must be JSON-formatted. " + e.getMessage());	
		}

	}


	private void storeTestMetaCSV(String content) throws SQLException, IOException{
		BufferedReader br = null;

		br = new BufferedReader(new StringReader(content));

		String firstline = br.readLine();

		if(firstline == null){
			throw new IllegalArgumentException("Invalid data format detected! CSV must have at least one line.");
		}

		if(!"id;smucs;rpersmuc;rtype;topo;stype".equals(firstline)){
			throw new IllegalArgumentException("Invalid data format detected! CSV must have header line 'id;smucs;rpersmuc;rtype;topo;stype'.");
		}

		String secondLine = br.readLine();

		// tokenize CSV line
		String[] tokens = secondLine.split(";");


		if(tokens.length != 6){
			throw new IllegalArgumentException("Invalid data format detected! Each line in CSV must have 6 tokens, separated by semicolons (';').");
		}

		String id = tokens[0];
		Integer smucs;
		Integer rpersmuc;
		try {
			smucs = Integer.parseInt(tokens[1]);
			rpersmuc = Integer.parseInt(tokens[2]);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid data format detected! Sender MUCs (smucs) and Receivers per sensor MUC (rpersmuc) must be specified as numeric values.");
		}

		String rtype = tokens[3];
		String topo = tokens[4];
		String stype = tokens[5];

		testMetaInsertStatement.clearParameters();

		testMetaInsertStatement.setString(1, id);
		testMetaInsertStatement.setInt(2, smucs);
		testMetaInsertStatement.setInt(3, rpersmuc);
		testMetaInsertStatement.setString(4, rtype);
		testMetaInsertStatement.setString(5, topo);
		testMetaInsertStatement.setString(6, stype);

		testMetaInsertStatement.executeUpdate();

		connection.commit();
	}

	private void storeDataPackageJSON(String testId, String content, boolean send) throws SQLException{

		// parse content; expected as JSON Array.
		JSONArray a;

		try {
			a = (JSONArray) JSONValue.parseWithException(content);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Invalid data format detected! Data must be JSON-formatted. " + e.getMessage());

		}

		if(!a.isEmpty()){

			if(!(a.get(0) instanceof JSONObject)){
				throw new IllegalArgumentException("Invalid data format detected! Data must be passed as array of JSON objects.");
			}

			JSONObject o = (JSONObject) a.get(0);

			if(o.get("id") == null || o.get("time") == null || o.get("fromjid") == null || o.get("tojid") == null || o.get("msg") == null || o.get("msgsize") == null || o.get("pldsize") == null){
				throw new IllegalArgumentException("Invalid data format detected! Each data object must have fields id, time, fromjid, tojid, msg, msgsize and pldsize.");
			}

			for(int i = 0; i<a.size(); i++){

				JSONObject obj = (JSONObject) a.get(i);
				Long time;

				try {
					time = Long.parseLong(obj.get("time").toString());
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Invalid data format detected! Time must be specified as decimal timestamp.");
				}

				if(!(obj.get("msgsize") instanceof Long && obj.get("pldsize") instanceof Long)){
					throw new IllegalArgumentException("Invalid data format detected! Message and payload size must be specified as numeric timestamp.");
				}

				if(send){
					sendDataInsertStatement.clearParameters();

					sendDataInsertStatement.setString(1, testId);
					sendDataInsertStatement.setString(2, (String) obj.get("id"));
					sendDataInsertStatement.setLong(3, time);
					sendDataInsertStatement.setString(4, (String) obj.get("fromjid"));
					sendDataInsertStatement.setString(5, (String) obj.get("tojid"));
					sendDataInsertStatement.setString(6, (String) obj.get("msg"));
					sendDataInsertStatement.setLong(7, (Long) obj.get("msgsize"));
					sendDataInsertStatement.setLong(8, (Long) obj.get("pldsize"));

					sendDataInsertStatement.executeUpdate();

				} else {
					receiveDataInsertStatement.clearParameters();

					receiveDataInsertStatement.setString(1, (String) obj.get("id"));
					receiveDataInsertStatement.setLong(2, time);
					receiveDataInsertStatement.setString(3, (String) obj.get("fromjid"));
					receiveDataInsertStatement.setString(4, (String) obj.get("tojid"));
					receiveDataInsertStatement.setString(5, (String) obj.get("msg"));
					receiveDataInsertStatement.setLong(6, (Long) obj.get("msgsize"));
					receiveDataInsertStatement.setLong(7, (Long) obj.get("pldsize"));

					receiveDataInsertStatement.executeUpdate();

				}
			}

			// finally commit transaction
			connection.commit();
		}
	}

	private void storeDataPackageCSV(String testId, String content,boolean send) throws SQLException{
		BufferedReader br = null;
		try { 

			br = new BufferedReader(new StringReader(content));
			String line = "";
			String firstline = br.readLine();

			if(firstline == null){
				throw new IllegalArgumentException("Invalid data format detected! CSV must have at least one line.");
			}

			if(!"id;time;fromjid;tojid;msg;msgsize;pldsize".equals(firstline)){
				throw new IllegalArgumentException("Invalid data format detected! CSV must have header line 'id;time;fromjid;tojid;msg;msgsize;pldsize'.");
			}

			while ((line = br.readLine()) != null) {
				// tokenize CSV line
				String[] tokens = line.split(";");

				if(tokens.length !=7){
					throw new IllegalArgumentException("Invalid data format detected! Each line in CSV must have 7 tokens, separated by semicolons (';').");
				}

				String id = tokens[0];

				Long time;
				try {
					time = Long.parseLong(tokens[1]);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Invalid data format detected! Time must be specified as milliseconds since Jan 1, 1970.");
				}

				String fromjid = tokens[2];
				String tojid = tokens[3];
				String msg = tokens[4];

				Integer msgsize = 0;
				Integer pldsize = 0;
				try {
					msgsize = Integer.parseInt(tokens[5]);
					pldsize = Integer.parseInt(tokens[6]);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Invalid data format detected! Message size and payload size must be specified as numeric values.");
				}

				if(send){

					sendDataInsertStatement.clearParameters();

					sendDataInsertStatement.setString(1, testId);
					sendDataInsertStatement.setString(2, id);
					sendDataInsertStatement.setLong(3, time);
					sendDataInsertStatement.setString(4, fromjid);
					sendDataInsertStatement.setString(5, tojid);
					sendDataInsertStatement.setString(6, msg);
					sendDataInsertStatement.setInt(7, msgsize);
					sendDataInsertStatement.setInt(8, pldsize);

					sendDataInsertStatement.executeUpdate();

				} else {

					receiveDataInsertStatement.clearParameters();

					receiveDataInsertStatement.setString(1, id);
					receiveDataInsertStatement.setLong(2, time);
					receiveDataInsertStatement.setString(3, fromjid);
					receiveDataInsertStatement.setString(4, tojid);
					receiveDataInsertStatement.setString(5, msg);
					receiveDataInsertStatement.setInt(6, msgsize);
					receiveDataInsertStatement.setInt(7, pldsize);

					receiveDataInsertStatement.executeUpdate();
				}
			}


			//finally commit transaction
			connection.commit();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private JSONObject getTestMetaJSON(ResultSet rs) throws SQLException{
		if(rs == null){
			return null; 
		}

		if(rs.next()){
			JSONObject o = new JSONObject();

			o.put("id",rs.getString("id"));
			o.put("smucs",rs.getInt("smucs"));
			o.put("rpersmuc",rs.getInt("rpersmuc"));
			o.put("rtype",rs.getString("rtype"));
			o.put("topo",rs.getString("topo"));
			o.put("stype",rs.getString("stype"));

			return o;

		} else {
			return null;
		}
	}

	private String getTestMetaCSV(ResultSet rs) throws SQLException{
		if(rs == null){
			return null; 
		}
		String csv = "id;smucs;rpersmuc;rtype;topo;stype\n"; 
		if(rs.next()){

			String id = rs.getString("id");
			Integer smucs = rs.getInt("smucs");
			Integer rpersmuc = rs.getInt("rpersmuc");
			String rtype = rs.getString("rtype");
			String topo = rs.getString("topo");
			String stype = rs.getString("stype");

			csv += id + ";" + smucs + ";" + rpersmuc + ";" + rtype + ";" + topo + ";" + stype + "\n";
			return csv;
		} else {
			return null;
		}
	}

	private String getDataCSV(ResultSet rs) throws SQLException{

		String csv = "testid;id;sendtime;sendfromjid;sendtojid;sendmsg;sendmsgsize;sendpldsize;receivetime;receivefromjid;receivetojid;receivemsg;receivemsgsize;receivepldsize\n"; 

		if(rs == null){
			return csv;
		}

		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setDecimalSeparator('.');
		DecimalFormat df = new DecimalFormat("#.000");
		df.setDecimalFormatSymbols(dfs);

		while(rs.next()){
			String testId = rs.getString("testid");
			String id = rs.getString("id");

			Long sendTime =  rs.getLong("send_time");
			String sendFromJid = rs.getString("send_fromjid");
			String sendToJid = rs.getString("send_tojid");
			String sendMsg = rs.getString("send_msg");
			Integer sendMsgSize = rs.getInt("send_msgsize");
			Integer sendPldSize = rs.getInt("send_pldsize");

			Long receiveTime = rs.getLong("receive_time");
			String receiveFromJid = rs.getString("receive_fromjid");
			String receiveToJid = rs.getString("receive_tojid");
			String receiveMsg = rs.getString("receive_msg");
			Integer receiveMsgSize = rs.getInt("receive_msgsize");
			Integer receivePldSize = rs.getInt("receive_pldsize");

			csv += testId + ";" + id + ";" + sendTime + ";" + sendFromJid + ";" + sendToJid + ";" + sendMsg + ";" + sendMsgSize + ";" + sendPldSize + ";" +
					receiveTime + ";" + receiveFromJid + ";" + receiveToJid + ";" + receiveMsg + ";" + receiveMsgSize + ";" + receivePldSize + "\n";
		}
		return csv;
	}

	@SuppressWarnings("unchecked")
	private String getDataJSON(ResultSet rs) throws SQLException{

		JSONArray a = new JSONArray();

		if(rs == null){
			return a.toJSONString();
		}

		while(rs.next()){
			JSONObject o = new JSONObject();

			String testId = rs.getString("testid");
			String id = rs.getString("id");

			Long sendTime =  rs.getLong("send_time");
			String sendFromJid = rs.getString("send_fromjid");
			String sendToJid = rs.getString("send_tojid");
			String sendMsg = rs.getString("send_msg");
			Integer sendMsgSize = rs.getInt("send_msgsize");
			Integer sendPldSize = rs.getInt("send_pldsize");

			Long receiveTime = rs.getLong("receive_time");
			String receiveFromJid = rs.getString("receive_fromjid");
			String receiveToJid = rs.getString("receive_tojid");
			String receiveMsg = rs.getString("receive_msg");
			Integer receiveMsgSize = rs.getInt("receive_msgsize");
			Integer receivePldSize = rs.getInt("receive_pldsize");

			o.put("testid", testId);
			o.put("id", id);
			o.put("sendtime", sendTime);
			o.put("sendfromjid", sendFromJid);
			o.put("sendtojid", sendToJid);
			o.put("sendmsg", sendMsg);
			o.put("sendmsgsize", sendMsgSize);
			o.put("sendpldsize", sendPldSize);
			o.put("receivetime", receiveTime);
			o.put("receivefromjid", receiveFromJid);
			o.put("receivetojid", receiveToJid);
			o.put("receivemsg", receiveMsg);
			o.put("receivemsgsize", receiveMsgSize);
			o.put("receivepldsize", receivePldSize);

			a.add(o);
		}

		return a.toJSONString();
	}
	
	private boolean existsTest(String testId) throws SQLException{
		getTestMetaQueryStatement.clearParameters();
		getTestMetaQueryStatement.setString(1, testId);
		
		ResultSet rs = getTestMetaQueryStatement.executeQuery();
		
		if(rs != null && rs.next()){
			System.out.println("Test " + testId + " exists.");
			return true;
		} else {
			System.out.println("Test " + testId + " not found!");
			return false;
		}
		
	}

	/**
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	private void initDatabaseConnection() throws ClassNotFoundException, SQLException{

		Class.forName(jdbcDriverClassName);
		connection = DriverManager.getConnection(jdbcUrl+jdbcSchema,jdbcLogin, jdbcPass);
		connection.setAutoCommit(false);

		testMetaInsertStatement = connection.prepareStatement("insert into " + jdbcSchema + ".testmeta(id,smucs,rpersmuc,rtype,topo,stype) values (?,?,?,?,?,?)");
		testDiscoInsertStatement = connection.prepareStatement("insert into " + jdbcSchema + ".disco(testid,cjid,time,dur) values (?,?,?,?)");
		sendDataInsertStatement = connection.prepareStatement("insert into " + jdbcSchema + ".send(testid,id,time,fromjid,tojid,msg,msgsize,pldsize) values (?,?,?,?,?,?,?,?)");
		receiveDataInsertStatement = connection.prepareStatement("insert into " + jdbcSchema + ".receive(id,time,fromjid,tojid,msg,msgsize,pldsize) values (?,?,?,?,?,?,?)");

		// if view acdsense.dataset is available, use it. If not use view query explicitly.
		//getDataQueryStatement = connection.prepareStatement("select * from acdsense.dataset");
		getDataQueryStatement = connection.prepareStatement("select s.testid, s.id, s.time as send_time, s.fromjid as send_fromjid, s.tojid as send_tojid, s.msg as send_msg, s.msgsize as send_msgsize, s.pldsize as send_pldsize, r.time as receive_time, r.fromjid as receive_fromjid, r.tojid as receive_tojid, r.msg as receive_msg, r.msgsize as receive_msgsize, r.pldsize as receive_pldsize from " + jdbcSchema + ".send as s join " + jdbcSchema + ".receive as r on (s.id = r.id)");
		getTestDataQueryStatement = connection.prepareStatement("select s.testid, s.id, s.time as send_time, s.fromjid as send_fromjid, s.tojid as send_tojid, s.msg as send_msg, s.msgsize as send_msgsize, s.pldsize as send_pldsize, r.time as receive_time, r.fromjid as receive_fromjid, r.tojid as receive_tojid, r.msg as receive_msg, r.msgsize as receive_msgsize, r.pldsize as receive_pldsize from " + jdbcSchema + ".send as s join " + jdbcSchema + ".receive as r on (s.id = r.id) where s.testid = ?");

		getTestMetaQueryStatement = connection.prepareStatement("select * from " + jdbcSchema + ".testmeta where id = ?");
		getTestDiscoQueryStatement = connection.prepareStatement("select * from " + jdbcSchema + ".disco where testid = ?");

		deleteTestMetaStatement = connection.prepareStatement("delete from " + jdbcSchema + ".testmeta where id = ?");

	}

	/**
	 * get all annotation and method data to allow mapping
	 */
	public String getRESTMapping()
	{
		String result="";
		try {
			result=RESTMapper.getMethodsAsXML(this.getClass());
		} catch (Exception e) {

			e.printStackTrace();
		}
		return result;
	}

}
