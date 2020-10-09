package Task1;

import com.sun.rowset.JdbcRowSetImpl;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import javax.sql.rowset.JdbcRowSet;

import static org.junit.Assert.*;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;

public class Task1 {
    private static final String url = "jdbc:mysql://192.168.14.73:3306/challange?useUnicode=true&serverTimezone=UTC";
    private static final String user = "root";
    private static final String password = "root";

    private static Connection con;
    private static Statement stmt;
    private static JdbcRowSet jdbcRs;

    @Before
    public void init() throws SQLException {
        con = getNewConnection();
        //createCustomerTable();
    }

    @After
    public void close() throws SQLException {
        con.close();
        jdbcRs.close();
    }

    private Connection getNewConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private int executeUpdate(String query) throws SQLException {
        stmt = con.createStatement();
        // Для Insert, Update, Delete
        int result = stmt.executeUpdate(query);
        return result;
    }

    private void createCustomerTable() throws SQLException {
        String customerTableQuery = "CREATE TABLE `mkoval` (`id` int(15) NOT NULL, `name` varchar(60) DEFAULT NULL, `surname` varchar(60) DEFAULT NULL, `age` int(5) DEFAULT NULL, `birthdate` date DEFAULT NULL, PRIMARY KEY (`id`))";
        String customerEntryQuery1 = "INSERT INTO `mkoval` (`id`, `name`, `surname`, `age`, `birthdate`) VALUES (1, 'Philipp', 'Kirkorov', 53, '1967-04-30')";
        String customerEntryQuery2 = "INSERT INTO `mkoval` (`id`, `name`, `surname`, `age`, `birthdate`) VALUES (2, 'Nikolay', 'Baskov', 43, '1976-10-15')";
        executeUpdate(customerTableQuery);
        executeUpdate(customerEntryQuery1);
        executeUpdate(customerEntryQuery2);
    }

    @Test
    public void checkRowsNotSame() throws SQLException {
        jdbcRs = new JdbcRowSetImpl(con);
        jdbcRs.setCommand("SELECT * FROM mkoval");
        jdbcRs.execute();
        jdbcRs.next();

        int id1 = jdbcRs.getInt("id");
        String name1 = jdbcRs.getString("name");
        String surname1 = jdbcRs.getString("surname");
        int age1 = jdbcRs.getInt("age");
        Date date1 = jdbcRs.getDate("birthdate");

        jdbcRs.next();

        int id2 = jdbcRs.getInt("id");
        String name2 = jdbcRs.getString("name");
        String surname2 = jdbcRs.getString("surname");
        int age2 = jdbcRs.getInt("age");
        Date date2 = jdbcRs.getDate("birthdate");

        assertNotSame(id1, id2);
        assertNotSame(name1, name2);
        assertNotSame(surname1, surname2);
        assertNotSame(age1, age2);
        assertNotSame(date1, date2);
    }

    @Test
    public void createJsonAndWriteToFile() throws SQLException {
        jdbcRs = new JdbcRowSetImpl(con);
        jdbcRs.setCommand("SELECT * FROM mkoval");
        jdbcRs.execute();

        JSONObject jsonObject = new JSONObject();
        JSONArray array = new JSONArray();

        while (jdbcRs.next()) {
            JSONObject record = new JSONObject();
            record.put("ID", jdbcRs.getInt("id"));
            record.put("First_Name", jdbcRs.getString("name"));
            record.put("Last_Name", jdbcRs.getString("surname"));
            record.put("Date_Of_Birth", jdbcRs.getInt("age"));
            record.put("Place_Of_Birth", jdbcRs.getDate("birthdate"));
            array.add(record);
        }

        jsonObject.put("Singers_data", array);
        try {
            FileWriter file = new FileWriter("/Rest-Assured/src/test/java/Task1/output.json");
            file.write(jsonObject.toJSONString());
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("JSON file created!");
    }
}