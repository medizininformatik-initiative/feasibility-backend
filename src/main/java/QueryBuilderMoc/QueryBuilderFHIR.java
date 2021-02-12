package QueryBuilderMoc;

public class QueryBuilderFHIR implements QueryBuilder {

  @Override
  public String getQueryContent() {
    return "FHIRQuery";
  }
}
