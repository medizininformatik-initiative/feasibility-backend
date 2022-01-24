package de.numcodex.feasibility_gui_backend.query.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

public class JsonNodeType implements UserType {

  @Override
  public int[] sqlTypes() {
    return new int[] {Types.JAVA_OBJECT};
  }

  @Override
  public Class<JsonNode> returnedClass() {
    return JsonNode.class;
  }

  @Override
  public Object nullSafeGet(
      ResultSet rs,
      String[] names,
      SharedSessionContractImplementor sharedSessionContractImplementor,
      Object value)
      throws HibernateException, SQLException {
    final String cellContent = rs.getString(names[0]);
    if (cellContent == null) {
      return null;
    }
    try {
      final ObjectMapper mapper = new ObjectMapper();
      return mapper.readTree(cellContent.getBytes(StandardCharsets.UTF_8));
    } catch (final Exception ex) {
      throw new RuntimeException(
          "Failed to convert String " + "to JsonNode: " + ex.getMessage(), ex);
    }
  }

  @Override
  public void nullSafeSet(
      PreparedStatement preparedStatement,
      Object value,
      int idx,
      SharedSessionContractImplementor sharedSessionContractImplementor)
      throws HibernateException, SQLException {
    if (value == null) {
      preparedStatement.setNull(idx, Types.OTHER);
      return;
    }
    try {
      final ObjectMapper mapper = new ObjectMapper();
      preparedStatement.setObject(idx, mapper.writeValueAsString(value), Types.OTHER);
    } catch (final Exception ex) {
      throw new RuntimeException("Failed to convert JsonNode to String: " + ex.getMessage(), ex);
    }
  }


  @Override
  public boolean equals(Object object1, Object object2) {
    return Objects.equals(object1, object2);
  }

  @Override
  public int hashCode(Object object) {
    return object.hashCode();
  }

  @Override
  public Object deepCopy(Object value) {
    return value;
  }

  @Override
  public boolean isMutable() {
    return false;
  }

  @Override
  public Serializable disassemble(Object object) {
    return (Serializable) object;
  }

  @Override
  public Object assemble(
          Serializable cached,
          Object owner) {
    return cached;
  }

  @Override
  public Object replace(
          Object object,
          Object target,
          Object owner) {
    return object;
  }
}