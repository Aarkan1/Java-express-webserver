import at.favre.lib.crypto.bcrypt.BCrypt;
import com.google.gson.Gson;
import database.SQLiteDb;
import database.Utils;
import dtos.LoginDTO;
import express.Express;
import express.http.SessionCookie;
import express.utils.Status;

import java.util.List;

@SuppressWarnings("unchecked")
public class Authentication {
  private Express app;
  private SQLiteDb SQLiteDb;

  public Authentication(Express app, SQLiteDb SQLiteDb) {
    this.app = app;
    this.SQLiteDb = SQLiteDb;
    get();
    post();
  }

  private void post() {
    app.post("/api/login", (req, res) -> {
      var sessionCookie = (SessionCookie) req.getMiddlewareContent("sessioncookie");
      if(sessionCookie.getData() != null) {
        res.send("Already logged in");
        return;
      }

      var loginDto = (LoginDTO) Utils.convertBodyToObject(req.getBody(), LoginDTO.class);
      var userFromDb = (List<User>) SQLiteDb.get(User.class, "SELECT * FROM users WHERE username = ?",
              statement -> statement.setString(1, loginDto.username));

      if(userFromDb.size() < 1) {
        res.setStatus(Status._401);
        res.send("Bad credentials!");
        return;
      }
      var user = userFromDb.get(0);

      var result = BCrypt.verifyer().verify(loginDto.password.toCharArray(), user.getPassword().toCharArray());
      if(!result.verified) {
        res.setStatus(Status._401);
        res.send("Bad credentials!");
        return;
      }

      sessionCookie.setData(user);
      user.setPassword(null); // sanitize password

      res.send(new Gson().toJson(user));
    });

    app.post("/api/register", (req, res) -> {
      var sessionCookie = (SessionCookie) req.getMiddlewareContent("sessioncookie");
      if(sessionCookie.getData() != null) {
        res.send("Already logged in");
        return;
      }
      var user = (User) Utils.convertBodyToObject(req.getBody(), User.class);
      var userInDB = (List<User>) SQLiteDb.get(User.class, "SELECT * FROM users WHERE username = ?",
              statement -> statement.setString(1, user.getUsername()));

      if(userInDB.size() > 0) {
        res.setStatus(Status._400);
        res.send("User already exists!");
        return;
      }

      String hashedPassword = BCrypt.withDefaults().hashToString(10, user.getPassword().toCharArray());
      user.setPassword(hashedPassword);

      var id = SQLiteDb.update("INSERT INTO users(name, username, password) VALUES(?, ?, ?)", stmt -> {
        stmt.setString(1, user.getName());
        stmt.setString(2, user.getUsername());
        stmt.setString(3, user.getPassword());
      });

      user.setId(id); // update with incremented id
      sessionCookie.setData(user); // log in user with session
      user.setPassword(null); // sanitize password

      res.send(new Gson().toJson(user));
    });
  }

  private void get() {
    app.get("/api/login", (req, res) -> {
      var sessionCookie = (SessionCookie) req.getMiddlewareContent("sessioncookie");
      if(sessionCookie.getData() == null) {
        res.send("Not logged in");
        return;
      }

      var user = (User) sessionCookie.getData();
      user.setPassword(null); // sanitize password
      res.send(new Gson().toJson(user));
    });

    app.get("/api/logout", (req, res) -> {
      var sessionCookie = (SessionCookie) req.getMiddlewareContent("sessioncookie");
      sessionCookie.setData(null);
      res.send("Successfully logged out");
    });
  }
}
