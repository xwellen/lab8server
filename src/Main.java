import ServerSocket.Controller;
//import Utils.JSON.ParserJson;
import Utils.Database.*;
import java.io.IOException;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws IOException {
////        Runtime.getRuntime().addShutdownHook(new Thread(ParserJson::collectionToJson));
//        Controller controller = new Controller();
//        controller.run(args[0]);
        try {
            DatabaseManagerImpl databaseManager = new DatabaseManagerImpl();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
