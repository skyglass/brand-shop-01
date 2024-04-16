package com.cakefactory.order;

import com.cakefactory.account.AccountService;
import com.cakefactory.auth.SecurityConfiguration;
import com.cakefactory.basket.Basket;
import com.cakefactory.basket.BasketItem;
import com.cakefactory.catalog.Item;
import com.cakefactory.payment.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
@Import({SecurityConfiguration.class, OrderTestConfiguration.class})
class OrderControllerTest {

    private final BasketItem ITEM_1 = new BasketItem(new Item("rv", "Red Velvet", BigDecimal.TEN), 2);
    private final BasketItem ITEM_2 = new BasketItem(new Item("bg", "Baguette", BigDecimal.ONE), 1);
    private final String ADDRESS_LINE_1 = "line 1";
    private final String ADDRESS_LINE_2 = "line 2";
    private final String POSTCODE = "P1 ST";
    private final String EXPECTED_ORDER_ID = "order-id";
    private final String EXPECTED_URL = "https://example.com/approve/123";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestOrderListener testOrderListener;

    @MockBean
    PaymentService paymentService;

    @MockBean
    Basket basket;

    @MockBean
    AccountService accountService;

    @BeforeEach
    void setUp() {
        when(basket.getItems()).thenReturn(
                List.of(
                        ITEM_1,
                        ITEM_2
                ));

        when(paymentService.create(any(), any())).thenReturn(new PendingPayment(EXPECTED_ORDER_ID, URI.create(EXPECTED_URL)));
    }

    @Test
    void createsPaymentWithAddress() throws Exception {
        createOrder();

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(paymentService).create(captor.capture(), any());

        Address expectedAddress = Address.builder().line1("line 1").line2("line 2").postcode("P1 ST").build();
        assertThat(captor.getValue().getAddress()).isEqualTo(expectedAddress);
    }

    @Test
    void createsPaymentWithItems() throws Exception {
        createOrder();

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(paymentService).create(captor.capture(), any());

        assertThat(captor.getValue().getItems()).containsExactlyInAnyOrder(
                OrderItem.builder().title("Red Velvet").value(BigDecimal.valueOf(20)).build(),
                OrderItem.builder().title("Baguette").value(BigDecimal.valueOf(1)).build()
        );
    }

    @Test
    void redirectsToApprovalUrl() throws Exception {
        mockMvc.perform(post("/order")
                .param("addressLine1", "line 1")
                .param("addressLine2", "line 2")
                .param("postcode", "P1 ST")
                .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", EXPECTED_URL));
    }

    @Test
    void passesReturnUrl() throws Exception {
        createOrder();

        ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);
        verify(paymentService).create(any(), captor.capture());

        assertThat(captor.getValue()).isEqualTo(URI.create("http://localhost/order/complete"));
    }

    @Test
    void completesOrder() throws Exception {
        createOrder();

        mockMvc.perform(get("/order/complete").queryParam("token", EXPECTED_ORDER_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/order"));

        verify(paymentService).complete(EXPECTED_ORDER_ID);
    }

    @Test
    void redirectsToIndexOnError() throws Exception {
        createOrder();

        when(paymentService.complete(EXPECTED_ORDER_ID)).thenThrow(new IllegalArgumentException("Invalid order id"));

        mockMvc.perform(get("/order/complete").queryParam("token", EXPECTED_ORDER_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/"));

        verify(basket, times(0)).clear(); // no need to clear basket or error
    }

    @Test
    void clearsBasket() throws Exception {
        createOrder();
        completeOrder(EXPECTED_ORDER_ID);

        verify(this.basket).clear();
    }

    @Test
    void publishesOrder() throws Exception {
        createOrder();
        completeOrder(EXPECTED_ORDER_ID);

        OrderReceivedEvent lastEvent = testOrderListener.getLastEvent();
        assertThat(lastEvent.getItems()).containsExactly(ITEM_1, ITEM_2);
        assertThat(lastEvent.getDeliveryAddress()).isEqualTo(String.format("%s, %s, %s", ADDRESS_LINE_1, ADDRESS_LINE_2, POSTCODE));
    }

    private void completeOrder(String expectedOrderId) throws Exception {
        mockMvc.perform(get("/order/complete").queryParam("token", expectedOrderId))
                .andExpect(status().is3xxRedirection());
    }

    private void createOrder() throws Exception {
        mockMvc.perform(post("/order")
                .param("addressLine1", ADDRESS_LINE_1)
                .param("addressLine2", ADDRESS_LINE_2)
                .param("postcode", POSTCODE)
                .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}