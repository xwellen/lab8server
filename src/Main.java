import ServerSocket.Controller;
//import Utils.JSON.ParserJson;
import Utils.Database.*;
import java.io.IOException;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws IOException {
        try {
//            Runtime.getRuntime().addShutdownHook(new Thread(ParserJson::collectionToJson));
            Controller controller = new Controller();
            controller.run(args[0]);
//            try {
//                DatabaseManagerImpl databaseManager = new DatabaseManagerImpl(args[1]);
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.println("Введено некорректное количество аргументов.\n" +
                    "Требуются 2 аргумента: порт, пароль к бд");
        }
    }
}
