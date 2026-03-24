package com.devikapps.vaikaparts.config;

import dev.razafindratelo.airtel_money_client.api.AuthenticationApi;
import dev.razafindratelo.airtel_money_client.api.CollectionApi;
import dev.razafindratelo.airtel_money_client.api.TransactionApi;
import dev.razafindratelo.airtel_money_client.invoker.ApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AirtelMoneyRestTemplateConf {

  private final AirtelMoneyConf conf;

  @Bean
  public ApiClient airtelMoneyApiClient() {
    ApiClient client = new ApiClient();
    client.setBasePath(conf.getBaseUrl());
    return client;
  }

  @Bean
  public AuthenticationApi airtelMoneyAuthenticationApi(
      @Qualifier("airtelMoneyApiClient") ApiClient apiClient) {
    return new AuthenticationApi(apiClient);
  }

  @Bean
  public CollectionApi airtelMoneyCollectionApi(
      @Qualifier("airtelMoneyApiClient") ApiClient apiClient) {
    return new CollectionApi(apiClient);
  }

  @Bean
  public TransactionApi airtelMoneyTransactionApi(
      @Qualifier("airtelMoneyApiClient") ApiClient apiClient) {
    return new TransactionApi(apiClient);
  }
}
