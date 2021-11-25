import com.mysql.cj.jdbc.MysqlDataSource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;

public class Admin {
    /* EACH VARIABLE CORRESPONDS TO A COLUMN IN ADMIN */
    String name;
    String passWord;
    String email;
    int eId;
    /* GLOBALS RELATED TO JDBC WORK */
    private static PreparedStatement pS = null;
    private static ResultSet resultSet = null;
    private static LocalDate date = LocalDate.now();
    private static Connection connection = null;
    /* SCANNER AND RANDOM */
    private static Random random = new Random();
    private static final Scanner scan = new Scanner(System.in);

    /* ADMIN CONSTRUCTOR ASSIGNS VARIABLES TO THE VALUES GOTTEN FROM THE ROW FOUND IN ADMIN LOGIN METHOD IN MAIN */
    public Admin(String email, String passWord, String name, int eId) {
        this.email = email;
        this.passWord = passWord;
        this.eId = eId;
        this.name = name;
    }

    /* DELETES EVERY ROW FROM INVENTORY AND FILLS AGAIN WITH ALL ITEMS WITH QUANTITY OF 1000
       Note: you could simply set amount back to 1000.00 in each row, but that wouldn't take into account
       new rows in the items table
        */
    private void fillInventory() throws SQLException{
        String query = "delete from inventory where itemId > 0";
        String itemName, category;
        int itemId, rows=0, lifespan=0;
        double price, amount=0;

        try{
            pS = connection.prepareStatement(query);
            rows = pS.executeUpdate();
            query = "select * from item";
            pS = connection.prepareStatement(query);
            resultSet = pS.executeQuery();
            /* FOR EACH ITEM IN THE ITEMS TABLE, ADD 1000.00 OF THAT TO INVENTORY */
            while(resultSet.next()){
                itemName = resultSet.getString("iName");
                category = resultSet.getString("category");
                itemId = resultSet.getInt("itemId");
                price = resultSet.getDouble("price");
                /*different categories expire at different rates */
                if(category.equals("dairy")) lifespan = 7;
                if(category.equals("ambient")) lifespan = 14;
                if(category.equals("produce")) lifespan = 7;
                if(category.equals("frozen")) lifespan = 30;
                if(category.equals("canned")) lifespan = 100;
                /* SET EXPIRATION DATE FOR CURRENT ITEM */
                LocalDate expDate = date.plusDays(lifespan);
                System.out.println(expDate);
                amount = 1000.0;
                /* POPULATE INVENTORY */
                query = "insert into inventory values ("+itemId+", '"+expDate+"', "+amount+");";
                pS = connection.prepareStatement(query);
                rows = pS.executeUpdate();
            }
        }
        catch(SQLException e){
            System.out.println(e);
        }


    }

    /* PRINTS THE INFORMATION FROM EACH COLUMN OF EVERY CUSTOMER ROW */
    private void printCustomers() throws SQLException {
        String query = "SELECT * from customer";
        pS = connection.prepareStatement(query);
        resultSet = pS.executeQuery();
        while(resultSet.next()){
            System.out.println(resultSet.getInt(1)+ " " + resultSet.getString(2) + " " + resultSet.getString(3) + " " + resultSet.getString(4) );
        }
    }

