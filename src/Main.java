
import java.io.BufferedInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;
//import java.sql.DriverManager;
//import java.sql.Statement;
import java.time.LocalDate;
//import java.util.Date;
import java.util.Properties;
import com.mysql.cj.jdbc.MysqlDataSource;

public class Main {

    /* DECLARE INSTANCE VARS RELATED TO JDBC WORK */
    private static PreparedStatement pS = null;
    private static ResultSet resultSet = null;
    private static LocalDate date = LocalDate.now();
    private static Connection connection = null;
    /* INSTANCE SCANNER AND RANDOM OBJECTS */
    private static Random random = new Random();
    private static final Scanner scan = new Scanner(System.in);


    public static void main(String[] args) throws SQLException, IOException {
        /*DECLARE SOME VARIABLES */
        int id;
        int login=1;
        String passWord;

        /* ATTEMPT TO ESTABLISH CONNECTION TO SERVER, IF FAILS, EXIT */
        Properties props = new Properties();
        String propsFileName = "/db.properties";
        BufferedInputStream propsFile = (BufferedInputStream) Main.class.getResourceAsStream(propsFileName);
        props.load(propsFile); //POPULATE PROPS WITH DB.PROPERTIES
        try {
            MysqlDataSource ds = new MysqlDataSource();
            ds.setURL(props.getProperty("MYSQL_DB_URL"));
            ds.setUser(props.getProperty("MYSQL_DB_USERNAME"));
            ds.setPassword(props.getProperty("MYSQL_DB_PASSWORD"));
            connection = ds.getConnection(); //ESTABLISH CONNECTION
        }
        catch (SQLException e){
            System.out.println(e);
            System.exit(-1); //EXIT WITH -1
        }

        /* PRINT OUT MAIN MENU USER OPTIONS */
        while(login != 0){// WHILE(TRUE) SHOULD ALSO WORK BECAUSE OF BREAK STATEMENT
            System.out.println("Customer portal: 1\nAdmin portal: 2\nRegister as Customer: 3\nExit: 0");
            login = scan.nextInt();
            scan.nextLine();
            switch(login){
                case 0:
                    System.out.println("Goodbye");
                    break;
                case 1:
                    customerLogin();
                    continue;
                case 2:
                    adminLogin();
                    continue;
                case 3:
                    registerCustomer();
                    continue;
                default:
                    System.out.println(login + " is not a valid selection");
            }
        }
    }

    /* GIVEN USER INPUT, SEARCH FOR CREDENTIALS IN THE ADMIN TABLE. IF FOUND, CONSTRUCT ADMIN OBJECT WITH APPROPRIATE VALUES AND CALL ADMINPORTAL METHOD */
    public static void adminLogin() throws SQLException, IOException {
        int id;
        String query;
        String name = null;
        String passWord;
        String email=null;
        boolean found = false;
        /* GET EMPLOYEE ID (eId) AND PASSWORD FROM USER */
        System.out.println("Enter Id");
        id = scan.nextInt();
        scan.nextLine();
        System.out.println("Enter password");
        passWord = scan.nextLine();
        try {
            /* IF THERE'S A ROW WITH THE ENTERED EID AND PASSWORD, THEN THE GIVEN CREDENTIALS ARE VALID */
            query = "select * from admin where eId = "+id+" and epassWord = '"+passWord+"'";
            pS = connection.prepareStatement(query);
            resultSet = pS.executeQuery();
            while(resultSet.next()){
                /* GRAB NAME AND EMAIL FROM THAT ROW FOR THE ADMIN OBJECT */
                name = resultSet.getString("eName");
                email = resultSet.getString("email");
                found = true;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        /* IF LOGIN UNSUCCESSFUL RETURN */
        if (!found) {
            System.out.println("Invalid Credentials. Login unsuccessful.");
            return;
        }
        /* CREATE ADMIN OBJECT WITH INFORMATION FROM ROW AND CALL ADMIN PORTAL */
        Admin admin = new Admin(email, passWord, name, id);
        admin.adminPortal();
    }

    /* GIVEN USER INPUT, SEARCH FOR CREDENTIALS IN THE CUSTOMER TABLE. IF FOUND, CONSTRUCT CUSTOMER OBJECT WITH APPROPRIATE VALUES AND CALL CUSTOMERPORTAL METHOD */
    public static void customerLogin() throws SQLException, IOException {
        String query, name=null, email, passWord;
        int id = 0;
        boolean found = false;
        /* GET CUSTOMER EMAIL AND PASSWORD */
        System.out.println("Enter email");
        email = scan.nextLine();
        System.out.println("Enter passWord");
        passWord = scan.nextLine();
        try{
            /* IF THERE'S A ROW WITH THE ENTERED  EMAIL AND PASSWORD, THEN THE GIVEN CREDENTIALS ARE VALID */
            query = "select * from customer where email = '"+email+"' and cpassWord = '"+passWord+"'";
            pS = connection.prepareStatement(query);
            resultSet = pS.executeQuery();
            while(resultSet.next()){
                name = resultSet.getString(2);
                id = resultSet.getInt("cId");
                found = true;
            }
        }
        catch(Exception e){
            System.out.println(e);
        }
        /* IF LOGIN UNSUCCESSFUL RETURN */
        if(!found){
            System.out.println("Invalid credentials. Login Unsuccessful");
            return;
        }
        /* CREATE CUSTOMER OBJECT WITH INFORMATION FROM ROW AND CALL CUSTOMER PORTAL */
        Customer customer = new Customer(name, email, passWord, id);
        customer.customerPortal();
    }

    /* NEW CUSTOMERS ARE ABLE TO CREATE AN ACCOUNT FOR THEMSELVES, WHICH MEANS ADDING A ROW WITH THE USER'S INPUT */
    public static void registerCustomer() throws SQLException {
        String name;
        String query;
        String passWord;
        String email;
        int newId;
        /* GET USER INFO */
        System.out.println("Enter name");
        name = scan.nextLine();
        System.out.println("Enter email");
        email = scan.nextLine();
        System.out.println("Create password");
        passWord = scan.nextLine();
        newId = random.nextInt(1000);
        try {
            /* INSERT NEW ROW WITH USER INPUT */
            query = "insert into customer values(" + newId + ",'" + name + "','" + passWord + "','" + email + "');";
            pS = connection.prepareStatement(query);
            pS.executeUpdate();
        }
        catch(SQLException e){
            System.out.println(e);
        }
    }
}
