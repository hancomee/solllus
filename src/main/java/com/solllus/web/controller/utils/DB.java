package com.solllus.web.controller.utils;

import com.boosteel.nativedb.NativeDB;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solllus.web.controller.domain.Display;
import com.solllus.web.controller.domain.DisplayKey;
import com.solllus.web.security.UserDetail;

import java.sql.ResultSet;
import java.util.*;

import static com.boosteel.nativedb.core.SQL.mapToSQL;

public class DB {

    public NativeDB db;

    public DB(NativeDB db) {
        this.db = db;
    }

    // [어드민용] removal체크까지 다 보내준다. (아직 삭제되지않은 유저는 새로 생성될 수 없다.)
    public Set<String> users() {
        return db.doStmtR(s -> {
            Set<String> set = new TreeSet<>();
            try (ResultSet rs = s.executeQuery("SELECT name FROM users WHERE removal = 0")) {
                while (rs.next()) set.add(rs.getString("name"));
            }
            return set;
        });
    }

    public int destroyUser(String name) {
        return db.doStmtR(s -> {
            s.executeUpdate(mapToSQL("DELETE FROM display WHERE name = {0}", name));
            s.executeUpdate(mapToSQL("DELETE FROM apps WHERE name = {0}", name));
            s.executeUpdate(mapToSQL("DELETE FROM display_key WHERE name = {0}", name));
            return s.executeUpdate(mapToSQL("DELETE FROM users WHERE name = {0}", name));
        });
    }

    public int restoreUser(String name) {
        return db.doStmtR(s -> s.executeUpdate("UPDATE users SET removal = 0 WHERE name = '" + name + "'"));
    }

    public int removalUser(String name) {
        return db.doStmtR(s -> s.executeUpdate("UPDATE users SET removal = 1 WHERE name = '" + name + "'"));
    }

    public void addUser(String name, String pass, String roles) {
        db.doStmt(s -> {
            s.executeUpdate(mapToSQL("INSERT INTO users (name, pass, roles) VALUES ({0}, {1}, {2})", name, pass, roles));
        });
    }

    public void updateUser(String name, String pass, String roles) {
        db.doStmt(s -> {
            s.executeUpdate(mapToSQL("UPDATE users set pass = {1}, roles = {2} WHERE name = {0}", name, pass, roles));
        });
    }

    public UserDetail userDetail(String name) {
        return db.doStmtR(s -> {
            try (ResultSet resultSet = s.executeQuery(mapToSQL("SELECT * FROM users WHERE name = {0}", name))) {
                if (resultSet.next()) {
                    UserDetail userDetail = new UserDetail();
                    userDetail.setPassword(resultSet.getString("pass"));
                    userDetail.setUsername(resultSet.getString("name"));
                    userDetail.setRoles(resultSet.getString("roles"));
                    return userDetail;
                }
            } catch (Exception e) {
            }
            return null;
        });
    }

    public Map<String, Display> display(String name) {
        return db.doStmtR(s -> {
            Map<String, Display> values = new TreeMap<>();
            try (ResultSet rs = s.executeQuery(mapToSQL("SELECT * FROM display WHERE name = {0} ORDER BY idx", name))) {
                while (rs.next()) {
                    Display display = new Display(rs.getString("idx"), name);
                    String jsonText = rs.getString("json");
                    if (!jsonText.trim().isEmpty()) {
                        Map<String, String> json = new ObjectMapper().readValue(jsonText, TypeReferences.map_str_str);
                        display.text = json.get("text");
                        display.root = json.get("root");
                        display.name = json.get("name");
                        display.path = json.get("path");
                    }
                    values.put(display.index, display.update());
                }
            } catch (Exception e) {
            }
            return values;
        });
    }

    /* ************************ ▼ App ▼ ************************ */

