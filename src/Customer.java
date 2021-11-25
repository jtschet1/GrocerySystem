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

public class Customer {
    /* CUSTOMER VARIABLES, EACH CORRESPONDING TO A COLUMN IN THE CUSTOMER TABLE */
    String name;
    String email;
    int cId;
    String passWord;

    /* DECLARE INSTANCE VARS RELATED TO JDBC WORK */
    private static PreparedStatement pS = null;
    private static ResultSet resultSet = null;
    private static LocalDate date = LocalDate.now();
    private static Connection connection = null;
    /* INSTANCE SCANNER AND RANDOM OBJECTS */
    private static Random random = new Random();
    private static final Scanner scan = new Scanner(System.in);

    /* CUSTOMER CONSTRUCTOR ASSIGNS VARIABLES TO THE VALUES GOTTEN FROM THE ROW FOUND IN CUSTOMER LOGIN METHOD IN MAIN */
    public Customer(String name, String email, String passWord, int cId){
        this.name = name;
        this.email = email;
        this.cId =  cId;
        this.passWord = passWord;

    }

    /* CUSTOMER PORTAL. THIS IS WHERE MOST OF THE WORK IS DONE AND ALL THE OTHER METHODS ARE CALLED. CAN BE THOUGHT OF AS THE PSEUDO MAIN FOR THE CUSTOMER CLASS.
       THIS IS THE ONLY PUBLIC METHOD, ACTING AS THE BRIDGE BETWEEN MAIN AND CUSTOMER.
     */
    public void customerPortal() throws SQLException, IOException {
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
        /* LOG IN SUCCESSFUL, PRINT OUT CUSTOMER OPTIONS */
        System.out.println("Logged in as "+name);
        int choice=-1;
        while(choice!=0){//WHILE(TRUE) SHOULD ALSO WORK BECAUSE OF BREAK
            System.out.println("What do you need today?");
            System.out.println("0: Sign out\n1: Order groceries\n2: View History\n3: Change personal information\n4: Remove Account");
            choice = scan.nextInt();
            scan.nextLine();
            switch(choice){
                case(0):
                    System.out.println("Goodbye");
                    break;
                case(1):
                    placeOrder();
                    continue;
                case(2):
                    viewHistory();
                    continue;
                case(3):
                    updateInfo();
                    continue;
                case(4):
                    deleteAccount();
                    break;
                default:
                    System.out.println(choice+" is an invalid selection");
            }
        }
    }

