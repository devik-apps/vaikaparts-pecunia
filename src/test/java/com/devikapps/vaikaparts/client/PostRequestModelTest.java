package com.devikapps.vaikaparts.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.wso2.client.api.JSON.setGson;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.wso2.client.api.JSON;
import org.wso2.client.model.MVOLA_Merchant_Pay_API.PostRequest;
import org.wso2.client.model.MVOLA_Merchant_Pay_API.PostRequestDebitPartyInner;

class PostRequestModelTest {

  @BeforeAll
  static void initialize_gson() {
    setGson(JSON.createGson().create());
  }

  @Test
  void should_set_amount_correctly() {
    final PostRequest request = new PostRequest().amount("5000");
    assertEquals("5000", request.getAmount());
  }

  @Test
  void should_set_currency_correctly() {
    final PostRequest request = new PostRequest().currency("Ar");
    assertEquals("Ar", request.getCurrency());
  }

  @Test
  void should_set_description_text_correctly() {
    final PostRequest request = new PostRequest().descriptionText("Test payment");
    assertEquals("Test payment", request.getDescriptionText());
  }

  @Test
  void should_set_request_date_correctly() {
    final String date = "2026-03-16T10:00:00.000+0300";
    final PostRequest request = new PostRequest().requestDate(date);
    assertEquals(date, request.getRequestDate());
  }

  @Test
  void should_set_requesting_organisation_transaction_reference_correctly() {
    final PostRequest request =
        new PostRequest().requestingOrganisationTransactionReference("REF-001");
    assertEquals("REF-001", request.getRequestingOrganisationTransactionReference());
  }

  @Test
  void should_set_original_transaction_reference_correctly() {
    final PostRequest request = new PostRequest().originalTransactionReference("ORIG-001");
    assertEquals("ORIG-001", request.getOriginalTransactionReference());
  }

  @Test
  void should_return_null_for_unset_fields() {
    final PostRequest request = new PostRequest();
    assertNull(request.getAmount());
    assertNull(request.getCurrency());
    assertNull(request.getDescriptionText());
    assertNull(request.getRequestDate());
    assertNull(request.getDebitParty());
    assertNull(request.getCreditParty());
    assertNull(request.getMetadata());
  }

  @Test
  void should_add_debit_party_item() {
    final PostRequest request =
        new PostRequest()
            .addDebitPartyItem(new PostRequestDebitPartyInner().key("msisdn").value("0341234567"));

    assertNotNull(request.getDebitParty());
    assertEquals(1, request.getDebitParty().size());
    assertEquals("msisdn", request.getDebitParty().getFirst().getKey());
    assertEquals("0341234567", request.getDebitParty().getFirst().getValue());
  }

  @Test
  void should_add_credit_party_item() {
    final PostRequest request =
        new PostRequest()
            .addCreditPartyItem(new PostRequestDebitPartyInner().key("msisdn").value("0340017983"));

    assertNotNull(request.getCreditParty());
    assertEquals(1, request.getCreditParty().size());
    assertEquals("msisdn", request.getCreditParty().getFirst().getKey());
    assertEquals("0340017983", request.getCreditParty().getFirst().getValue());
  }

  @Test
  void should_add_multiple_metadata_items() {
    final PostRequest request =
        new PostRequest()
            .addMetadataItem(
                new PostRequestDebitPartyInner().key("partnerName").value("TestPartner"))
            .addMetadataItem(new PostRequestDebitPartyInner().key("fc").value("USD"))
            .addMetadataItem(new PostRequestDebitPartyInner().key("amountFc").value("1"));

    assertNotNull(request.getMetadata());
    assertEquals(3, request.getMetadata().size());
  }

  @Test
  void should_contain_partner_name_in_metadata() {
    final PostRequest request = MvolaApiTestBase.buildValidPostRequest();

    assertNotNull(request.getMetadata());
    final boolean hasPartnerName =
        request.getMetadata().stream()
            .anyMatch(m -> "partnerName".equals(m.getKey()) && "TestPartner".equals(m.getValue()));

    assertTrue(hasPartnerName);
  }

  @Test
  void should_contain_fc_in_metadata() {
    final PostRequest request = MvolaApiTestBase.buildValidPostRequest();

    assertNotNull(request.getMetadata());
    final boolean hasFc =
        request.getMetadata().stream()
            .anyMatch(m -> "fc".equals(m.getKey()) && "USD".equals(m.getValue()));

    assertTrue(hasFc);
  }

