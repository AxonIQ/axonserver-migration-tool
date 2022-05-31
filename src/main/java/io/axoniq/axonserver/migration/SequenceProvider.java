package io.axoniq.axonserver.migration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.axonframework.axonserver.connector.AxonServerConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class SequenceProvider {

    private final AxonServerConnectionManager axonDBClient;

    Cache<String, Long> cache = Caffeine.newBuilder()
                                        .expireAfterWrite(1, TimeUnit.MINUTES)
                                        .maximumSize(10000)
                                        .build();

    @Autowired
    public SequenceProvider(AxonServerConnectionManager axonDBClient) {
        this.axonDBClient = axonDBClient;
    }

    public Long getNextSequenceForAggregate(String aggregateIdentifier) {
        Long currentSequence = cache.get(aggregateIdentifier, s -> {
            try {
                return axonDBClient.getConnection().eventChannel().findHighestSequence(s).get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        if (currentSequence == null) {
            currentSequence = -1L;
        }

        Long newSequence = currentSequence + 1;
        cache.put(aggregateIdentifier, newSequence);
        return newSequence;
    }
}