    /* ALLOWS CUSTOMER TO REVIEW ALL THEIR PAST PURCHASES. LISTS ALL PURCHASES IN ORDER OF MOST TO LEAST RECENT AND PRINTS OUT
       ITEMIZED RECEIPTS AT THE USER'S REQUEST */
    private void viewHistory() throws SQLException{
        int purchaseId=-1;
        double cost=-1;
        boolean found=false;
        date = LocalDate.now();//MAKE SURE DATE IS CURRENT DATE
        while(purchaseId != 0) {
            found=false;
            /* GET ENTIRE ROW FOR PURCHASES UNDER THAT CUSTOMER. ORDER BY PURCHASEDATE FROM MOST TO LEAST RECENT */
            String query = "select * from purchase where cId = "+this.cId + " order by purchaseDate desc";
            pS = connection.prepareStatement(query);
            resultSet = pS.executeQuery();
            if(purchaseId==0) break; // 0 TO EXIT. WILL NOT EXIT THE FIRST TIME BECAUSE PURCHASEID SET TO -1
            while (resultSet.next()) {
                /* PRINT OUT EACH PURCHASE INFO */
                System.out.println("Purchase Id: " + resultSet.getInt("purchaseId") + " Date: " + resultSet.getDate("purchaseDate") + " Total: " + resultSet.getDouble("total"));
            }
            /* ASK USER FOR PURCHASE THEY WANT TO SEE THE ITEMIZED RECEIPT FOR */
            System.out.println("Enter purchase Id of purchase you would like to inspect. Enter 0 to exit");
            purchaseId = scan.nextInt();

            /* INNER JOIN OF PURCHASE DETAILS AND PURCHASE ON PURCHASEID, AND ITEMS ON ITEMID.
               GIVEN THE PURCHASEID SPECIFIED BY THE CUSTOMER,
               IF THERE IS A PURCHASE WITH THAT PURCHASEDID MADE BY THAT CUSTOMER (EVIDENT BY CID),
               SELECT OUT ITEM NAMES, CATEGORY, QUANTITY PURCHASED AND COST OF EACH ITEM IN THAT PURCHASE
            */
            query = "select item.iName, item.category, purchasedetail.quantityPurchased, purchasedetail.cost from purchasedetail inner join item on purchaseDetail.itemId = item.itemId inner join purchase on purchase.purchaseId = purchasedetail.purchaseId where purchasedetail.purchaseId = " + purchaseId + " and purchase.cId = "+this.cId + " and purchase.purchaseId = purchasedetail.purchaseId;";
            pS = connection.prepareStatement(query);
            resultSet = pS.executeQuery();
            while (resultSet.next()) {
                /* IF PURCHASE FOUND, SET FOUND AND PRINT RECEIPT TITLE */
                if(!found){
                    found=true;
                    System.out.println("Itemized receipt:");
                }
                /* GET THE INFO OF EACH ITEM AND PRINT IT FOR CUSTOMER TO REVIEW. PRODUCE CHARGED PER POUND AND NON PRODUCE CHARGEED PER ITEM */
                if (!resultSet.getString("category").equals("produce")) {
                    System.out.println(resultSet.getDouble("quantityPurchased") + " " + resultSet.getString("iName") + " for " + resultSet.getDouble("cost") + " each.");
                } else {
                    System.out.println(resultSet.getDouble("quantityPurchased") + " " + resultSet.getString("iName") + " for " + resultSet.getDouble("cost") + " per pound.");
                }
            }
            /* IF NO PURCHASE EXISTS WITH THAT PURCHASEID MADE BY THAT CUSTOMER */
            if(!found){
                System.out.println("Purchase not found");
            }
        }
    }

