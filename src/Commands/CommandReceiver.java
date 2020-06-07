package Commands;

import BasicClasses.Person;
import BasicClasses.StudyGroup;
import Collection.CollectionManager;
import Collection.CollectionUtils;
import Commands.SerializedCommands.*;
import Exceptions.DatabaseException;
import Utils.Database.DatabaseManagerImpl;
import Utils.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Ресивер(получатель), отправляет серилизованные объекты на сервер.
 */
public class CommandReceiver {
    private final Socket socket;
    private static final Logger logger = LoggerFactory.getLogger(CommandReceiver.class);
    private CollectionManager collectionManager = CollectionManager.getCollectionManager();
    private DatabaseManagerImpl databaseManager = new DatabaseManagerImpl();
    private final String login;
    private final String password;

    public CommandReceiver(Socket socket, String login, String password) {
        this.socket = socket;
        this.login = login;
        this.password = password;
    }

    private boolean checkUser() throws DatabaseException, IOException {
        boolean exist = databaseManager.validateUserData(login, password);

        if (exist) {
            logger.info(String.format("Пользователь %s:%s, живущий по адресу %s:%s - прошел проверку на реального ИТМОшника", login, password, socket.getInetAddress(), socket.getPort()));
            return true;
        } else {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(new SerializedMessage("Дядя, ты не зарегистрирован в нашем гей-кружке, проваливай отсюда!"));
            logger.info(String.format("Товарищ %s:%s ошибся дверью, клуб кожевного ремесла два блока вниз.", socket.getInetAddress(), socket.getPort()));
        }

        return false;
    }

