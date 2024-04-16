package com.cakefactory.order;

import org.springframework.context.event.EventListener;

class TestOrderListener {

    private OrderReceivedEvent lastEvent;

    @EventListener
    public void onOrderReceived(OrderReceivedEvent orderReceivedEvent) {
        this.lastEvent = orderReceivedEvent;
    }

    public OrderReceivedEvent getLastEvent() {
        return this.lastEvent;
    }
}