    /* METHOD HANDLING CUSTOMER'S ABILITY TO PURCHASE ITEMS. STARTS BY GETTING EVERY ITEM IN STOCK AND PRINTING IT OUT
       FOR THE USER. THE USER THEN SELECT WHICH ITEMS AND HOW MUCH THEY WANT TO PURCHASE. AFTER CONFIRMING THE PURCHASE,
       THE NECESSARY TABLES ARE UPDATED.
     */
    private void placeOrder() throws SQLException{
        int itemId=0, purchaseId, curId, confirm;
        double total=0, cost=0, quantity=0;
        date = LocalDate.now();
        boolean found=false;
        /* GENERATE PURCHASE ID */
        purchaseId = random.nextInt(10000);
        String query=null;
        String choice=null;
        String cat=null;
        try {
            /* make sure purchase Id is unique */
            query = "select purchaseId from purchase";
            pS = connection.prepareStatement(query);
            resultSet = pS.executeQuery();
            while(resultSet.next()){
                curId = resultSet.getInt(1);
                /* IF ID IS NOT UNIQUE, INCREMENT UNTIL IT IS */
                if(curId == purchaseId){
                    purchaseId++;
                }
            }
            do {
                /* QUERY TO SELECT INVENTORY ITEMS THAT ARE IN STOCK (AMOUNT > 0) */
                query = "select * from item x where exists ( select * from inventory z where z.itemId = x.itemId and z.amount > 0 );";
                pS = connection.prepareStatement(query);
                resultSet = pS.executeQuery();
                while (resultSet.next()) {
                    /* PRINT LIST OF ITEMS AND THEIR PRICES */
                    if(resultSet.getString("category").equals("produce")) {
                        System.out.println(resultSet.getString("iName") + " - $" + resultSet.getDouble("price") + "/lb.");
                    }
                    else{
                        System.out.println(resultSet.getString("iName") + " - S" + resultSet.getDouble("price") + " each.");
                    }
                }
                /* ASK USER FOR WHAT ITEMS THEY WANT AND HOW MUCH OF IT. IF DONE, CONTINUE TO NEXT STEP */
                System.out.println("Enter desired item (If done shopping, enter done)");
                choice = scan.nextLine();
                if(choice.toUpperCase(Locale.ROOT).equals("DONE")) {
                    choice = choice.toUpperCase(Locale.ROOT);
                    continue;
                }
                System.out.println("Enter desired quantity");
                quantity = scan.nextDouble();
                scan.nextLine();
                /* DETERMINE IF ITEM IS PRODUCE OR NOT */
                query = "select category from item where iName = '"+choice+"'";
                pS = connection.prepareStatement(query);
                resultSet = pS.executeQuery();
                while(resultSet.next()){
                    cat = resultSet.getString("category");
                }
                /* QUANTITY HAS TO BE A WHOLE NUMBER IF THE ITEM IS NOT PRODUCE */
                if(cat.equals("produce") && quantity == Math.floor(quantity)){
                    System.out.println("Can only be purchased by a whole number quantity");
                    continue;
                }
                /* JOIN INVENTORY AND ITEM ON ITEMID. GET ALL COLUMNS FOR THE
                   ITEM THE USER SPECIFIED IF THE EXISTING AMOUNT IN INVENTORY
                   IS GREATER THAN OR EQUAL TO THE AMOUNT REQUESTED BY CUSTOMER
                 */
                query = "select * from item x join inventory z on z.itemId = x.itemId where x.iName = '"+choice+"' and z.amount >= "+quantity+";";
                pS = connection.prepareStatement(query);
                resultSet = pS.executeQuery();
                while(resultSet.next()){
                    found=true;
                    itemId = resultSet.getInt("itemId");
                    System.out.println(itemId);
                    cost = resultSet.getDouble("price");
                    total += cost * quantity;
                }
                /* THERE IS NO ITEM OF THAT NAME WITH THE REQUIRED QUANTITY */
                if(!found){
                    System.out.println("Item "+choice+" not found");
                    continue;
                }
                /* INSERT A ROW INTO PURCHASE DETAIL RECORDING THE AMOUNT OF THE ITEM PURCHASED BY THE CUSTOMER
                   ORDER HAS NOT BEEN FINALIZED SO THE STOCK HAS NOT BEEN REMOVED FROM INVENTORY */
                query = "insert into purchasedetail values ("+purchaseId+", "+itemId+", "+cost+", "+quantity+");";
                pS = connection.prepareStatement(query);
                pS.executeUpdate();
            }while(!choice.equals("DONE")); //CUSTOMER HAS SELECTED ALL DESIRED ITEMS, SO EXIT
            /* JOIN ITEM AND PURCHASEDETAIL ON ITEMID
               SELECT ITEM NAME, CATEGORY, AMOUNT PURCHASED AND COST FOR EVERY SELECTION WHERE THE PURCHASEID IS THE CURRENT PURCHASE
                */
            System.out.println("Purchase Review:");
            query = "select item.iName, item.category, purchasedetail.quantityPurchased, purchasedetail.cost from purchasedetail inner join item on purchaseDetail.itemId = item.itemId where purchasedetail.purchaseId = "+purchaseId;
            pS = connection.prepareStatement(query);
            resultSet = pS.executeQuery();
            while(resultSet.next()){
                /* PRINT OUT ALL THE PURCHASEDETAILS OF THE PURCHASE FOR THE CUSTOMER TO REVIEW BEFORE FINALIZING */
                if(!resultSet.getString("category").equals("produce")) {
                    System.out.println(resultSet.getDouble("quantityPurchased") + " " + resultSet.getString("iName") + " for " + resultSet.getDouble("cost") + " each.");
                }
                else{
                    System.out.println(resultSet.getDouble("quantityPurchased")+" "+resultSet.getString("iName")+" for "+resultSet.getDouble("cost")+" per pound.");
                }
            }
            /* ASK USER TO CONFIRM PURCHASE WITH 1, OR SCRAP THE PURCAHSE WITH 2. ANY OTHER INTEGER RESULTS IN ASKING AGAIN */
            while(true) {
                System.out.println("1 - confrim purchase\n2 - deny purchase");
                confirm = scan.nextInt();
                scan.nextLine();
                if (confirm == 2) {
                    System.out.println("Purchase not made");
                    query = "delete from purchasedetail where purchaseId = "+purchaseId;
                    pS = connection.prepareStatement(query);
                    pS.executeUpdate();
                    return;
                }
                if(confirm == 1) break;
            }
            /* CREATE A RECORD FOR THE PURCHASE, SUMMARIZING THE DETAILS WITH THE PURCAHSE ID, CUSTOMER ID, TOTAL, AND DATE OF PURCHASE */
            query = "insert into purchase values("+purchaseId+", "+this.cId+", "+total+", '"+date+"');";
            pS = connection.prepareStatement(query);
            pS.executeUpdate();
            System.out.println("Purchased completed");
            /* INNER JOIN PURCHASE DETAIL AND INVENTORY ON ITEMID
               SUBTRACT AMOUNT PURCHASED BY CUSTOMER FROM EACH ITEM IF
               THAT ITEM IS FOUND IN THE PURCHASE DETAILS WITH THE CURRENT PURCHASE ID*/
            query = "update inventory inner join purchasedetail on inventory.itemId = purchasedetail.itemId set inventory.amount = inventory.amount - purchasedetail.quantityPurchased where inventory.itemId = purchasedetail.itemId and purchasedetail.purchaseId = "+purchaseId+";";
            pS = connection.prepareStatement(query);
            /* REMOVES PURCHASED ITEMS FROM INVENTORY */
            int rows = pS.executeUpdate();
        } catch (Exception e) {
            System.out.println(e);
        }


    }

