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
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

@Component
@Slf4j
public class CustomerRunner implements ApplicationRunner {
    private static final URI ROOT_URI = URI.create("http://localhost:8080/api");
    @Autowired
    private RestTemplate restTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Link coffeeLink = getLink(ROOT_URI,"coffees");
        readCoffeeMenu(coffeeLink);
        EntityModel<Coffee> americano = addCoffee(coffeeLink);

        Link orderLink = getLink(ROOT_URI, "coffeeOrders");
        addOrder(orderLink, americano);
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

    private void addOrder(Link link, EntityModel<Coffee> coffee) {
        log.info("進入 addOrder 方法");
        CoffeeOrder newOrder = CoffeeOrder.builder()
                .customer("Li Lei")
                .state(OrderState.INIT)
                .build();
        RequestEntity<?> req =
                RequestEntity.post(link.getTemplate().expand()).body(newOrder);
        ResponseEntity<Map> resp =
                restTemplate.exchange(req, Map.class);
        log.info("add Order Response: {}", resp);

        Map<String, Object> orderData = resp.getBody();
        log.info("orderData: {}", orderData);
        if (orderData == null) {
            log.error("orderData is null");
            return;
        }
        Map<String, Object> links = (Map<String, Object>) orderData.get("_links");
        log.info("links: {}", links);
        if (links == null) {
            log.error("_links is null");
            return;
        }
        Map<String, Object> itemsLinkData = (Map<String, Object>) links.get("items");
        log.info("itemsLinkData: {}", itemsLinkData);
        if (itemsLinkData == null) {
            log.error("itemsLinkData is null");
            return;
        }
        String itemsHref = (String) itemsLinkData.get("href");
        Link items = Link.of(itemsHref, "items");

        String coffeeUri = lastCreatedCoffeeUri;
        req = RequestEntity.post(items.getTemplate().expand())
                .contentType(org.springframework.http.MediaType.parseMediaType("text/uri-list"))
                .body(coffeeUri);
        ResponseEntity<String> itemResp = restTemplate.exchange(req, String.class);
        log.info("add Order Items Response: {}", itemResp);
    }

    // 新增一個欄位來保存上一次新增咖啡的 URI
    private String lastCreatedCoffeeUri;

    private EntityModel<Coffee> addCoffee(Link link) {
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
        // 保存新建咖啡的 URI
        lastCreatedCoffeeUri = resp.getHeaders().getLocation().toString();
        return resp.getBody();
    }

    private void queryOrders(Link link) {
        ResponseEntity<String> resp = restTemplate.getForEntity(link.getTemplate().expand(), String.class);
        log.info("query Order Response: {}", resp);
    }
}