    public void info() throws IOException, DatabaseException {
        if (checkUser()) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            out.writeObject(new SerializedMessage(collectionManager.getInfo()));
            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды INFO", socket.getInetAddress(), socket.getPort()));
        }
    }

    public void show() throws IOException, DatabaseException {
        if (checkUser()) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            out.writeObject(new SerializedMessage(collectionManager.show()));
            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды SHOW", socket.getInetAddress(), socket.getPort()));
        }
    }

    public void add(Object o) throws IOException, DatabaseException {
        if (checkUser()) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            try {
                StudyGroup studyGroup = (StudyGroup) o;
                studyGroup.setId(databaseManager.addElement(studyGroup, login));
                collectionManager.add(studyGroup);
                out.writeObject(new SerializedMessage("Элемент добавлен в коллекцию."));
            } catch (Exception e){
                e.printStackTrace();
                out.writeObject(new SerializedMessage("Полученный элемент не добавлен: " + e));
            }

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды ADD", socket.getInetAddress(), socket.getPort()));
        }
    }

    /**
     *
     * @param ID - апдейт элемента по ID.
     */
    public void update(String ID, StudyGroup studyGroup) throws IOException, DatabaseException {
        if (checkUser()) {
            Integer groupId;
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            try {
                groupId = Integer.parseInt(ID);
                if (CollectionUtils.checkExist(groupId)) {
                    if (Validator.validateStudyGroup(studyGroup)) {
                        collectionManager.update(studyGroup, groupId);
                        out.writeObject(new SerializedMessage("Команда update выполнена."));
                    } else {
                        out.writeObject(new SerializedMessage("Полученный элемент не прошел валидацию на стороне сервера."));
                    }
                } else {
                    out.writeObject(new SerializedMessage("Элемента с таким ID нет в коллекции."));
                }
            } catch (NumberFormatException e) {
                out.writeObject(new SerializedMessage("Команда не выполнена. Вы ввели некорректный аргумент."));
            }

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды UPDATE", socket.getInetAddress(), socket.getPort()));
        }
    }

    /**
     *
     * @param ID - удаление по ID.
     */
    public void removeById(String ID) throws IOException, DatabaseException {
        if (checkUser()) {
            Integer groupId;
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            try {
                groupId = Integer.parseInt(ID);
                if (CollectionUtils.checkExist(groupId)) {
                    collectionManager.removeById(groupId);
                    out.writeObject(new SerializedMessage("Элемент с ID " + groupId + " успешно удален из коллекции."));
                } else {
                    out.writeObject(new SerializedMessage("Элемента с таким ID нет в коллекции."));
                }
            } catch (NumberFormatException e) {
                out.writeObject(new SerializedMessage("Команда не выполнена. Вы ввели некорректный аргумент."));
            }

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды REMOVE_BY_ID", socket.getInetAddress(), socket.getPort()));
        }
    }

    public void clear() throws IOException, DatabaseException {
        if (checkUser()) {
            collectionManager.clear();
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            out.writeObject(new SerializedMessage("Коллекция успешно очищена."));
            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды CLEAR", socket.getInetAddress(), socket.getPort()));
        }
    }

    public void head() throws IOException, DatabaseException {
        if (checkUser()) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            out.writeObject(new SerializedMessage(collectionManager.head()));
            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды HEAD", socket.getInetAddress(), socket.getPort()));
        }
    }

    public void removeGreater(StudyGroup studyGroup) throws IOException, DatabaseException {
        if (checkUser()) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            if (Validator.validateStudyGroup(studyGroup)) {
                out.writeObject(new SerializedMessage(collectionManager.removeGreater(studyGroup)));
            } else {
                out.writeObject(new SerializedMessage("Полученный элемент не прошел валидацию на стороне сервера."));
            }

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды REMOVE_GREATER", socket.getInetAddress(), socket.getPort()));
        }
    }

    public void removeLower(StudyGroup studyGroup) throws IOException, DatabaseException {
        if (checkUser()) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            if (Validator.validateStudyGroup(studyGroup)) {
                out.writeObject(new SerializedMessage(collectionManager.removeLower(studyGroup)));
            } else {
                out.writeObject(new SerializedMessage("Полученный элемент не прошел валидацию на стороне сервера."));
            }

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды REMOVE_LOWER", socket.getInetAddress(), socket.getPort()));
        }
    }

    public void minBySemesterEnum() throws IOException, DatabaseException {
        if (checkUser()) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            out.writeObject(new SerializedMessage(collectionManager.minBySemesterEnum()));
            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды MIN_BY_SEMESTER_ENUM", socket.getInetAddress(), socket.getPort()));
        }
    }

    public  void maxByGroupAdmin() throws IOException, DatabaseException {
        if (checkUser()) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            out.writeObject(new SerializedMessage(collectionManager.maxByGroupAdmin()));
            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды MAX_BY_GROUP_ADMIN", socket.getInetAddress(), socket.getPort()));
        }
    }

    public void countByGroupAdmin(Person groupAdmin) throws IOException, DatabaseException {
        if (checkUser()) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            if (Validator.validatePerson(groupAdmin)) {
                out.writeObject(new SerializedMessage(collectionManager.countByGroupAdmin(groupAdmin)));
            } else {
                out.writeObject(new SerializedMessage("Полученный элемент не прошел валидацию на стороне сервера."));
            }

            logger.info(String.format("Клиенту %s:%s отправлен результат работы команды COUNT_BY_GROUP_ADMIN", socket.getInetAddress(), socket.getPort()));
        }
    }

    public void register(String login, String password) throws IOException, InterruptedException, ClassNotFoundException, DatabaseException {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

        if (!databaseManager.doesUserExist(login)) {
            databaseManager.addUser(login, password);
            out.writeObject(new SerializedMessage("Пользователь с логином " + login + " успешно создан!"));
            logger.info(String.format("Пользователь %s успешно зарегистрирован!", login));
        } else { out.writeObject(new SerializedMessage("Пользователь с таким логином уже существует!")); }
        logger.info(String.format("Клиенту %s:%s отправлен результат попытки регистрации", socket.getInetAddress(), socket.getPort()));
    }
}
