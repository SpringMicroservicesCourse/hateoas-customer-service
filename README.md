# HATEOAS Customer Service ⚡

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 專案介紹

這是一個展示 **HATEOAS (Hypermedia as the Engine of Application State)** 實作的 Spring Boot 客戶端服務，主要用於與 waiter-service 進行 RESTful API 互動。專案展示了如何透過 HATEOAS 連結動態探索和操作 REST API 資源。

### 🎯 專案特色

- **HATEOAS 動態連結探索** - 透過 API 回應中的 `_links` 自動發現可用資源
- **Spring Data REST 整合** - 與 waiter-service 完美配合，支援自動化 REST 端點
- **現代化技術棧** - 使用 Spring Boot 3.x、Java 21、HttpClient 5.x
- **連線池最佳化** - 自訂 HTTP 連線管理策略，提升效能
- **完整的訂單流程** - 從咖啡菜單查詢到訂單建立與關聯

> 💡 **為什麼選擇 HATEOAS？**
> - **鬆耦合架構** - 客戶端不需要預先知道所有 API 端點
> - **動態探索能力** - 透過連結自動發現可用操作
> - **REST 標準遵循** - 符合 Richardson Maturity Model 最高層級
> - **微服務友好** - 適合分散式系統的服務間通訊

## 技術棧

### 核心框架
- **Spring Boot 3.2.5** - 現代化 Java 應用程式框架
- **Spring HATEOAS** - 提供 HATEOAS 支援和連結管理
- **Spring Data REST** - 自動化 REST API 端點生成

### HTTP 客戶端與連線管理
- **HttpClient 5.3.1** - 新一代 Apache HTTP 客戶端
- **PoolingHttpClientConnectionManager** - 連線池管理
- **CustomConnectionKeepAliveStrategy** - 自訂連線存活策略

### 開發工具與輔助
- **Lombok** - 減少樣板程式碼
- **Joda Money** - 貨幣處理庫
- **Apache Commons Lang3** - 工具類庫

## 專案結構

```
hateoas-customer-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── tw/fengqing/spring/springbucks/customer/
│   │   │       ├── CustomerServiceApplication.java    # 主應用程式類別
│   │   │       ├── CustomerRunner.java               # 業務邏輯執行器
│   │   │       ├── model/                           # 資料模型
│   │   │       │   ├── Coffee.java                  # 咖啡實體
│   │   │       │   ├── CoffeeOrder.java             # 咖啡訂單實體
│   │   │       │   └── OrderState.java              # 訂單狀態列舉
│   │   │       └── support/                         # 支援類別
│   │   │           └── CustomConnectionKeepAliveStrategy.java
│   │   └── resources/
│   │       └── application.properties               # 應用程式設定
│   └── test/
├── pom.xml                                         # Maven 專案配置
└── README.md
```

## 快速開始

### 前置需求
- **Java 21** - 確保已安裝 JDK 21
- **Maven 3.8+** - 專案建置工具
- **waiter-service** - 需要先啟動 waiter-service 服務 (port 8080)

### 安裝與執行

1. **克隆此倉庫：**
```bash
git clone https://github.com/SpringMicroservicesCourse/spring-microservices-demo.git
```

2. **編譯專案：**
```bash
mvn clean compile
```

3. **執行應用程式：**
```bash
mvn spring-boot:run
```

### 執行流程說明

應用程式啟動後會自動執行以下流程：

1. **探索 API 根端點** - 從 `http://localhost:8080/api` 取得可用資源連結
2. **查詢咖啡菜單** - 透過 `coffees` 連結取得所有咖啡資訊
3. **新增咖啡** - 建立一杯美式咖啡 (TWD 125)
4. **建立訂單** - 為客戶 "Li Lei" 建立新訂單
5. **關聯咖啡到訂單** - 將新增的咖啡加入訂單項目
6. **查詢所有訂單** - 顯示完整的訂單清單

## 重要程式碼解析

### HATEOAS 連結探索機制

```java
/**
 * 從 API 根端點動態取得指定資源的連結
 * 使用 Map 解析 HAL 格式的 _links 物件，避免 HATEOAS 物件序列化問題
 */
private Link getLink(URI uri, String rel) {
    // 向根端點發送 GET 請求，取得 HAL 格式的回應
    ResponseEntity<Map> rootResp = 
        restTemplate.exchange(uri, HttpMethod.GET, null, Map.class);
    
    // 解析 _links 物件，取得指定 rel 的連結資訊
    Map<String, Object> links = (Map<String, Object>) rootResp.getBody().get("_links");
    Map<String, Object> linkData = (Map<String, Object>) links.get(rel);
    String href = (String) linkData.get("href");
    
    // 建立 Link 物件供後續使用
    Link link = Link.of(href, rel);
    log.info("Link: {}", link);
    return link;
}
```

### 訂單關聯處理 (Spring Data REST 標準)

