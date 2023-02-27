package com.elorankingbot.backend.logging;

import discord4j.core.retriever.*;

@FunctionalInterface
public interface CustomExtendedEntityRetrievalStrategy extends EntityRetrievalStrategy {
    discord4j.core.retriever.EntityRetrievalStrategy STORE_FALLBACK_REST_WITH_CACHE_MISS_LOGGING = (gateway) -> {
        return new FallbackEntityRetrieverWithAddedCacheMissLogging(new StoreEntityRetriever(gateway), new RestEntityRetriever(gateway));
    };
}
