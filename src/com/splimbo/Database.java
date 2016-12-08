package com.splimbo;

import java.sql.*;

public class Database {
	
	public static Connection getMySQLConnection(String database){
		Connection result = null;
		try {
			try {
				Class.forName("com.mysql.jdbc.Driver").newInstance();
			} catch (InstantiationException | IllegalAccessException
					| ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			result = DriverManager.getConnection("jdbc:mysql://localhost:3306/"+database+"?" + 
												"user=root&password=root");
		} 
		catch (SQLException e) {
			e.printStackTrace();
		}
		
		return result;
	}	
}
