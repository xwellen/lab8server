package Utils.Database;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import BasicClasses.*;
import Collection.CollectionManager;
import Exceptions.DatabaseException;

import Interfaces.SqlConsumer;
import Interfaces.SqlFunction;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Commands.Utils.HashEncrypter;

public class DatabaseManagerImpl {
    private final String url = "jdbc:postgresql://localhost:5432/ITMO";
    private final String user = "postgres";
    private final String password = "sasha";
    private final String salty = "Pozhalyista_Postavte_10_ballov";
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManagerImpl.class);

    public DatabaseManagerImpl() {
        try {
            Class.forName("org.postgresql.Driver");
            logger.info("Драйвер подключён");
        } catch (ClassNotFoundException e) {
            logger.error("Драйвер не подключён");
            e.printStackTrace();
            System.exit(1);
        }
//        buildTables();
    }

    private void buildTables(){
        try {
            Connection connection = DriverManager.getConnection(url, user, password);
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Добавляет элемент в базу данных.
     *
     * @param studyGroup  добавляемый элемент
     * @param username имя пользователя (создателя элемента)
     * @return назначенный базой данных id для этого элемента
     * @throws DatabaseException если что-то пошло не так при работе с базой данных
     */
    public Integer addElement(StudyGroup studyGroup, String username) throws DatabaseException {
        return this.<Integer>handleQuery((Connection connection) -> {

            Person groupAdmin = studyGroup.getGroupAdmin();
            String addPersonSql = "INSERT INTO person (person_name, height, eye_color, hair_color, nationality)" +
                    "VALUES (?, ?, ?::color, ?::color, ?::country)";

            PreparedStatement groupAdminStatement = connection.prepareStatement(addPersonSql, Statement.RETURN_GENERATED_KEYS);
            groupAdminStatement.setString(1, groupAdmin.getName());
            groupAdminStatement.setInt(2, groupAdmin.getHeight());
            groupAdminStatement.setString(3, groupAdmin.getEyeColor().name());
            groupAdminStatement.setString(4, groupAdmin.getHairColor().name());
            groupAdminStatement.setString(5, groupAdmin.getNationality().name());
            groupAdminStatement.executeUpdate();

            ResultSet rs = groupAdminStatement.getGeneratedKeys();
            if (rs.next()) {
                String addElementSql = "INSERT INTO studyGroup (studyGroup_name, coordinateX, coordinateY, creationDate, " +
                        "studentsCount, formOfEducation, semester, person_id, user_id)" +
                        " SELECT ?, ?, ?, ?, ?, ?::formOfEducation, ?::semester, ?, id" +
                        " FROM \"user\"" +
                        " WHERE \"user\".login = ?";

                PreparedStatement elementStatement = connection.prepareStatement(addElementSql, Statement.RETURN_GENERATED_KEYS);
                Coordinates coordinates = studyGroup.getCoordinates();
                elementStatement.setString(1, studyGroup.getName());
                elementStatement.setInt(2, coordinates.getX());
                elementStatement.setFloat(3, coordinates.getY());
                elementStatement.setTimestamp(4, Timestamp.valueOf(studyGroup.getCreationDate().toLocalDateTime()));
                elementStatement.setInt(5, studyGroup.getStudentsCount());
                elementStatement.setString(6, studyGroup.getFormOfEducation().name());
                elementStatement.setString(7, studyGroup.getSemesterEnum().name());
                elementStatement.setInt(8, rs.getInt(1));
                elementStatement.setString(9, username);

                elementStatement.executeUpdate();
                ResultSet result = elementStatement.getGeneratedKeys();
                result.next();

                logger.info("В коллекцию добавлен элемент");
                return result.getInt(1);
            } else {
                throw new SQLException("Creating user failed, no ID obtained.");
            }
        });
    }

    /**
     * Обработка запроса без возврата значения.
     *
     * @param queryBody тело запроса (Consumer)
     * @throws DatabaseException если что-то пошло не так при работе с базой данных
     */
    private void handleQuery(SqlConsumer<Connection> queryBody) throws DatabaseException {
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            queryBody.accept(connection);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при работе с базой данных: " + e.getMessage());
        }
    }

    /**
     * Обработка запроса с возвратом значения.
     *
     * @param queryBody тело запроса (Function)
     * @param <T>       тип возвращаемого значения
     * @return запрошенное у базы данных значение
     * @throws DatabaseException если что-то пошло не так при работе с базой данных
     */
    private <T> T handleQuery(SqlFunction<Connection, T> queryBody) throws DatabaseException {
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            return queryBody.apply(connection);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при работе с базой данных: " + e.getMessage());
        }
    }

    /**
     * Удаляет элемент из базы данных по id.
     *
     * @param id       id удаляемого элемента
     * @param username пользователь, который пытается удалить элемент
     * @return true, если успешно; false, если нет
     * @throws DatabaseException если что-то пошло не так при работе с базой данных
     */
    public boolean removeById(int id, String username) throws DatabaseException {
        return handleQuery((Connection connection) -> {
            String query =
                    "DELETE from person" +
                            " USING \"user\", studyGroup" +
                            " WHERE person.person_id = studyGroup.person_id AND studyGroup.studyGroup_id = ? " +
                            "AND studyGroup.user_id = \"user\".id AND \"user\".login = ?;";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, id);
            statement.setString(2, username);

            int rowsDeleted = statement.executeUpdate();
            return rowsDeleted > 0;
        });
    }

    /**
     * Обновляет элемент базы данных с указанным id.
     *
     * @param studyGroup  новый элемент
     * @param id       id обновляемого элемента
     * @param username пользователь, который пытается обновить элемент
     * @return true, если успешно; false, если нет
     * @throws DatabaseException если что-то пошло не так при работе с базой данных
     */
    public boolean updateById(StudyGroup studyGroup, int id, String username) throws DatabaseException {
        return handleQuery((Connection connection) -> {
            connection.createStatement().execute("BEGIN TRANSACTION;");
            String query =
                    "UPDATE studyGroup" +
                            " SET studyGroup_name = ?," +
                            "coordinateX = ?," +
                            "coordinateY = ?," +
                            "creationDate = ?," +
                            "studentsCount = ?," +
                            "formOfEducation = ?::formOfEducation" +
                            "semester = ?::semester" +
                            " FROM \"user\"" +
                            " WHERE studyGroup.studyGroup_id = ? AND studyGroup.user_id = \"user\".id AND \"user\".login = ?;" +
                            "UPDATE person" +
                            " SET person_name = ?," +
                            "height = ?," +
                            "eye_color = ?::color," +
                            "hair_color = ?::color," +
                            "nationality = ?::country" +
                            "FROM studyGroup, \"user\"" +
                            " WHERE studyGroup.person_id = person.person_id and studyGroup.studyGroup_id = ? AND studyGroup.user_id = \"user\".id AND \"user\".login = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, studyGroup.getName());
            statement.setInt(2, studyGroup.getCoordinates().getX());
            statement.setFloat(3, studyGroup.getCoordinates().getY());
            statement.setTimestamp(4, Timestamp.valueOf(studyGroup.getCreationDate().toLocalDateTime()));
            statement.setInt(5, studyGroup.getStudentsCount());
            statement.setString(6, studyGroup.getFormOfEducation().name());
            statement.setString(7, studyGroup.getSemesterEnum().name());
            statement.setInt(8, id);
            statement.setString(9, username);

            Person groupAdmin = studyGroup.getGroupAdmin();
            statement.setString(11, groupAdmin.getName());
            statement.setInt(12, groupAdmin.getHeight());
            statement.setString(13, groupAdmin.getEyeColor().name());
            statement.setString(14, groupAdmin.getHairColor().name());
            statement.setString(15, groupAdmin.getNationality().name());

            statement.setInt(16, id);

            statement.setString(17, username);

            int result = statement.executeUpdate();

            connection.createStatement().execute("COMMIT;");

            return result > 0; // Если true, значит результат не пустой и записи обновлены
        });
    }

    /**
     * Загружает коллекцию из базы данных.
     *
     * @param collectionManager куда загрузить коллекцию
     * @throws DatabaseException если что-то пошло не так при работе с базой данных
     */
    public void loadCollectionFromDatabase(CollectionManager collectionManager) throws DatabaseException {
        handleQuery((connection -> {
            collectionManager.initList();
            String query = "SELECT * FROM studyGroup" +
                    " INNER JOIN person ON studyGroup.person_id = person.person_id";
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            StudyGroup studyGroup;
            while (rs.next()) {
                studyGroup = new StudyGroup(
                        rs.getString("studyGroup_name"),
                        new Coordinates(
                                rs.getInt("coordinatex"),
                                rs.getFloat("coordinatey")
                        ),
                        rs.getInt("studentsCount"),
                        FormOfEducation.valueOf(rs.getString("formOfEducation")),
                        Semester.valueOf(rs.getString("semester")),
                        new Person(
                                rs.getString("person_name"),
                                rs.getInt("height"),
                                Color.valueOf(rs.getString("eye_color")),
                                Color.valueOf(rs.getString("hair_color")),
                                Country.valueOf(rs.getString("nationality"))
                        )
                );
                studyGroup.setId(rs.getInt("studyGroup_id"));
                collectionManager.add(studyGroup);
            }
            logger.info("Коллекция загружена из базы данных");
        }));
    }

    /**
     * Получает пароль по имени пользователя.
     *
     * @param username имя пользователя
     * @return пароль (хешированный)
     * @throws DatabaseException если что-то пошло не так при работе с базой данных
     */
    public String getPassword(String username) throws DatabaseException {
        return this.handleQuery((Connection connection) -> {
            String query = "SELECT (\"password\")" +
                    " FROM \"user\"" +
                    " WHERE \"user\".login = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);

            ResultSet result = statement.executeQuery();

            if (result.next()) {
                return result.getString("password");
            }
            return null;
        });
    }

    /**
     * Проверяет, существует ли пользователь.
     *
     * @param username имя пользователя
     * @return true, если существует; false, если нет
     * @throws DatabaseException если что-то пошло не так при работе с базой данных
     */
    public boolean doesUserExist(String username) throws DatabaseException {
        return this.<Boolean>handleQuery((Connection connection) -> {
            String query = "SELECT COUNT(*)" +
                    " FROM \"user\"" +
                    " WHERE \"user\".login = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);
            ResultSet result = statement.executeQuery();

            result.next();

            return result.getInt("count") > 0;
        });
    }

    /**
     * Добавляет пользователя в базу данных.
     *
     * @param username имя пользователя
     * @param password пароль (хешированный)
     * @throws DatabaseException если что-то пошло не так при работе с базой данных
     */
    public void addUser(String username, String password) throws DatabaseException {
        handleQuery((Connection connection) -> {
            String query = "INSERT INTO \"user\" (\"login\", \"password\")" +
                    "VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);
            statement.setString(2, HashEncrypter.encryptString(password + salty));

            statement.executeUpdate();
        });
    }

    /**
     * Удаляет все элементы, владельцем которых является пользователь.
     *
     * @param username имя пользователя
     * @return список id элементов, которые были удалены
     * @throws DatabaseException если что-то пошло не так при работе с базой данных
     */
    public List<Integer> clear(String username) throws DatabaseException {
        return handleQuery((Connection connection) -> {
            String query = "DELETE FROM person" +
                    " USING studyGroup, \"user\"" +
                    "WHERE person.person_id = studyGroup.person_id AND studyGroup.user_id = \"user\".id AND \"user\".login = ?" +
                    " RETURNING studyGroup.studyGroup_id;";

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);
            ResultSet result = statement.executeQuery();

            ArrayList<Integer> ids = new ArrayList<>();

            while (result.next()) {
                ids.add(result.getInt("studyGroup_id"));
            }
            return ids;
        });
    }

    public boolean validateUserData(String login, String password) throws DatabaseException {
        String realPassword = getPassword(login);

        return HashEncrypter.encryptString(password + salty).equals(realPassword);
    }
}
