package ma.qlinkr;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/** A real STOMP client on /ws: the staff screen and the customer's phone receive changes without polling. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LiveUpdatesTest extends IntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    private Fixtures.Shop shop;
    private Fixtures.Shop other;
    private long tea;

    @BeforeAll
    void setUp() {
        shop = Fixtures.shop(this, "live", "fr");
        other = Fixtures.shop(this, "live-other", "fr");
        tea = Fixtures.item(this, shop, Fixtures.threeLanguages("Thé", "Tea", "شاي"), 1000, null);
    }

    private StompSession connect(String token) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        StompHeaders headers = new StompHeaders();
        if (token != null) {
            headers.add("Authorization", "Bearer " + token);
        }
        return client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), headers,
            new StompSessionHandlerAdapter() { }).get(10, TimeUnit.SECONDS);
    }

    private static BlockingQueue<Map<String, Object>> subscribe(StompSession session, String destination) {
        BlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((Map<String, Object>) payload);
            }
        });
        return received;
    }

    private String placeOrder() {
        Map<?, ?> body = RestClient.create("http://localhost:" + port).post()
            .uri("/api/public/scan/" + shop.scanCode() + "/orders")
            .contentType(MediaType.APPLICATION_JSON).header("Idempotency-Key", UUID.randomUUID().toString())
            .body(Map.of("lines", List.of(Map.of("itemId", tea, "quantity", 1))))
            .retrieve().body(Map.class);
        return (String) body.get("token");
    }

    @Test
    void newOrderAppearsOnTheStaffScreenAndStatusReachesThePhone() throws Exception {
        StompSession staff = connect(shop.staffToken());
        BlockingQueue<Map<String, Object>> staffScreen = subscribe(staff, "/topic/business." + shop.businessId() + ".orders");
        Thread.sleep(300); // let the subscription register

        String token = placeOrder();
        Map<String, Object> arrived = staffScreen.poll(10, TimeUnit.SECONDS);
        assertThat(arrived).isNotNull().containsEntry("status", "NEW").containsEntry("scanPointLabel", "Table 4");

        StompSession phone = connect(null);
        BlockingQueue<Map<String, Object>> customer = subscribe(phone, "/topic/orders." + token);
        Thread.sleep(300);
        long id = ((Number) arrived.get("id")).longValue();
        assertThat(call(HttpMethod.POST, "/api/orders/" + id + "/status", shop.staffToken(), Map.of("to", "ACCEPTED"))
            .status()).isEqualTo(200);

        Map<String, Object> update = customer.poll(10, TimeUnit.SECONDS);
        assertThat(update).isNotNull().containsEntry("status", "ACCEPTED").containsEntry("token", token);
        staff.disconnect();
        phone.disconnect();
    }

    @Test
    void staffOfAnotherBusinessCannotListen() throws Exception {
        StompSession spy = connect(other.staffToken());
        BlockingQueue<Map<String, Object>> stolen = subscribe(spy, "/topic/business." + shop.businessId() + ".orders");
        Thread.sleep(300);

        placeOrder();

        assertThat(stolen.poll(2, TimeUnit.SECONDS)).isNull();
    }

    @Test
    void anonymousClientsCannotListenToABusiness() throws Exception {
        StompSession anonymous = connect(null);
        BlockingQueue<Map<String, Object>> stolen = subscribe(anonymous, "/topic/business." + shop.businessId() + ".orders");
        Thread.sleep(300);

        placeOrder();

        assertThat(stolen.poll(2, TimeUnit.SECONDS)).isNull();
    }
}
