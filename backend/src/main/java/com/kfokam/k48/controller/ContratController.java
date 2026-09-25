package com.kfokam.k48.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * `spring.web.resources.add-mappings=false` désactive volontairement le service de fichiers
 * statiques (aucune ressource arbitraire ne doit être exposée). Seul le contrat d'API, source
 * unique de vérité affichée par Swagger UI (springdoc.swagger-ui.url), a besoin d'être servi.
 */
@RestController
public class ContratController {

    @GetMapping(value = "/api/contrat.yaml", produces = "application/yaml")
    public Resource contrat() {
        return new ClassPathResource("api/contrat.yaml");
    }
}
