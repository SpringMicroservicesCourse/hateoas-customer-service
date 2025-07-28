package tw.fengqing.spring.springbucks.customer.support;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;

import java.util.Arrays;

/**
 * 自訂 Keep-Alive 策略，支援 httpclient5
 * 會解析 Keep-Alive header 的 timeout 參數，若無則預設 30 秒
 */
public class CustomConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {
    private final long DEFAULT_SECONDS = 30;

    @Override
    public TimeValue getKeepAliveDuration(HttpResponse response, HttpContext context) {
        return Arrays.asList(response.getHeaders("Keep-Alive"))
                .stream()
                .filter(h -> StringUtils.endsWithIgnoreCase(h.getName(), "timeout")
                        && StringUtils.isNumeric(h.getValue()))
                .findFirst()
                .map(h -> TimeValue.ofSeconds(NumberUtils.toLong(h.getValue(), DEFAULT_SECONDS)))
                .orElse(TimeValue.ofSeconds(DEFAULT_SECONDS));
    }
}
