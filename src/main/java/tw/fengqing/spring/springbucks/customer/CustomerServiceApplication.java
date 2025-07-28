package tw.fengqing.spring.springbucks.customer;

import tw.fengqing.spring.springbucks.customer.support.CustomConnectionKeepAliveStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.client5.http.config.RequestConfig;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@SpringBootApplication
@Slf4j
public class CustomerServiceApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder()
				.sources(CustomerServiceApplication.class)
				.bannerMode(Banner.Mode.OFF)
				.web(WebApplicationType.NONE)
				.run(args);
	}

	@Bean
	public HttpComponentsClientHttpRequestFactory requestFactory() {
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(200); // 設定連線池最大連線數
		connectionManager.setDefaultMaxPerRoute(20); // 單一路由最大連線數

		// 設定 timeout
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(Timeout.ofMilliseconds(100))
				.setResponseTimeout(Timeout.ofMilliseconds(500))
				.build();

		CloseableHttpClient httpClient = HttpClients.custom()
				.setConnectionManager(connectionManager)
				.setDefaultRequestConfig(config)
				.evictIdleConnections(TimeValue.ofSeconds(30)) // 30秒閒置自動關閉
				.disableAutomaticRetries()
				.setKeepAliveStrategy(new CustomConnectionKeepAliveStrategy())
				.build();

		return new HttpComponentsClientHttpRequestFactory(httpClient);
	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder
				.requestFactory(this::requestFactory)
				.build();
	}
}
