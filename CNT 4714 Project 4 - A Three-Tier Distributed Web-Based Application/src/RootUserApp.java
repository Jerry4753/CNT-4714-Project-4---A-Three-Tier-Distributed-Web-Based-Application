import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;

import com.mysql.cj.jdbc.MysqlDataSource;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

public class RootUserApp extends HttpServlet{
	private Connection connection; // normal user command connection
	private Statement statement; // statement object for sending command to DB
	private int mysqlReturnValue; // int returned from updating command
	private int [] updateReturnValues; // this array holds the MySQL return values for the business logic operations
									   // updateReturnValues[0] holds the result of the original user update command
									   // updateReturnValues[0] holds the result of the business logic update command
	
	/*
	 * The doPost() method handles the execution of the user request, i.e. their SQL command
	 * It takes the text from HTML text area and checks it to determine if it is a SELECT or and UPDATE command
	 * Based on the result, the command is passed to the appropriate executor
	 * All the results of the query are then passed to the ResultSetToHTMLFormatter class for conversion into a format
	 * (an HTML Table) that can be rendered by any web browser (HTML). All errors or responses by the server are returned
	 * to the user's browser session
	 */
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException{
		String sqlStatement = request.getParameter("sqlStatement");
		String message = "";
		String copyTable = "drop table if exists beforeshipments;";
		String copyTable2= "create table beforeshipments like shipments;";
		String copyTable3 = "insert into beforeshipments select * from shipments;";

		
		String after = "update suppliers "
				+ "set status = status + 5 "
				+ "where suppliers.snum in "
				+ "	(select distinct snum from shipments "
				+ "		where shipments.quantity >= 100 "
				+ "		and "
				+ "		not exists (select * from beforeShipments "
				+ "			where shipments.snum = beforeshipments.snum "
				+ "			and shipments.pnum = beforeshipments.pnum "
				+ "			and shipments.jnum = beforeshipments.jnum "
				+ "			and beforeshipments.quantity >= 100 "
				+ "		)"
				+ "	);";
		String drop = "drop table beforeShipments;";
		
		try {
			connectToDatabase();
			// create a statement
			statement = connection.createStatement();
			
			//handle user command
			if(sqlStatement.toLowerCase().startsWith("select")) {
				ResultSet resultSet = statement.executeQuery(sqlStatement);
				message = ResultSetToHTMLFormatter.getHtmlRows(resultSet);
				
			}
			else {
				if(sqlStatement.toLowerCase().startsWith("insert into shipments")
						|| sqlStatement.toLowerCase().startsWith("update shipments")
						|| sqlStatement.toLowerCase().startsWith("replace into shipments values")
						|| sqlStatement.toLowerCase().startsWith("replace into shipments set")
						) {
					statement.executeUpdate(copyTable);
					statement.executeUpdate(copyTable2);
					statement.executeUpdate(copyTable3);
					int rows = statement.executeUpdate(sqlStatement);
					int rows2 = statement.executeUpdate(after);
					statement.executeUpdate(drop);
					message ="<tr bgcolor=#46FF00><td style=\"text-align:center\"><font color=#000000><b>The statement executed successfully.<br>"
			                + rows + " row(s) affected.<br><br> Business Logic Detected! - Updating Supplier Status.<br>Business Logic updated " + rows2 + " supplier status marks.</b></font></td></tr>";
				}
				else {
					statement.executeUpdate(copyTable);
					statement.executeUpdate(copyTable2);
					statement.executeUpdate(copyTable3);
					int rows = statement.executeUpdate(sqlStatement);
					statement.executeUpdate(drop);
					message ="<tr bgcolor=#46FF00><td style=\"text-align:center\"><font color=#000000><b>The statement executed successfully.<br>"
			                + rows + " row(s) affected.<br><br> Business Logic Not Triggered!</b></font></td></tr>";
				}
				
			}
			
			
			statement.close();
			connection.close();

			
		}catch(SQLException e) {
			message = "<tr bgcolor=#ff0000><td style=\"text-align:center\"><font color=#ffffff><b>Error executing the SQL statement:</b><br>" + e.getMessage() + "</font></td></tr>";
		}
		
		HttpSession session = request.getSession();
		session.setAttribute("message", message);
		session.setAttribute("sqlStatement", sqlStatement);
		response.sendRedirect("rootUser.jsp");
	
//		RequestDispatcher dispatcher = ServletConfig.getServletContext().getRequestDispatcher("/rootUser.jsp");
//		dispatcher.forward(request, response);

	}
	
	
	// connect to database
	public void connectToDatabase() {
		// update operations log db as a root user client: +1 to num_updates
		Properties properties = new Properties();
		FileInputStream filein = null;
		MysqlDataSource dataSource = null;
		
		try {
			// read properties file
			filein = new FileInputStream("C:/Program Files/Apache Software Foundation/Tomcat 10.0/webapps/Project4/WEB-INF/lib/root.properties");
			properties.load(filein);
			
			// set the parameters
			dataSource = new MysqlDataSource();
			dataSource.setUrl(properties.getProperty("MYSQL_DB_URL"));
			dataSource.setUser(properties.getProperty("MYSQL_DB_USERNAME"));
			dataSource.setPassword(properties.getProperty("MYSQL_DB_PASSWORD"));
			
			//establish a connection
			connection = dataSource.getConnection();
		}
		catch (SQLException sqlException) {
			sqlException.printStackTrace();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	

			
}
