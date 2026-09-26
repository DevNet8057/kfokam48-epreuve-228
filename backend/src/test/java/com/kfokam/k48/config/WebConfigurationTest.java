package com.kfokam.k48.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/**
 * K48-30 : sans configuration CORS, le préflight du navigateur (OPTIONS) est bloqué et le
 * frontend (origine distincte en développement/Docker, ex. http://localhost:5173) ne peut
 * appeler aucun endpoint de l'API, alors que les appels curl (sans en-tête Origin) passent.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void autorise_le_prevol_cors_depuis_l_origine_du_frontend() throws Exception {
        mockMvc.perform(options("/api/promotions")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }
}
