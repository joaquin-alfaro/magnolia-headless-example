package news.org.ws.service;

import info.magnolia.cms.i18n.I18nContentSupport;
import info.magnolia.context.MgnlContext;
import info.magnolia.dam.api.Asset;
import info.magnolia.dam.templating.functions.DamTemplatingFunctions;
import lombok.extern.slf4j.Slf4j;
import news.org.ws.NewsWs;
import news.org.ws.dto.NewsDto;
import org.apache.commons.lang3.StringUtils;
import org.jokes.dto.Joke;
import org.jokes.service.IcndbJokesService;

import javax.inject.Inject;
import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Slf4j
public class NewsService {

    private static final String PROPERTY_AUTHOR = "author";
    private static final String PROPERTY_PUBLICATION_DATE = "publicationDate";
    private static final String PROPERTY_TITLE = "title";
    private static final String PROPERTY_SUMMARY = "summary";
    private static final String PROPERTY_IMAGE = "image";
    private static final String PROPERTY_DETAIL = "detail";

    private final I18nContentSupport i18nContentSupport;
    private final DamTemplatingFunctions damTemplatingFunctions;
    private final IcndbJokesService icndbJokesService;

    @Inject
    public NewsService(I18nContentSupport i18nContentSupport, DamTemplatingFunctions damTemplatingFunctions, IcndbJokesService icndbJokesService) {
        this.i18nContentSupport = i18nContentSupport;
        this.damTemplatingFunctions = damTemplatingFunctions;
        this.icndbJokesService = icndbJokesService;
    }

    /**
     * List news filtered by date
     * @param dateFrom
     * @param locale
     * @return
     * @throws RepositoryException
     */
    public List<NewsDto> listNews(Date dateFrom, Locale locale) throws RepositoryException {
        List<NewsDto> newDtoList = new ArrayList<NewsDto>();

        final Session session = MgnlContext.getJCRSession(NewsWs.NEWS_REPOSITORY);

        final QueryManager queryManager = session.getWorkspace().getQueryManager();

            /*
                SELECT * FROM [mgnl:news]
                WHERE ([dateFrom] IS NULL OR [dateFrom] <= CAST('2018-09-14T00:00:00.000+02:00' AS DATE)) AND ([dateTo] IS NULL OR [dateTo] >= CAST('2018-09-14T00:00:00.000+02:00' AS DATE))
            */
        String select = "SELECT * FROM [mgnl:news]";
        StringBuilder filter = new StringBuilder();

        if (dateFrom != null) {
            SimpleDateFormat sdfJcr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            String sDate = sdfJcr.format(dateFrom);
            sDate = sDate.substring(0, sDate.length()-2) + ":" + sDate.substring(sDate.length()-2);

            if (filter.length() == 0) {
                filter.append(" WHERE");
            } else {
                filter.append(" AND");
            }
            filter.append(" ([publicationDate] IS NULL OR [publicationDate] >= CAST('" + sDate + "' AS DATE))");
        }
        select += " " + filter.toString();

        Query query = queryManager.createQuery(select, Query.JCR_SQL2);
        QueryResult queryResult = query.execute();
        NodeIterator nodeIterator = queryResult.getNodes();
        if (nodeIterator != null) {
            nodeIterator.forEachRemaining((node) -> {
                try {
                    newDtoList.add(marshallNodeToNewsDto((Node) node, locale));
                } catch (RepositoryException e) {
                    log.error("Errors during marshalling of JCR Node to NewsDto {}", node, e);
                }

            });
        }

        return newDtoList;
    }

