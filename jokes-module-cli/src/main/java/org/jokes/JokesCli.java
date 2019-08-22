package org.jokes;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class is optional and represents the configuration for the jokes-module-cli module.
 * By exposing simple getter/setter/adder methods, this bean can be configured via content2bean
 * using the properties and node from <tt>config:/modules/jokes-module-cli</tt>.
 * If you don't need this, simply remove the reference to this class in the module descriptor xml.
 * See https://documentation.magnolia-cms.com/display/DOCS/Module+configuration for information about module configuration.
 */
@Data
@NoArgsConstructor
public class JokesCli {
    /* you can optionally implement info.magnolia.module.ModuleLifecycle */

    /**
     * Name of the Magnolia Rest Client for api.icndb.com
     */
    private String icndbRestClient;

    /**
     * Name of the cache for jokes
     */
    private String cacheName;
}
