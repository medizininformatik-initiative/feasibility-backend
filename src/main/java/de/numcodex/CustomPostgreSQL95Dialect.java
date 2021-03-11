package de.numcodex;

import org.hibernate.dialect.PostgreSQL95Dialect;

import java.sql.Types;

public class CustomPostgreSQL95Dialect extends PostgreSQL95Dialect {
  
  public CustomPostgreSQL95Dialect() {
    this.registerColumnType(Types.JAVA_OBJECT, "json");
  }
}