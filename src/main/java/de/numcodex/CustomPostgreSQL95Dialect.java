package de.numcodex;

import java.sql.Types;
import org.hibernate.dialect.PostgreSQL95Dialect;

public class CustomPostgreSQL95Dialect extends PostgreSQL95Dialect {
  
  public CustomPostgreSQL95Dialect() {
    this.registerColumnType(Types.JAVA_OBJECT, "json");
  }
}