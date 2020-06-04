package Utils.Database;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;

import org.apache.ibatis.jdbc.ScriptRunner;

public class DatabaseManagerImpl {
    private final String url = "jdbc:postgresql://localhost:5432/postgres";
    private final String user = "postgres";
    private final String password = "...";
    private Connection connection;
    private Statement statement;
    public DatabaseManagerImpl() throws SQLException {
        connection = DriverManager.getConnection(url, user, password);
        statement = connection.createStatement();

        buildTables();
    }

    private void buildTables(){
        ScriptRunner sr = new ScriptRunner(connection);
        try {
            Files.walk(Paths.get("src/Utils/Database/Sql"))
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        Reader reader = null;
                        try {
                            reader = new BufferedReader(new FileReader(String.valueOf(path)));
                            sr.runScript(reader);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