    /* ADMIN PORTAL. THIS IS WHERE MOST OF THE WORK IS DONE AND ALL THE OTHER METHODS ARE CALLED. CAN BE THOUGHT OF AS THE PSEUDO MAIN FOR THE ADMIN CLASS.
       THIS IS THE ONLY PUBLIC METHOD, ACTING AS THE BRIDGE BETWEEN MAIN AND ADMIN.
     */
    public void adminPortal() throws SQLException, IOException {
        /* ATTEMPT TO ESTABLISH CONNECTION WITH SERVER, IF FAILS, EXIT */
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
            System.exit(-1); //PRINT OUT PROPERTY VALUES AND EXIT
        }
        /* LOGIN SUCCESSFUL, PRINT ADMIN NAME */
        System.out.println("Logged in as "+name);
        int choice=-1;
        while(choice!=0){ // WHILE(TRUE) SHOULD ALSO WORK BECAUSE OF BREAK
            System.out.println("What do you need today?");
            System.out.println("0: Sign out\n1: Get all registered admin\n2: Remove existing admin\n3: Register new admin\n4: Get all customers\n5: Remove existing customer\n6: Restock shelves");
            choice = scan.nextInt();
            scan.nextLine();
            switch(choice){
                case(0):
                    System.out.println("Goodbye");
                    break;
                case(1):
                    printAdmin();
                    continue;
                case(2):
                    removeAdmin();
                    continue;
                case(3):
                    registerAdmin();
                    continue;
                case(4):
                    printCustomers();
                    continue;
                case(5):
                    removeCustomer();
                    continue;
                case(6):
                    fillInventory();
                    continue;
                default:
                    System.out.println(choice+" is an invalid selection");
            }
        }


    }

    /* REMOVE ENTRY FROM CUSTOMER TABLE BY SPECIFYING THE EMAIL AND CID OF THAT CUSTOMER. DOES NOT DELETE PURCHASES
    * MADE BY THAT CUSTOMER */
    private void removeCustomer(){
        String name=null;
        String email;
        String passWord;
        String deletion;
        boolean found=false;
        int id, result;
        /* GET CUSTOMER EMAIL AND CID */
        System.out.println("Enter id of customer to be deleted");
        id = scan.nextInt();
        scan.nextLine();
        System.out.println("Enter their email");
        email = scan.nextLine();
        String query;
        try {
            /* IF THERE'S A ROW WITH THE ENTERED CID AND EMAIL, THEN THE GIVEN CREDENTIALS ARE VALID */
            query = "select * from customer where cId = "+id+" and email = '"+email+"'";

            pS = connection.prepareStatement(query);
            resultSet = pS.executeQuery();
            while(resultSet.next()){
                name = resultSet.getString("cName");
                found = true;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        /* IF ACCOUNT NOT FOUND RETURN */
        if (!found) {
            System.out.println("Invalid Credentials. Removal unsuccessful.");
            return;
        }
        /* VERIFY THAT THE CUSTOMER SHOULD BE DELETED */
        System.out.println("Remove customer "+name+"? (Press Y or y to confirm)");
        deletion = scan.nextLine();
        deletion = deletion.toUpperCase(Locale.ROOT);
        if(deletion.charAt(0)=='Y'){

            try {
                /* DELETION QUERY THAT LOOKS FOR CUSTOMER BASED ON PRIMARY KEY CID */
                query = "delete from customer where cId = "+id;
                pS = connection.prepareStatement(query);
                result = pS.executeUpdate();
            } catch (Exception e) {
                System.out.println(e);
            }
            /* DELETION CONFIRMED */
            System.out.println("Customer "+name+" deleted");
        }
        else{
            /* NO DELETION */
            System.out.println("Customer "+name+" not deleted");
        }
    }

    /* PRINTS EVERY COLUMN OF ALL THE ENTRIES IN THE ADMIN TABLE */
    private void printAdmin() throws SQLException {
        String query = "SELECT * from admin";
        pS = connection.prepareStatement(query);
        resultSet = pS.executeQuery();
        while(resultSet.next()){
            System.out.println(resultSet.getInt(1)+ " " + resultSet.getString(2) + " " + resultSet.getString(3) + " " + resultSet.getString(4) );
        }

    }

    /* REGISTERS A NEW ADMIN. INTENTIONALLY ONLY AVAILABLE TO ADMIN, ENSURING NO ONE CAN REGISTER THEMSELVES AS AN EMPLOYEE */
    private void registerAdmin(){
        int newId, curId;
        String query;
        /* GET NEW ADMIN INFO */
        System.out.println("Enter name");
        String name = scan.nextLine();
        System.out.println("Create password");
        String passWord = scan.nextLine();
        newId = random.nextInt(1000);
        System.out.println("Enter email");
        String email = scan.nextLine();
        System.out.println(name + " " + passWord + " " + newId);
        try {
            /* ENSURE THAT THE EID IS UNIQUE, IF NOT, INCREMENT UNTIL IT IS */
            query = "select eId from admin order by eId asc";
            pS = connection.prepareStatement(query);
            resultSet = pS.executeQuery();
            while(resultSet.next()){
                curId = resultSet.getInt("eId");
                System.out.println(curId);
                if(curId == newId){
                    newId++;
                }
            }
            /* STATEMENT INSERTING NEW ROW INTO ADMIN WITH THE GIVEN INFORMATION */
            query = "insert into admin values("+newId+",'"+name+"','"+passWord+"','"+email+"');";
            pS = connection.prepareStatement(query);
            pS.executeUpdate();
            System.out.println("NEW ADMIN:");
            query = "select * from admin where eId = " + newId;
            pS = connection.prepareStatement(query);
            resultSet = pS.executeQuery();
            while(resultSet.next()){
                System.out.println(resultSet.getInt(1)+ " " + resultSet.getString(2) + " " + resultSet.getString(3) + " " + resultSet.getString(4) );
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /* METHOD TO REMOVE ADMIN BY LOOKING FOR EID AND PASSWORD */
    private void removeAdmin(){
        String name=null;
        String email;
        String passWord;
        String deletion;
        boolean found=false;
        int id, result;
        /* GET ADMIN INFO */
        System.out.println("Enter id of admin to be deleted");
        id = scan.nextInt();
        scan.nextLine();
        System.out.println("Enter their password");
        passWord = scan.nextLine();
        String query;
        try {
            /* IF THERE'S A ROW WITH THE ENTERED EID AND PASSWORD, THEN THE GIVEN CREDENTIALS ARE VALID */
            query = "select * from admin where eId = "+id+" and epassWord = '"+passWord+"'";

            pS = connection.prepareStatement(query);
            resultSet = pS.executeQuery();
            while(resultSet.next()){
                name = resultSet.getString("eName");
                found = true;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        /* IF ADMIN NOT FOUND RETURN */
        if (!found) {
            System.out.println("Invalid Credentials. Removal unsuccessful.");
            return;
        }
        /* VERIFY REMOVAL */
        System.out.println("Remove admin "+name+"? (Press Y or y to confirm)");
        deletion = scan.nextLine();
        deletion = deletion.toUpperCase(Locale.ROOT);
        if(deletion.charAt(0)=='Y'){

            try {
                query = "delete from admin where eId = "+id;
                pS = connection.prepareStatement(query);
                result = pS.executeUpdate();
            } catch (Exception e) {
                System.out.println(e);
            }
            /* CONFIRM REMOVAL */
            System.out.println("Admin "+name+" deleted");
        }
        else{
            /* CONFIRM NO REMOVAL */
            System.out.println("Admin "+name+" not deleted");
        }
    }

}
