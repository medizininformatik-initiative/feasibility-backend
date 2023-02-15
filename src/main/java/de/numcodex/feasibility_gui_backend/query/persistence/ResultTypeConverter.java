package de.numcodex.feasibility_gui_backend.query.persistence;


import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ResultTypeConverter implements AttributeConverter<ResultType, String> {

  @Override
  public String convertToDatabaseColumn(ResultType resultType) {
    if (resultType == null) {
      return null;
    }

    return resultType.toString();
  }

  @Override
  public ResultType convertToEntityAttribute(String value) {
    if (value == null) {
      return null;
    }

    return ResultType.valueOf(value);
  }
}
