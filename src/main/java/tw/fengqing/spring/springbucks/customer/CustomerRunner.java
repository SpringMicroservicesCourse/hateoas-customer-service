package tw.fengqing.spring.springbucks.customer;

import tw.fengqing.spring.springbucks.customer.model.Coffee;
import tw.fengqing.spring.springbucks.customer.model.CoffeeOrder;
import tw.fengqing.spring.springbucks.customer.model.OrderState;
import lombok.extern.slf4j.Slf4j;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;

import java.net.URI;
import java.util.Map;

@Component
@Slf4j
public class CustomerRunner implements ApplicationRunner {
    private static final URI ROOT_URI = URI.create("http://localhost:8080/");
    @Autowired
    private RestTemplate restTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Link coffeeLink = getLink(ROOT_URI,"coffees");
        readCoffeeMenu(coffeeLink);
        String coffeeUri = addCoffee(coffeeLink);

        Link orderLink = getLink(ROOT_URI, "coffeeOrders");
        addOrder(orderLink, coffeeUri);
        queryOrders(orderLink);
    }

    private Link getLink(URI uri, String rel) {
        ResponseEntity<Map> rootResp =
                restTemplate.exchange(uri, HttpMethod.GET, null, Map.class);
        
        Map<String, Object> links = (Map<String, Object>) rootResp.getBody().get("_links");
        Map<String, Object> linkData = (Map<String, Object>) links.get(rel);
        String href = (String) linkData.get("href");
        
        Link link = Link.of(href, rel);
        log.info("Link: {}", link);
        return link;
    }

    private void readCoffeeMenu(Link coffeeLink) {
        ResponseEntity<PagedModel<EntityModel<Coffee>>> coffeeResp =
                restTemplate.exchange(coffeeLink.getTemplate().expand(),
                        HttpMethod.GET, null,
                        new ParameterizedTypeReference<PagedModel<EntityModel<Coffee>>>() {});
        log.info("Menu Response: {}", coffeeResp.getBody());
    }

    private String addCoffee(Link link) {
        Coffee americano = Coffee.builder()
                .name("americano")
                .price(Money.of(CurrencyUnit.of("TWD"), 125.0))
                .build();
        RequestEntity<Coffee> req =
                RequestEntity.post(link.getTemplate().expand()).body(americano);
        ResponseEntity<EntityModel<Coffee>> resp =
                restTemplate.exchange(req,
                        new ParameterizedTypeReference<EntityModel<Coffee>>() {});
        log.info("add Coffee Response: {}", resp);
        
        // 從 Location 標頭取得咖啡的 URI，不依賴固定 ID
        String coffeeUri = resp.getHeaders().getLocation().toString();
        log.info("Coffee URI from Location header: {}", coffeeUri);
        return coffeeUri;
    }

    private void addOrder(Link link, String coffeeUri) {
        CoffeeOrder newOrder = CoffeeOrder.builder()
                .customer("Ray Chu")
                .state(OrderState.INIT)
                .build();
        RequestEntity<?> req =
                RequestEntity.post(link.getTemplate().expand()).body(newOrder);
        ResponseEntity<Map> resp =
                restTemplate.exchange(req, Map.class);
        log.info("add Order Response: {}", resp);

        // 直接解析 JSON 回應中的 _links
        Map<String, Object> orderData = resp.getBody();
        Map<String, Object> links = (Map<String, Object>) orderData.get("_links");
        if (links != null && links.containsKey("items")) {
            Map<String, Object> itemsLinkData = (Map<String, Object>) links.get("items");
            String itemsHref = (String) itemsLinkData.get("href");
            Link items = Link.of(itemsHref, "items");
            
            // 使用從 Location 標頭取得的咖啡 URI
            req = RequestEntity.post(items.getTemplate().expand())
                    .contentType(MediaType.parseMediaType("text/uri-list"))
                    .body(coffeeUri);
            ResponseEntity<String> itemResp = restTemplate.exchange(req, String.class);
            log.info("add Order Items Response: {}", itemResp);
        } else {
            log.warn("No 'items' link found in order response");
        }
    }

    private void queryOrders(Link link) {
        ResponseEntity<String> resp = restTemplate.getForEntity(link.getTemplate().expand(), String.class);
        log.info("query Order Response: {}", resp);
    }
}
