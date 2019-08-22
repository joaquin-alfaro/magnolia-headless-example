package news.org.ws.rest.v1;

import info.magnolia.rest.AbstractEndpoint;
import info.magnolia.rest.EndpointDefinition;
import io.swagger.annotations.*;
import news.org.ws.dto.NewsDto;
import news.org.ws.service.NewsService;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Api(value = "/news-portal/v1", description = "Operations related with News")
@Path("/news-portal/v1")
public class NewsEndpoint<D extends EndpointDefinition> extends AbstractEndpoint<D> {

    private static final String STATUS_MESSAGE_OK = "OK";
    private static final String STATUS_MESSAGE_NOT_FOUND = "Not Found";
    private static final String STATUS_MESSAGE_NO_CONTENT = "No content and Not Found";
    private static final String STATUS_MESSAGE_INTERNAL_ERROR = "Internal Server Error";

    private final NewsService newsService;
    @Inject
    public NewsEndpoint(D endpointDefinition, NewsService newsService) {
        super(endpointDefinition);
        this.newsService = newsService;
    }

    @GET
    @Path("/news")
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(value = "List news")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = STATUS_MESSAGE_OK),
            @ApiResponse(code = 404, message = STATUS_MESSAGE_NOT_FOUND),
            @ApiResponse(code = 500, message = STATUS_MESSAGE_INTERNAL_ERROR)
    })
    public Response getNews(
            @ApiParam(example = "en", value = "Translation language") @HeaderParam("accept-language") String language,
            @ApiParam(example = "2019-08-22", value = "Date of publication. The date must use this mask yyyy-MM-dd", required = true) @QueryParam("dateFrom") String dateFrom
    ) throws ParseException, RepositoryException {

        // Parse date string to Date type
        Date date = null;
        if (dateFrom != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            date = sdf.parse(dateFrom);
        }

        /**
         * NOTE
         * It is not necessary to get "language" param because Magnolia loads the value of "accept-language" to the context
         *
         * Why I do it? because it is an example
         */
        Locale locale = buildLocale(language);

        List<NewsDto> newsDtoList = newsService.listNews(date, locale);

        return Response.ok(newsDtoList).build();
    }

    @GET
    @Path("/news/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(value = "Return New by id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = STATUS_MESSAGE_OK),
            @ApiResponse(code = 404, message = STATUS_MESSAGE_NOT_FOUND),
            @ApiResponse(code = 204, message = STATUS_MESSAGE_NO_CONTENT),
            @ApiResponse(code = 500, message = STATUS_MESSAGE_INTERNAL_ERROR)
    })
    public Response getNewsById(
            @ApiParam(example = "fd684d86-46c7-46ed-9d4e-f3f0e71a5c17", value = "News identifier") @PathParam("id") String id,
            @ApiParam(example = "en", value = "Translation language") @HeaderParam("accept-language") String language
    ) throws RepositoryException {

        /**
         * NOTE
         * It is not necessary to get "language" param because Magnolia loads the value of "accept-language" to the context
         *
         * Why I do it? because it is an example
          */
        Locale locale = buildLocale(language);
        NewsDto newsDto = newsService.getNewsById(id, locale);
        if (newsDto == null) {
            return Response.noContent().build();
        } else {
            return Response.ok(newsDto).build();
        }
    }

    /**
     * Builds "Locale" from lang_country string
     * @param language
     * @return
     */
    private Locale buildLocale(String language) {
        // Build locale from "lang" param
        Locale locale = null;
        if (language != null) {
            // Retrieve lang and country from param
            // Example: pt_BR is lang "pt" and country "BR"
            String[] lang_country = language.split("_");
            if (lang_country.length == 2) {
                locale = new Locale(lang_country[0], lang_country[1]);
            } else {
                locale = new Locale(language);
            }
        }
        return locale;
    }
}