    /* METHOD THAT ALLOWS CUSTOMER TO CHANGE THE INFORMATION ASSOCIATED WITH THEIR ACCOUNT,
       OTHER THAN THEIR CUSTOMER ID, WHICH WAS ASSIGNED TO THEM
     */
    private void updateInfo()throws SQLException {
        System.out.println("Enter new desired name");
        this.name = scan.nextLine();
        System.out.println("Enter new desired email");
        this.email = scan.nextLine();
        System.out.println("Enter new desired password");
        this.passWord = scan.nextLine();
        int result=0;
        /* UPDATING THE NAME, PASSWORD AND EMAIL IN THE ROW WHERE CID IS THE CURRENT CUSTOMER */
        String query = "update customer set cName='"+this.name+"', cpassWord='"+this.passWord+"', email='"+this.email+"' where cId="+this.cId;
        try{
            pS = connection.prepareStatement(query);
            result=pS.executeUpdate();
            System.out.println("Update successful");

        }
        catch(SQLException e){
            System.out.println("Update unsuccessful");
        }
    }

    /* DELETE THE ACCOUNT OF THE CURRENT USER */
    private void deleteAccount(){
        boolean found=false;
        String deletion=null;
        int result;
        String query;
        /* MAKE USER VERIFY AGAIN, AS THIS CANNOT BE UNDONE */
        System.out.println(this.name+", are you sure you want to delete your account? (Press Y or y to confirm)");
        deletion = scan.nextLine();
        deletion = deletion.toUpperCase(Locale.ROOT);
        if(deletion.charAt(0)=='Y'){
            try {
                query = "delete from customer where cId = "+this.cId;
                pS = connection.prepareStatement(query);
                result = pS.executeUpdate();
            } catch (Exception e) {
                System.out.println(e);
            }
            System.out.println(this.name+", your account has been deleted");
        }
        else{
            System.out.println(this.name+", your account has not been deleted");
        }
    }

}