    public Set<String> getAppList(String name, String index) {
        return db.doStmtR(s -> {
            Set<String> values = new TreeSet<>();
            try (ResultSet resultSet = s.executeQuery(mapToSQL("SELECT * FROM apps WHERE name = {0} AND idx = {1}", name, index))) {
                while (resultSet.next()) {
                    values.add(resultSet.getString("path"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return values;
        });
    }

    public int addApp(String user, String index, String appPath) {
        return db.doStmtR(s -> {
            try (ResultSet rs = s.executeQuery(mapToSQL("SELECT id FROM apps WHERE name = {0} AND idx = {1} AND path = {2}",
                    user, index, appPath))) {
                if (rs.next()) return 0;
            }
            return s.executeUpdate(mapToSQL("INSERT INTO apps (name, idx, path) VALUES ({0}, {1}, {2})", user, index, appPath));
        });
    }

    public int removeApp(String user, String index, String appPath) {
        return db.doStmtR(s -> {
            return s.executeUpdate(mapToSQL("DELETE FROM apps WHERE name = {0} AND idx = {1} AND path = {2}", user, index, appPath));
        });
    }

    public int removeApps(String user, String index) {
        return db.doStmtR(s -> {
            return s.executeUpdate(mapToSQL("DELETE FROM apps WHERE name = {0} AND idx = {1}", user, index));
        });
    }
    /* ************************ ▲ App ▲ ************************ */


    public int removeUserApp(String user, String appPath) {
        return db.doStmtR(s -> {
            return s.executeUpdate(mapToSQL("DELETE FROM apps WHERE name = {0} AND path = {1}", user, appPath));
        });
    }


    public List<String> getUsedList(String path) {
        return db.doStmtR(s -> {
            List<String> list = new ArrayList<>();
            try (ResultSet rs = s.executeQuery(mapToSQL("SELECT name FROM apps WHERE path = {0}", path))) {
                while (rs.next()) list.add(rs.getString("name"));
            }
            return list;
        });
    }

    public int deleteApp(String path) {
        return db.doStmtR(s -> s.executeUpdate(mapToSQL("DELETE FROM apps WHERE path = {0}", path)));
    }


    /* ***************** ▼ 디스플레이 접속인증키 ▼ ***************** */
    public List<DisplayKey> getKeys() {
        return db.doStmtR(s -> {
            List<DisplayKey> list = new ArrayList<>();
            try(ResultSet rs = s.executeQuery("SELECT * FROM display_key")) {
                while(rs.next()) {
                    DisplayKey key = new DisplayKey();
                    key.createTime = rs.getTimestamp("createTime").getTime();
                    key.name = rs.getString("name");
                    key.nickname = rs.getString("nickname");
                    key.index = rs.getString("idx");
                    key.value = rs.getString("value");
                    list.add(key);
                }
            }
            return list;
        });
    }
    public int addKey(DisplayKey displayKey) {
        return db.doStmtR(s -> {
           return s.executeUpdate(mapToSQL("INSERT INTO display_key (createTime, value, name, idx, nickname) VALUES ({0}, {1}, {2}, {3}, {4})",
                   displayKey.datetime(), displayKey.value, displayKey.name, displayKey.index, displayKey.nickname));

        });
    }
    public int modifyKey(DisplayKey displayKey) {
        return db.doStmtR(s -> {
            return s.executeUpdate(mapToSQL("UPDATE display_key SET name = {1}, idx = {2}, nickname = {3} WHERE value = {0}",
                    displayKey.value, displayKey.name, displayKey.index, displayKey.nickname));

        });
    }
    public int deleteKey(DisplayKey displayKey) {
        return deleteKey(displayKey.value);
    }
    public int deleteKey(String keyValue) {
        return db.doStmtR(s -> s.executeUpdate(mapToSQL("DELETE FROM display_key WHERE value = {0}", keyValue)));
    }
    /* ***************** ▲ 디스플레이 접속인증키 ▲ ***************** */


    /* ***************** ▼ 디스플레이 추가/삭제 ▼ ***************** */
    public Display updateDisplay(String path, Display display) {
        return db.doStmtR(s -> {
            s.executeUpdate(mapToSQL("UPDATE display SET json = {2} WHERE name = {0} AND idx = {1}", path, display.index, display.jsonText()));
            return display;
        });
    }

    public int removeDisplay(String path, String index) {
        return db.doStmtR(s -> {
            return s.executeUpdate(mapToSQL("DELETE FROM display WHERE name = {0} AND idx= {1}", path, index));
        });
    }

    public Display addDisplay(String path, Display display) {
        return db.doStmtR(s -> {
            s.executeUpdate(mapToSQL("INSERT INTO display (name, idx, json) VALUES ({0}, {1}, {2})", path, display.index, display.jsonText()));
            return display;
        });
    }
    /* ***************** ▲ 디스플레이 추가/삭제 ▲ ***************** */

}
