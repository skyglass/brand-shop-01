package com.cakefactory.order;

import com.cakefactory.basket.Basket;
import com.cakefactory.basket.BasketItem;
import com.cakefactory.payment.Address;
import com.cakefactory.payment.Order;
import com.cakefactory.payment.OrderItem;
import com.cakefactory.payment.PaymentService;
import com.cakefactory.payment.PendingPayment;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/order")
@Slf4j
class OrderController {

    private final Basket basket;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentService paymentService;
    private final ConcurrentHashMap<String, String> pendingOrders = new ConcurrentHashMap<>();

    public OrderController(Basket basket, ApplicationEventPublisher eventPublisher, PaymentService paymentService) {
        this.basket = basket;
        this.eventPublisher = eventPublisher;
        this.paymentService = paymentService;
    }

    @GetMapping
    String order() {
        return "order";
    }

    @GetMapping("/complete")
    String completeOrder(@RequestParam String token) {
        try {
            paymentService.complete(token);
            this.eventPublisher.publishEvent(new OrderReceivedEvent(pendingOrders.getOrDefault(token, ""), this.basket.getItems()));
            this.basket.clear();
            this.pendingOrders.computeIfPresent(token, (s, s2) -> null);
            return "redirect:/order";
        } catch (Exception e) {
            log.error("Failed to complete order", e);
            return "redirect:/";
        }
    }

    @PostMapping
    String createOrder(@RequestParam String addressLine1, @RequestParam String addressLine2, @RequestParam String postcode, HttpServletRequest request) {
        Address address = Address.builder().line1(addressLine1).line2(addressLine2).postcode(postcode).build();
        List<OrderItem> items = this.basket.getItems().stream().map(this::toPaymentItem).collect(Collectors.toList());
        Order payment = Order.builder().address(address).items(items).build();

        PendingPayment pendingPayment = paymentService.create(payment, buildReturnUrl(request));
        URI approveUri = pendingPayment.getApproveUri();
        pendingOrders.compute(pendingPayment.getId(), (s, s2) -> buildAddress(addressLine1, addressLine2, postcode));

        return "redirect:" + approveUri;
    }

    private String buildAddress(String addressLine1, String addressLine2, String postcode) {
        return Stream.of(addressLine1, addressLine2, postcode).filter(s -> !StringUtils.isEmpty(s)).collect(Collectors.joining(", "));
    }

    private URI buildReturnUrl(HttpServletRequest request) {
        try {
            URI requestUri = URI.create(request.getRequestURL().toString());
            return new URI(requestUri.getScheme(), requestUri.getUserInfo(), requestUri.getHost(), requestUri.getPort(), "/order/complete", null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private OrderItem toPaymentItem(BasketItem basketItem) {
        return OrderItem.builder()
                .title(basketItem.getItem().getTitle())
                .value(basketItem.getTotal())
                .build();
    }

}
