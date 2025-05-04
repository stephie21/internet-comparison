
package com.check24.internetcomparison.api;

import com.check24.internetcomparison.model.Address;
import com.check24.internetcomparison.model.InternetOffer;
import com.check24.internetcomparison.service.ComparisonServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class WebWunderClient implements ProviderClient {
    private final RestTemplate restTemplate = new RestTemplate(
            List.of(new StringHttpMessageConverter(StandardCharsets.UTF_8))
    );

    private static final Logger log = LoggerFactory.getLogger(ComparisonServiceImpl.class);

    @Value("${providers.webWunder.apiKey}")
    private String apiKey;

    @Override
    public List<InternetOffer> fetchOffers(Address address) {
        String url = "https://webwunder.gendev7.check24.fun/endpunkte/soap/ws/getInternetOffers.wsdl";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.set("X-Api-Key", apiKey);
        headers.set(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name());

        log.info("API-Key: " + apiKey);

        List<InternetOffer> offers = new ArrayList<>();
        try {
            String requestBody = buildSoapRequest(address);
            log.info("SOAP Request: {}", requestBody);

            HttpEntity<String> request = new HttpEntity<>(
                    buildSoapRequest(address), headers
            );

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);

            String xml = response.getBody();
            log.info("WebWunder Response XML: {}", xml);

            if (xml != null) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

                XPathFactory xPathFactory = XPathFactory.newInstance();
                XPath xpath = xPathFactory.newXPath();
                XPathExpression expr = xpath.compile("//*[local-name()='products']");

                NodeList productNodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                for (int i = 0; i < productNodes.getLength(); i++) {
                    Element product = (Element) productNodes.item(i);
                    String name = getTagValue(product, "providerName");
                    String priceStr = getTagValue(product, "monthlyCostInCent");

                    if (name != null && priceStr != null) {
                        try {
                            double price = Double.parseDouble(priceStr) / 100.0;
                            offers.add(new InternetOffer("WebWunder", name, price));
                        } catch (NumberFormatException e) {
                            log.warn("Ungültiger Preis bei WebWunder: {}", priceStr);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Fehler bei WebWunder SOAP/XPath: {}", e.getMessage());
        }

        return offers;
    }

    private String getTagValue(Element element, String tagName) {
        NodeList list = element.getElementsByTagNameNS("*", tagName);
        if (list.getLength() > 0) {
            Node node = list.item(0);
            return node.getTextContent().trim();
        }
        return null;
    }

    private String buildSoapRequest(Address address) {
        return """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                          xmlns:gs="http://webwunder.gendev7.check24.fun/offerservice">
           <soapenv:Header/>
           <soapenv:Body>
              <gs:legacyGetInternetOffers>
                 <gs:input>
                    <gs:installation>true</gs:installation>
                    <gs:connectionEnum>DSL</gs:connectionEnum>
                    <gs:address>
                       <gs:street>%s</gs:street>
                       <gs:houseNumber>%s</gs:houseNumber>
                       <gs:city>%s</gs:city>
                       <gs:plz>%s</gs:plz>
                       <gs:countryCode>DE</gs:countryCode>
                    </gs:address>
                 </gs:input>
              </gs:legacyGetInternetOffers>
           </soapenv:Body>
        </soapenv:Envelope>
        """.formatted(address.street(), address.houseNumber(), address.city(), address.zip());
    }


    @Override
    public String getProviderName() {
        return "WebWunder";
    }
}
