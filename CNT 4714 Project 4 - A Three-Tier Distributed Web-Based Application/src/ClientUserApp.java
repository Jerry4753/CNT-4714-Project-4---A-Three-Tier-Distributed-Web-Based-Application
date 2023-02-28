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

public class ClientUserApp extends HttpServlet{
	private Connection connection2; // normal user command connection
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
		
		try {
			connectToDatabase();
			// create a statement
			statement = connection2.createStatement();
			
			//handle user command
			if(sqlStatement.toLowerCase().startsWith("select")) {
				ResultSet resultSet = statement.executeQuery(sqlStatement);
				message = ResultSetToHTMLFormatter.getHtmlRows(resultSet);
				
			}
			else {
				statement.executeUpdate(sqlStatement);
			}
			
			
			statement.close();
			connection2.close();

			
		}catch(SQLException e) {
			message = "<tr bgcolor=#ff0000><td style=\"text-align:center\"><font color=#ffffff><b>Error executing the SQL statement:</b><br>" + e.getMessage() + "</font></td></tr>";
		}
		
		HttpSession session = request.getSession();
		session.setAttribute("message", message);
		session.setAttribute("sqlStatement", sqlStatement);
		response.sendRedirect("clientUser.jsp");
	
//		RequestDispatcher dispatcher = ServletConfig.getServletContext().getRequestDispatcher("/rootUser.jsp");
//		dispatcher.forward(request, response);

	}
	
	
	// connect to database
	public void connectToDatabase() {
		Properties properties = new Properties();
		FileInputStream filein = null;
		MysqlDataSource dataSource = null;
		
		try {
			// read properties file
			filein = new FileInputStream("C:/Program Files/Apache Software Foundation/Tomcat 10.0/webapps/Project4/WEB-INF/lib/client.properties");
			properties.load(filein);
			
			// set the parameters
			dataSource = new MysqlDataSource();
			dataSource.setUrl(properties.getProperty("MYSQL_DB_URL"));
			dataSource.setUser(properties.getProperty("MYSQL_DB_USERNAME"));
			dataSource.setPassword(properties.getProperty("MYSQL_DB_PASSWORD"));
			
			//establish a connection
			connection2 = dataSource.getConnection();
		}
		catch (SQLException sqlException) {
			sqlException.printStackTrace();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	

			
}
