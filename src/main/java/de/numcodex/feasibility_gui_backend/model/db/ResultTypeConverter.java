package de.numcodex.feasibility_gui_backend.model.db;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class ResultTypeConverter implements AttributeConverter<ResultType, String> {
    @Override
    public String convertToDatabaseColumn(ResultType resultType) {
        return resultType.getShortcode();
    }

    @Override
    public ResultType convertToEntityAttribute(String dbData) {
        return ResultType.fromShortcode(dbData);
    }
}
