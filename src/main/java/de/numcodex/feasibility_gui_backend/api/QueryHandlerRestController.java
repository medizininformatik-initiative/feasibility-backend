package de.numcodex.feasibility_gui_backend.api;

import de.numcodex.feasibility_gui_backend.model.query.QueryResult;
import de.numcodex.feasibility_gui_backend.model.query.StructuredQuery;
import de.numcodex.feasibility_gui_backend.service.QueryHandlerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/*
 Rest Interface for the UI to send queries from the ui to the ui backend.
 */
@RequestMapping("api/v1/query-handler")
@RestController
public class QueryHandlerRestController {

    private final QueryHandlerService queryHandlerService;

    public QueryHandlerRestController(QueryHandlerService queryHandlerService) {
        this.queryHandlerService = queryHandlerService;
    }


    @PostMapping
    public Response runQuery(@Context UriInfo uriInfo, StructuredQuery query) {
        String id = queryHandlerService.runQuery(query);

        // TODO: build absolute URI (.../result/{id})
        uriInfo.getAbsolutePathBuilder().path(QueryHandlerRestController.class, "getQueryResult");
        return Response.created(null).build();
    }

    @GetMapping(path = "/result/{id}")
    public QueryResult getQueryResult(@PathVariable("id") String id) {
        return queryHandlerService.getQueryResult(id);
    }

}