  @Test
  void should_serialize_all_expected_fields_to_json() {
    final PostRequest request = MvolaApiTestBase.buildValidPostRequest();
    final JsonObject json = JsonParser.parseString(request.toJson()).getAsJsonObject();

    assertTrue(json.has("amount"));
    assertTrue(json.has("currency"));
    assertTrue(json.has("descriptionText"));
    assertTrue(json.has("requestDate"));
    assertTrue(json.has("requestingOrganisationTransactionReference"));
    assertTrue(json.has("originalTransactionReference"));
    assertTrue(json.has("debitParty"));
    assertTrue(json.has("creditParty"));
    assertTrue(json.has("metadata"));
  }

  @Test
  void should_serialize_amount_value_correctly() {
    final PostRequest request = new PostRequest().amount("5000");
    final JsonObject json = JsonParser.parseString(request.toJson()).getAsJsonObject();

    assertEquals("5000", json.get("amount").getAsString());
  }

  @Test
  void should_serialize_currency_as_ar() {
    final PostRequest request = MvolaApiTestBase.buildValidPostRequest();
    final JsonObject json = JsonParser.parseString(request.toJson()).getAsJsonObject();

    assertEquals("Ar", json.get("currency").getAsString());
  }

  @Test
  void should_serialize_debit_party_as_array() {
    final PostRequest request = MvolaApiTestBase.buildValidPostRequest();
    final JsonObject json = JsonParser.parseString(request.toJson()).getAsJsonObject();

    assertTrue(json.get("debitParty").isJsonArray());
    assertEquals(1, json.getAsJsonArray("debitParty").size());
  }

  @Test
  void should_serialize_metadata_as_array_with_three_entries() {
    final PostRequest request = MvolaApiTestBase.buildValidPostRequest();
    final JsonObject json = JsonParser.parseString(request.toJson()).getAsJsonObject();

    assertTrue(json.get("metadata").isJsonArray());
    assertEquals(3, json.getAsJsonArray("metadata").size());
  }

  @Test
  void should_not_include_unknown_fields_in_serialized_json() {
    final PostRequest request = MvolaApiTestBase.buildValidPostRequest();
    final JsonObject json = JsonParser.parseString(request.toJson()).getAsJsonObject();

    for (final String key : json.keySet()) {
      assertTrue(
          PostRequest.openapiFields.contains(key),
          String.format("Unexpected field '%s' found in serialized JSON", key));
    }
  }

  @Test
  void should_consider_equal_instances_with_same_key_and_value() {
    final PostRequestDebitPartyInner a =
        new PostRequestDebitPartyInner().key("msisdn").value("0341234567");
    final PostRequestDebitPartyInner b =
        new PostRequestDebitPartyInner().key("msisdn").value("0341234567");

    assertEquals(a, b);
  }

  @Test
  void should_produce_same_hash_code_for_equal_instances() {
    final PostRequestDebitPartyInner a =
        new PostRequestDebitPartyInner().key("msisdn").value("0341234567");
    final PostRequestDebitPartyInner b =
        new PostRequestDebitPartyInner().key("msisdn").value("0341234567");

    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void should_not_consider_equal_instances_with_different_value() {
    final PostRequestDebitPartyInner a =
        new PostRequestDebitPartyInner().key("msisdn").value("0341234567");
    final PostRequestDebitPartyInner b =
        new PostRequestDebitPartyInner().key("msisdn").value("0349999999");

    assertNotEquals(a, b);
  }

  @Test
  void should_not_consider_equal_instances_with_different_key() {
    final PostRequestDebitPartyInner a =
        new PostRequestDebitPartyInner().key("msisdn").value("0341234567");
    final PostRequestDebitPartyInner b =
        new PostRequestDebitPartyInner().key("other").value("0341234567");

    assertNotEquals(a, b);
  }

  @Test
  void post_request_equals_itself() {
    final PostRequest request = MvolaApiTestBase.buildValidPostRequest();
    assertEquals(request, request);
  }

  @Test
  void post_request_not_equal_to_null() {
    final PostRequest request = MvolaApiTestBase.buildValidPostRequest();
    assertNotEquals(null, request);
  }

  @Test
  void should_produce_same_hash_for_two_identical_post_requests() {
    final PostRequest a = MvolaApiTestBase.buildValidPostRequest();
    final PostRequest b = MvolaApiTestBase.buildValidPostRequest();

    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void to_string_contains_class_name() {
    final PostRequest request = MvolaApiTestBase.buildValidPostRequest();
    assertTrue(request.toString().contains("PostRequest"));
  }

  @Test
  void to_string_contains_amount_value() {
    final PostRequest request = new PostRequest().amount("9999");
    assertTrue(request.toString().contains("9999"));
  }
}
