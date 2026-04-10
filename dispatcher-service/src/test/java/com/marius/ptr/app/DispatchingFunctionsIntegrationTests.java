package com.marius.ptr.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.test.FunctionalSpringBootTest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.function.Function;

@FunctionalSpringBootTest
class DispatchingFunctionsIntegrationTests {

    @Autowired
    private Function<Flux<OrderAcceptedMessage>, Flux<Long>> pack;

    @Autowired
    private Function<Flux<Long>, Flux<OrderDispatchedMessage>> label;

    @Test
    void packAndLabelOrder() {
        long orderId = 121;

        StepVerifier.create(label.apply(pack.apply(Flux.just(new OrderAcceptedMessage(orderId)))))
                .expectNextMatches(dispatchedOrder -> dispatchedOrder.equals(new OrderDispatchedMessage(orderId)))
                .verifyComplete();
    }
}