```java
/**
 * 將咖啡關聯到訂單項目
 * 使用 text/uri-list 格式，符合 Spring Data REST 官方規範
 * 這是 ManyToMany 關聯的標準做法
 */
private void addOrder(Link link, EntityModel<Coffee> coffee) {
    // 建立新訂單
    CoffeeOrder newOrder = CoffeeOrder.builder()
            .customer("Li Lei")
            .state(OrderState.INIT)
            .build();
    
    // 發送 POST 請求建立訂單
    RequestEntity<?> req = RequestEntity.post(link.getTemplate().expand()).body(newOrder);
    ResponseEntity<Map> resp = restTemplate.exchange(req, Map.class);
    
    // 解析訂單回應，取得 items 連結
    Map<String, Object> orderData = resp.getBody();
    Map<String, Object> links = (Map<String, Object>) orderData.get("_links");
    Map<String, Object> itemsLinkData = (Map<String, Object>) links.get("items");
    String itemsHref = (String) itemsLinkData.get("href");
    Link items = Link.of(itemsHref, "items");
    
    // 使用 text/uri-list 格式關聯咖啡到訂單
    String coffeeUri = lastCreatedCoffeeUri; // 從 Location header 取得的 URI
    req = RequestEntity.post(items.getTemplate().expand())
            .contentType(MediaType.parseMediaType("text/uri-list"))
            .body(coffeeUri);
    ResponseEntity<String> itemResp = restTemplate.exchange(req, String.class);
}
```

### HTTP 連線池配置

```java
/**
 * 配置 HttpClient5 連線池與自訂策略
 * 提供高效能的 HTTP 連線管理
 */
@Bean
public HttpComponentsClientHttpRequestFactory requestFactory() {
    // 設定連線池管理器
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(200);           // 最大連線數
    connectionManager.setDefaultMaxPerRoute(20);  // 單一路由最大連線數

    // 設定請求超時配置
    RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(100))    // 連線超時
            .setResponseTimeout(Timeout.ofMilliseconds(500))   // 回應超時
            .build();

    // 建立 HttpClient 實例
    CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(config)
            .evictIdleConnections(TimeValue.ofSeconds(30))    // 閒置連線清理
            .disableAutomaticRetries()
            .setKeepAliveStrategy(new CustomConnectionKeepAliveStrategy())
            .build();

    return new HttpComponentsClientHttpRequestFactory(httpClient);
}
```

## 進階說明

### 環境變數
```properties
# 應用程式設定
spring.application.name=customer-service
logging.level.tw.fengqing.spring.springbucks.customer=DEBUG

# HTTP 客戶端設定
http.client.connect-timeout=100
http.client.response-timeout=500
http.client.max-connections=200
http.client.max-per-route=20
```

### 與 waiter-service 互動流程

1. **服務發現** - 透過 `/api` 端點探索可用資源
2. **資源操作** - 使用 HATEOAS 連結進行 CRUD 操作
3. **關聯管理** - 透過 `text/uri-list` 格式管理 ManyToMany 關聯

## 參考資源

- [Spring HATEOAS 官方文件](https://spring.io/projects/spring-hateoas)
- [Spring Data REST 參考指南](https://docs.spring.io/spring-data/rest/docs/current/reference/html/)
- [HAL 規格說明](https://tools.ietf.org/html/draft-kelly-json-hal-08)
- [HttpClient 5.x 文件](https://hc.apache.org/httpcomponents-client-5.2.x/)

## 注意事項與最佳實踐

### ⚠️ 重要提醒

| 項目 | 說明 | 建議做法 |
|------|------|----------|
| HATEOAS 連結解析 | 避免直接使用 HATEOAS 物件 | 使用 Map 解析 HAL 格式 |
| 訂單關聯 | ManyToMany 關聯格式 | 使用 `text/uri-list` 格式 |
| HTTP 連線管理 | 避免連線洩漏 | 使用連線池和 KeepAlive 策略 |
| 錯誤處理 | 網路異常處理 | 添加重試機制和超時設定 |

### 🔒 最佳實踐指南

- **HATEOAS 設計原則** - 確保 API 回應包含完整的連結資訊
- **連線池管理** - 適當配置連線池大小和超時設定
- **錯誤處理** - 實作完善的異常處理和重試機制
- **日誌記錄** - 添加詳細的除錯日誌，方便問題診斷

## 授權說明

本專案採用 MIT 授權條款，詳見 LICENSE 檔案。

## 關於我們

我們主要專注在敏捷專案管理、物聯網（IoT）應用開發和領域驅動設計（DDD）。喜歡把先進技術和實務經驗結合，打造好用又靈活的軟體解決方案。

## 聯繫我們

- **FB 粉絲頁**：[風清雲談 | Facebook](https://www.facebook.com/profile.php?id=61576838896062)
- **LinkedIn**：[linkedin.com/in/chu-kuo-lung](https://www.linkedin.com/in/chu-kuo-lung)
- **YouTube 頻道**：[雲談風清 - YouTube](https://www.youtube.com/channel/UCXDqLTdCMiCJ1j8xGRfwEig)
- **風清雲談 部落格**：[風清雲談](https://blog.fengqing.tw/)
- **電子郵件**：[fengqing.tw@gmail.com](mailto:fengqing.tw@gmail.com)

---

**📅 最後更新：2025-07-28**  
**👨‍💻 維護者：風清雲談團隊** 