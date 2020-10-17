package guru.sfg.beer.inventory.service.services;

import guru.sfg.beer.inventory.service.config.JmsConfig;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class AllocationListener {

    private final AllocationService allocationService;
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    private void listen(AllocateOrderRequest allocateOrderRequest) {
        log.debug("Receiving Allocation Order Request for order: " + allocateOrderRequest.getBeerOrderDto().getId());

        AllocateOrderResult.AllocateOrderResultBuilder builder = AllocateOrderResult.builder();
        builder.beerOrderDto(allocateOrderRequest.getBeerOrderDto());

        try {
            Boolean allocationResult  = allocationService.allocateOrder(allocateOrderRequest.getBeerOrderDto());
            if(allocationResult) {
                builder.pendingInventory(false);
            } else {
                builder.pendingInventory(true);
            }
        } catch (Exception e) {
            log.error("Allocation failed for Order id: " + allocateOrderRequest.getBeerOrderDto().getId());
            builder.allocationError(true);
        }

        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE,
                builder.build());
    }
}
