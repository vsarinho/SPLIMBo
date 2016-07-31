package com.simion.monitors;

import java.io.FileInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import com.restfb.*;
import com.restfb.types.*;
import com.simion.Database;


public class FacebookMonitor extends java.lang.Thread {
	private String MY_ACCESS_TOKEN;
	private Random random = new Random();
	public java.sql.Connection conn = null;
	
	public FacebookMonitor(String MY_ACCESS_TOKEN) {
		this.MY_ACCESS_TOKEN = MY_ACCESS_TOKEN;
	}

	public void run() {
		int  msg_count = 0;
		while (msg_count < 200){
			try {
				conn = Database.getMySQLConnection("zapserver");
				conn.setAutoCommit(false);
				FacebookClient facebookClient = new DefaultFacebookClient(MY_ACCESS_TOKEN);
				
				User user = facebookClient.fetchObject("me", User.class);		
				System.out.println("User name: " + user.getName());
				
				
				HashMap<String,LinkedList<String>> talks = new HashMap<String,LinkedList<String>>();
				boolean firstCharge = true;
				
				while (true){					
					Connection<Conversation> conversations = facebookClient.fetchConnection("me/conversations", 
																							Conversation.class, 
																							Parameter.with("limit",5));
								
					for (List<Conversation> conversationPage : conversations) {
						for (Conversation conversation : conversationPage) {
							LinkedList<String> sentMessages;
							if (talks.containsKey(conversation.getId()))
								sentMessages = talks.get(conversation.getId());
							else {
								sentMessages = new LinkedList<String>();
								talks.put(conversation.getId(), sentMessages);	
							}
			
							int count = 1;
							for (Message message : conversation.getMessages()) {
								if (!message.getFrom().getName().equals(user.getName()) && !sentMessages.contains(message.getId())){
									System.out.println("Message "+(count++)+": id = " + message.getId()+
														" text = " + message.getMessage()+
														" from = " + message.getFrom().getName()+
														" to = " + message.getTo().get(0).getName());
				
									if (!firstCharge){
										registrarMensagem(user.getName(), conversation.getId(), message.getMessage());
									}
									sentMessages.add(message.getId());
									if (sentMessages.size()>30) sentMessages.remove(0);		                	
								}
							}		            
			
						}    
					}
					
					// enviar mensagens processadas
					enviarMensagens(facebookClient, user.getName());
					
					
					// espera + impress�o de status
					try {
			            sleep(20000);
			        }
			        catch (InterruptedException ie) {
			        }
					
					if (firstCharge) firstCharge = false;
					System.out.println(user.getName()+" ### "+(new java.util.Date()));
				}
				
			}
			catch (Exception e){
				e.printStackTrace();
				
				try {
					conn.close();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
			
			// espera para tentar nova conex�o
			try {
	            sleep(120000);
	        }
	        catch (InterruptedException ie) {
	        }
			
			msg_count++;
		}
	}
	
	private void enviarMensagens(FacebookClient facebookClient, String jidServer) {
		java.sql.PreparedStatement pdst;
		try {
			pdst = conn.prepareStatement("SET NAMES utf8mb4");
			pdst.executeUpdate();
			pdst = conn.prepareStatement("SET CHARACTER SET utf8mb4");
			pdst.executeUpdate();
			pdst = conn.prepareStatement("SET character_set_connection=utf8mb4");
			pdst.executeUpdate();
			
			pdst = conn.prepareStatement(
						"select id, jidClient, message, extension, imageLabel "+
						"from Queue where (dateTimeToSend < now() or dateTimeToSend is null) and status = 'S' "+ 
						"and jidServer = ? AND url = 'facebook' LIMIT 5");
			pdst.setString(1, jidServer);
			
			ResultSet rs =  pdst.executeQuery();
			
			ArrayList<String> messageIds = new ArrayList<String>();
			
			while (rs.next()) {
				messageIds.add(rs.getString("id"));
				String jidCliente = rs.getString("jidClient");
				String message = rs.getString("message");
				String extension = rs.getString("extension");
				String imageLabel = rs.getString("imageLabel");
				
				if (imageLabel == null || "".equals(imageLabel)){
					System.out.println("send text!!");
					
					String spaces = new String(new char[random.nextInt(14)+1]).replace('\0', ' ');
					
					facebookClient.publish(jidCliente+"/messages", FacebookType.class, 
							Parameter.with("message", message+spaces));
				}
				else {
					System.out.println("send media!! imageLabel="+imageLabel);
					facebookClient.publish(jidCliente+"/messages", FacebookType.class,
							  BinaryAttachment.with("file."+extension, new FileInputStream(imageLabel)));
				}				
			}			
			rs.close();			


			Iterator<String> iter = messageIds.iterator();
			String ids = "";
			while (iter.hasNext()){
				ids += iter.next()+",";
			}
			if (!"".equals(ids)){
				ids = ids.substring(0,ids.length()-1);
				
				pdst = conn.prepareStatement("delete from Queue where id in ("+ids+")");
				pdst.executeUpdate();
			}			
			
			pdst.close();
			conn.commit();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void registrarMensagem(String jidServer, String jidClient, String message){
		try {
			java.sql.PreparedStatement pdst = conn.prepareStatement("SET NAMES utf8mb4");
			pdst.executeUpdate();
			pdst = conn.prepareStatement("SET CHARACTER SET utf8mb4");
			pdst.executeUpdate();
			pdst = conn.prepareStatement("SET character_set_connection=utf8mb4");
			pdst.executeUpdate();
			
			pdst = conn.prepareStatement(
					"insert into Queue(jidServer, jidClient, url, message, extension, imageLabel, status, dateTime) " +
					"values (?,?,?,?,?,?,?,now())");
			pdst.setString(1, jidServer);
			pdst.setString(2, jidClient);
			pdst.setString(3, "facebook");
			pdst.setString(4, message);
			pdst.setString(5, "txt");
			pdst.setString(6, "");
			pdst.setString(7, "R");
			pdst.executeUpdate();
			
			pdst = conn.prepareStatement(
					"insert into Log(jidServer, jidClient, url, message, extension, status, dateTime) values (?,?,?,?,?,?,now())");
			pdst.setString(1, jidServer);
			pdst.setString(2, jidClient);
			pdst.setString(3, "facebook");
			pdst.setString(4, message);
			pdst.setString(5, "txt");
			pdst.setString(6, "R");			
			pdst.executeUpdate();
			
			pdst.close();
			conn.commit();			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
}

