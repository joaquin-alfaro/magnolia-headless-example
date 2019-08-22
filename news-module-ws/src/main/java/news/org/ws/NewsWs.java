package news.org.ws;

/**
 * This class is optional and represents the configuration for the news-module-ws module.
 * By exposing simple getter/setter/adder methods, this bean can be configured via content2bean
 * using the properties and node from <tt>config:/modules/news-module-ws</tt>.
 * If you don't need this, simply remove the reference to this class in the module descriptor xml.
 */
public class NewsWs {
    /* you can optionally implement info.magnolia.module.ModuleLifecycle */

    public static final String NEWS_REPOSITORY = "news";
}
