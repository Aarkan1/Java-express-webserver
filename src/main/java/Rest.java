import database.SQLiteDb;
import entities.User;
import express.Express;

import java.util.List;

@SuppressWarnings("unchecked")
public class Rest {
  private Express app;
  private SQLiteDb db;

  public Rest(Express app, SQLiteDb db) {
    this.app = app;
    this.db = db;

    get();
    post();
  }

  private void get() {
    app.get("/rest/users", (req, res) -> {
      // db.get return a list of object, and we need to
      // explicitly cast the list to the type we want - see (List<User>)
      // We also need to pass which class we're going to
      // populate the list with - see User.class
      var allUsers = (List<User>) db.get(User.class, "SELECT * FROM users");

      res.json(allUsers);
    });

    app.get("/rest/users/:id", (req, res) -> {
      var id = Long.parseLong(req.getParam("id")); // Param are always strings, and needs to be parsed
      var user = (List<User>) db.get(User.class,"SELECT * FROM users WHERE id = ?", List.of(id));

      res.json(user.get(0));
    });
  }

  private void post() {
    // To set query parameters we can either add a list of values
    // (this will automatically set right param and prevent SQL injection)
    // Note: it's important that right variable type is passed in the list
    // to properly set right parameter
    app.post("/rest/users", (req, res) -> {
      // to get an instance of a specific class we must
      // provide which class the body should convert to
      var user = (User) req.getBody(User.class);

      // db.update returns auto incremented id after insertion.
      var id = db.update("INSERT INTO users(firstname, lastname, age) VALUES(?, ?, ?)",
              List.of(user.getFirstname(), user.getLastname(), user.getAge()));
      user.setId(id);

      res.json(user);
    });

    // or use a lambda to manually set the PreparedStatement parameters, like:
    /*
      var id = db.update("INSERT INTO users(name, username, password) VALUES(?, ?, ?)", statement -> {
        statement.setString(1, user.getFirstname());
        statement.setString(2, user.getLastname());
        statement.setInt(3, user.getAge());
      });
     */
  }
}
