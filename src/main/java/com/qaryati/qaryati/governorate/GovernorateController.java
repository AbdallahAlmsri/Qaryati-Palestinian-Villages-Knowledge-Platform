package com.qaryati.qaryati.governorate;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/governorates")
public class GovernorateController {

    private final GovernorateService governorateService;

    public GovernorateController(GovernorateService governorateService) {
        this.governorateService = governorateService;
    }

    @GetMapping
    public List<GovernorateResponse> getGovernorates() {
        return governorateService.getGovernorates()
                .stream()
                .map(GovernorateResponse::from)
                .toList();
    }

    @GetMapping("/{governorateId}/municipalities")
    public List<MunicipalityResponse> getMunicipalities(@PathVariable Long governorateId) {
        return governorateService.getMunicipalities(governorateId)
                .stream()
                .map(MunicipalityResponse::from)
                .toList();
    }
}
