package de.numcodex.feasibility_gui_backend.model.db;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

// TODO: this does not work with postgresql...(see https://thorben-janssen.com/hibernate-enum-mappings/#Customized_Mapping_to_a_Database-Specific_Enum_Type)
@Converter
public class QueryStatusConverter implements AttributeConverter<QueryStatus, String> {
    @Override
    public String convertToDatabaseColumn(QueryStatus queryStatus) {
        return queryStatus.getShortcode();
    }

    @Override
    public QueryStatus convertToEntityAttribute(String dbData) {
        return QueryStatus.fromShortcode(dbData);
    }
}