    /**
     * Returns New by id
     * @param id
     * @param locale
     * @return
     */
    public NewsDto getNewsById(String id, Locale locale) throws RepositoryException {
        if (id == null) {
            return null;
        }

        NewsDto newsDto = null;
        try {
            final Session session = MgnlContext.getJCRSession(NewsWs.NEWS_REPOSITORY);
            Node node = session.getNodeByIdentifier(id);
            if (node != null) {
                newsDto = marshallNodeToNewsDto(node, locale);
            }
        } catch (ItemNotFoundException inf) {
            // Do nothing for ItemNotFoundException because means "node not found"
            ;
        }
        return newsDto;

    }
    /**
     * Marshalls a JCR Node of a new to a NewsDto
     * @param node
     * @param locale
     * @return
     */
    private NewsDto marshallNodeToNewsDto(Node node, Locale locale) throws RepositoryException {
        NewsDto newsDto = new NewsDto();

        newsDto.setIdentifier((node.getIdentifier()));
        newsDto.setName(node.getName());

        if (node.hasProperty(PROPERTY_AUTHOR)) {
            newsDto.setAuthor(node.getProperty(PROPERTY_AUTHOR).getString());
            // Loads a joke based on the name of the Author of the new
            newsDto.setAuthorJoke(getJokeForName(newsDto.getAuthor()));
        }

        if (node.hasProperty(PROPERTY_PUBLICATION_DATE)) {
            newsDto.setPublicationDate(node.getProperty(PROPERTY_PUBLICATION_DATE).getDate());
        }

        if (node.hasProperty(PROPERTY_IMAGE)) {
            newsDto.setImage(buildImagePath(node.getProperty(PROPERTY_IMAGE).getString()));
        }

        Property property;

        property = getPropertyTranslation(node, PROPERTY_TITLE, locale);
        newsDto.setTitle((property == null)? "": property.getString());

        property = getPropertyTranslation(node, PROPERTY_SUMMARY, locale);
        newsDto.setSummary((property == null)? "": property.getString());

        property = getPropertyTranslation(node, PROPERTY_DETAIL, locale);
        newsDto.setDetail((property == null)? "": property.getString());

        return newsDto;
    }

    /**
     * Returns the translation of a property
     *
     * @param node
     * @param name
     * @param locale
     * @return
     * @throws RepositoryException
     */
    private Property getPropertyTranslation(Node node, String name, Locale locale) throws RepositoryException {

        final Locale defaultLocale = i18nContentSupport.getDefaultLocale();

        if (locale == null) {
            if (node.hasProperty(name)) {
                return node.getProperty(name);
            } else {
                return null;
            }
        }

        if (defaultLocale != null && locale!=null && locale.equals(defaultLocale)) {
            if (node.hasProperty(name)) {
                return node.getProperty(name);
            } else {
                return null;
            }
        }

        Property translation = i18nContentSupport.getProperty(node, name, locale);

        // If country variant translation does not exist then return main translation of the language (without country)
        if (translation == null && StringUtils.isNotEmpty(locale.getCountry())) {
            Locale langOnlyLocale = new Locale(locale.getLanguage());
            translation = i18nContentSupport.getProperty(node, name, langOnlyLocale);
        }

        return translation;
    }

    /**
     * Build the path of an image from the identifier in DAM assets
     * @param image
     * @return
     */
    private String buildImagePath(String image) {
        Asset asset = damTemplatingFunctions.getAsset(image);
        /**
         * Technically is possible that the image of news does not exist.
         * Return null in this case
         */
        if (asset == null) {
            return null;
        }

        String fileName = null;
        try {
            fileName = new URI(null, null, asset.getFileName(), null).toASCIIString();
        } catch (URISyntaxException e) {
            log.error("Errors when building the path of the image {}", image, e);
        }
        return "/dam/" + image + "/" + fileName;
    }

    /**
     * Returns a joke from icndb.com for the given name
     * @param fullName
     * @return
     */
    private String getJokeForName(String fullName) {

        if (fullName == null) {
            return null;
        }

        int separator = fullName.indexOf(" ");
        String firstName = (separator < 0)? fullName: fullName.substring(0, separator);
        String lastName = (separator < 0)? "": fullName.substring(separator + 1);

        Joke joke = icndbJokesService.getJoke(firstName, lastName);

        return (joke == null) ? "": joke.value.joke;
    }
}
