package de.numcodex.feasibility_gui_backend.query.templates;

public class QueryTemplateException extends Exception {

  /**
   * Constructs a new {@link QueryTemplateException} without further details.
   */
  public QueryTemplateException() {
    super();
  }

  /**
   * Constructs a new {@link QueryTemplateException} with the specified detail message.
   *
   * @param message The detail message.
   */
  public QueryTemplateException(String message) {
    super(message);
  }

  /**
   * Constructs a new {@link QueryTemplateException} with the specified detail message and cause.
   *
   * @param message The detail message.
   * @param cause The cause.
   */
  public QueryTemplateException(String message, Throwable cause) {
    super(message, cause);
  }

}
