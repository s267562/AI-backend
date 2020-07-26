package it.polito.ai.backend.controllers;

import it.polito.ai.backend.dtos.ConfigurationDTO;
import it.polito.ai.backend.dtos.TeamDTO;
import it.polito.ai.backend.services.vm.ConfigurationNotFoundException;
import it.polito.ai.backend.services.vm.VirtualMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/API/configurations")
@Validated
public class ConfigurationController {

    @Autowired
    VirtualMachineService virtualMachineService;

    @GetMapping("/{configurationId}")
    ConfigurationDTO getOne(@PathVariable @NotNull Long configurationId) {
        ConfigurationDTO configurationDTO = virtualMachineService.getConfiguration(configurationId)
                .orElseThrow(() -> new ConfigurationNotFoundException(configurationId.toString()));
        Long teamId = virtualMachineService.getTeamForConfiguration(configurationId).map(TeamDTO::getId).orElse(null);

        return ModelHelper.enrich(configurationDTO, teamId);
    }

    @PostMapping({"", "/"})
    @ResponseStatus(HttpStatus.CREATED)
    ConfigurationDTO addConfiguration(@RequestBody @Valid ConfigurationDTO configurationDTO) {
        // todo
        Long teamId = 0L;
        ConfigurationDTO newConfigurationDTO = virtualMachineService.createConfiguration(teamId, configurationDTO);
        return ModelHelper.enrich(newConfigurationDTO, teamId);
    }

    @PutMapping("/{configurationId}")
    ConfigurationDTO setConfiguration(@RequestBody @Valid ConfigurationDTO configurationDTO, @PathVariable @NotNull Long configurationId) {
        ConfigurationDTO configurationDTO1 = virtualMachineService.updateConfiguration(configurationDTO);
        Long teamId = virtualMachineService.getTeamForConfiguration(configurationId).map(TeamDTO::getId).orElse(null);
        return ModelHelper.enrich(configurationDTO1, teamId);
    }
}
