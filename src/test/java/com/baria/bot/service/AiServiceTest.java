package com.baria.bot.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class AiServiceTest {
    @Test
    public void testSourcesIncluded() {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        RestTemplate template = builder.build();
        MockRestServiceServer server = MockRestServiceServer.createServer(template);
        server.expect(requestTo("http://localhost:8000/ask"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"answer\":\"ok\",\"sources\":[\"Doc1\"]}", MediaType.APPLICATION_JSON));
        AiService ai = new AiService(new RestTemplateBuilder().requestFactory(() -> template.getRequestFactory()));
        String res = ai.ask("q");
        assertTrue(res.contains("Doc1"));
        server.verify();
    }
}
