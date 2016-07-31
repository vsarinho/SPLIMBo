package com.simion;

import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;


public class Messenger extends Thread {
	public class Session<ZapApp, Date> { 
		public ZapApp currentApp; 
		public Date initialDate; 
		public Session(ZapApp currentApp, Date initialDate) { 
			this.currentApp = currentApp; 
			this.initialDate = initialDate; 
		} 
	} 
	
	public static int EXPIRATION_TIME = 20;
	public static HashMap<String, Session<ZapApp,Date>> sessions = new HashMap<String, Session<ZapApp,Date>>();
	public static Connection conn = null;
	
	
	public Messenger(String str) {
		super(str);
	}
	
	public static void sendMessage(String jidServer, String jidClient, String url, String message){
		try {
			PreparedStatement pdst = conn.prepareStatement(
					"insert into Queue(jidServer, jidClient, url, message, status, dateTime, dateTimeToSend) values (?,?,?,?,?,?,?)");
			pdst.setString(1, jidServer);
			pdst.setString(2, jidClient);
			pdst.setString(3, url);
			pdst.setString(4, message);
			pdst.setString(5, "S");
			
			String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());			
			pdst.setString(6, currentDate);
			pdst.setString(7, currentDate);
			pdst.executeUpdate();	
			
			conn.commit();			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void sendMedia(String jidServer, String jidClient, String url, String path, String extension){
		
        try {
			PreparedStatement pdst = conn.prepareStatement(
					"insert into Queue(jidServer, jidClient, url, data, imageLabel, extension, status, dateTime, dateTimeToSend) "
					+ "values (?,?,?,?,?,?,?,?,?)");
			pdst.setString(1, jidServer);
			pdst.setString(2, jidClient);
			pdst.setString(3, url);
			pdst.setBytes(4, null);
			pdst.setString(5, path);
			pdst.setString(6, extension);
			pdst.setString(7, "S");
			
			String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
			pdst.setString(8, currentDate);
			pdst.setString(9, currentDate);
			pdst.executeUpdate();

			conn.commit();
			
			System.out.println("sendMedia:" + path);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		while(true){
			System.out.println(new Date());
			
			conn = Database.getMySQLConnection(Util.ZAPSERVER_DATABASE);	
			
			try { 
				conn.setAutoCommit(false);
				
				PreparedStatement pdst = conn.prepareStatement(
						"select id, jidServer, jidClient, url, message, extension, imageLabel from Queue where status = 'R'");
				
				ArrayList<String> messageIds = new ArrayList<String>();
				
				ResultSet rs =  pdst.executeQuery();
				while (rs.next()) {
					messageIds.add(rs.getString("id"));
					String jidClient = rs.getString("jidClient");
					String jidServer = rs.getString("jidServer");
					String jidUrlClient = rs.getString("url");
					String message = rs.getString("message");
					String extension = rs.getString("extension");
					String imageLabel = rs.getString("imageLabel");
					
					Session<ZapApp,Date> session = (Session<ZapApp,Date>) sessions.get(jidClient);
					
					String[] tokens = null;
					String appCode = "";
					String zapAppRef = null;
					
					if ((session == null) || 
						((session != null) && ((new Date().getTime()) - session.initialDate.getTime() > 1000 * 60 * EXPIRATION_TIME)) ||
						((session != null) && (!jidServer.equals((String) session.currentApp.cursor.variables.get("jidServer"))))){
						
						zapAppRef = getZapAppReference(jidClient);
						if (zapAppRef != null) tokens = zapAppRef.split(";");
						appCode = jidClient;						
						
						if (tokens == null){
							zapAppRef = getZapAppReference(jidServer);
							if (zapAppRef != null) tokens = zapAppRef.split(";");
							appCode = jidServer;							
						}
						if (tokens == null){
							zapAppRef = getZapAppReference(message);
							if (zapAppRef != null) tokens = zapAppRef.split(";");
							appCode = message;							
						}
						
						if (tokens == null){
							sendMessage(jidServer, jidClient, jidUrlClient, "App code, please:");
						}
						else {						
							ZapApp zapApp = (ZapApp) Class.forName(tokens[0]).newInstance();
							
							zapApp.setJidClient(jidClient);
							zapApp.setJidUrlClient(jidUrlClient);
							zapApp.setAppCode(appCode);
							if (tokens.length > 1) zapApp.setXmlConfig(tokens[1]);
							
							Session<ZapApp,Date> newSession = new Session(zapApp, new Date());
							sessions.put(jidClient, newSession);
							
							newSession.currentApp.perform(jidServer, message, extension, imageLabel);
						}
					}
					else {						
						session.currentApp.perform(jidServer, message, extension, imageLabel);
						session.initialDate = new Date();
					}
				}
				rs.close();
				
				// remove evaluated messages
				Iterator<String> iter = messageIds.iterator();
				String ids = "";
				while (iter.hasNext()){
					ids += iter.next()+",";
				}
				if (!"".equals(ids)){
					ids = ids.substring(0,ids.length()-1);
					
					pdst = conn.prepareStatement(
							"delete from Queue where id in ("+ids+")");
					pdst.executeUpdate();
					
					System.out.println("removing:"+ ids);
				}
				
				
				pdst.close();
				conn.commit();
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
			finally {
				try {
					if (conn != null){
						conn.rollback();
						conn.close();
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			
			try {
				sleep((int)(2000));
			} 
			catch (Exception e) {}
		}
	}

	public String getZapAppReference(String code) throws Exception {		 
		Properties prop = new Properties();
		String propFileName = Util.APPS_PATH + "zapApps.properties";
 
		prop.load(new FileInputStream(propFileName));
		
 		return prop.getProperty(code);
	}	
	
}
