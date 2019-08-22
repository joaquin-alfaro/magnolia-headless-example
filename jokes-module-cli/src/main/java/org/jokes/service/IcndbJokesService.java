package org.jokes.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.magnolia.module.cache.Cache;
import info.magnolia.module.cache.inject.CacheFactoryProvider;
import info.magnolia.registry.RegistrationException;
import info.magnolia.rest.client.registry.RestClientRegistry;
import info.magnolia.resteasy.client.RestEasyClient;
import org.jokes.JokesCli;
import org.jokes.dto.Joke;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;

public class IcndbJokesService {

    private final Logger log = LoggerFactory.getLogger(IcndbJokesService.class);

    private final int CacheTimeToLive = 86400; // 1 day = 24 hours = 86400 secs
    private final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final JokesCli definition;
    private final RestClientRegistry restClientRegistry;
    private final Provider<CacheFactoryProvider> cacheFactoryProvider;

    @Inject
    public IcndbJokesService(JokesCli definition, RestClientRegistry restClientRegistry, Provider<CacheFactoryProvider> cacheFactoryProvider) {
        this.definition = definition;
        this.restClientRegistry = restClientRegistry;
        this.cacheFactoryProvider = cacheFactoryProvider;
    }

    /**
     * Get joke from cache and if it does not exist the get from icndb.com
     * @param firstName
     * @param lastName
     * @return
     */
    public Joke getJoke(String firstName, String lastName) {
        if (firstName == null && lastName == null) {
            return null;
        }

        Joke joke = null;
        Cache cache = cacheFactoryProvider.get().get().getCache(definition.getCacheName());
        String key = buildCacheKey(firstName, lastName);
        if (cache == null) {
            log.error("The cache {} is null.", definition.getCacheName());
        } else if (cache.hasElement(key)) {
            joke = (Joke)cache.get(key);
        }

        if (joke == null) {
            joke = getJokeFromExternal(firstName, lastName);
            if (joke != null && cache != null) {
                cache.put(key, joke, CacheTimeToLive);
            }
        }

        return joke;
    }

    /**
     * Returns a joke for a given name and surname. Get from icndb.com
     * @param firstName
     * @param lastName
     * @return
     */
    public Joke getJokeFromExternal(String firstName, String lastName) {
        RestEasyClient client;
        IcndbJokesCli icndbJokesCli = null;

        try {
            client = (RestEasyClient) restClientRegistry.getRestClient(definition.getIcndbRestClient());
            icndbJokesCli = client.getClientService(IcndbJokesCli.class);
        } catch (RegistrationException e) {
            log.error("Failed to get a client for [{}].", definition.getIcndbRestClient(), e);
        }

        Joke joke = null;
        if (icndbJokesCli != null) {
            try  {
                String response = icndbJokesCli.getJoke("random", firstName, lastName);
                if (response != null) {
                    try {
                        TypeReference<Joke> mapType = new TypeReference<Joke>() {};
                        joke = mapper.readValue(response, mapType);
                    } catch (Exception e) {
                        log.error("Failure parsing response from icndb. {}", response, e);
                    }
                }
            }
            catch (javax.ws.rs.NotFoundException e) {
                log.warn("Joke for {}, {} not found in icndb", firstName, lastName);
            }
        }
        return joke;
    }

    /**
     * Builds the cache key from firstName and lastName e.g. Jhon|Doe
     * @param firstName
     * @param lastName
     * @return
     */
    private String buildCacheKey(String firstName, String lastName) {
        return ((firstName != null) ? firstName : "") + "|" + ((lastName != null) ? lastName: "");
    }
}
