package database;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;

@SuppressWarnings("unchecked")
public class ObjectMapper<T> {
  private Class clazz;
  private Map<String, Field> fields = new HashMap<>();

  public ObjectMapper(Class clazz, boolean useColumnTags) {
    this.clazz = clazz;

    List<Field> fieldList = Arrays.asList(clazz.getDeclaredFields());
    for (Field field : fieldList) {
      if(useColumnTags) {
//         Only map annotated fields
        Column col = field.getAnnotation(Column.class);
        if (col != null) {
          field.setAccessible(true);
          fields.put(col.value().isEmpty() ? field.getName() : col.value(), field);
        }
      } else {
//        Map all fields in entity
        field.setAccessible(true);
        fields.put(field.getName(), field);
      }
    }
  }

  public T map(Map<String, Object> row) {
    try {
      T dto = (T) clazz.getConstructor().newInstance();
      for (Map.Entry<String, Object> entity : row.entrySet()) {
        if (entity.getValue() == null) {
          continue;
        }
        String column = entity.getKey();
        Field field = fields.get(column);
        if (field != null) {
          Object val = entity.getValue();
          field.set(dto, convertInstanceOfObject(val));
        }
      }
      return dto;
    } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
      e.printStackTrace();
      return null;
    }
  }

  public List<T> map(List<Map<String, Object>> rows) {
    List<T> list = new LinkedList<>();

    for (Map<String, Object> row : rows) {
      list.add(map(row));
    }

    return list;
  }

  public List<T> map(ResultSet rs) {
    return this.map(this.resultSetToArrayList(rs));
  }

  public List resultSetToArrayList(ResultSet rs){
    ArrayList list = new ArrayList(50);
    try {
      ResultSetMetaData md = rs.getMetaData();
      int columns = md.getColumnCount();
      while (rs.next()) {
        HashMap row = new HashMap(columns);
        for (int i = 1; i <= columns; ++i) {
          row.put(md.getColumnName(i), rs.getObject(i));
        }
        list.add(row);
      }
    } catch (Exception ex) { ex.printStackTrace(); }

    return list;
  }

  private T convertInstanceOfObject(Object o) {
    try {
      return (T) o;
    } catch (ClassCastException e) {
      return null;
    }
  }
}