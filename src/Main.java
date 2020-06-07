import ServerSocket.Controller;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        try {
            Controller controller = new Controller();
            controller.run(args[0]);
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.println("Введено некорректное количество аргументов.\n" +
                    "Требуются 1 аргумент: порт");
        }
    }
}
