package it.polito.ai.backend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import it.polito.ai.backend.dtos.ConfigurationDTO;
import it.polito.ai.backend.dtos.TeamDTO;
import it.polito.ai.backend.services.vm.ConfigurationNotFoundException;
import it.polito.ai.backend.services.vm.VirtualMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/API/configurations")
@Validated
public class ConfigurationController {

    @Autowired
    VirtualMachineService virtualMachineService;

    @Operation(summary = "get configuration")
    @GetMapping("/{configurationId}")
    ResponseEntity<ConfigurationDTO> getOne(@PathVariable @NotNull Long configurationId) {
        ConfigurationDTO configurationDTO = virtualMachineService.getConfiguration(configurationId)
                .orElseThrow(() -> new ConfigurationNotFoundException(configurationId.toString()));
        Long teamId = virtualMachineService.getTeamForConfiguration(configurationId).map(TeamDTO::getId).orElse(null);

        return new ResponseEntity<>(ModelHelper.enrich(configurationDTO, teamId), HttpStatus.OK);
    }

    @Operation(summary = "create a new configuration")
    @PostMapping({"", "/"})
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<ConfigurationDTO> addConfiguration(@RequestBody @Valid ConfigurationDTO configurationDTO) {
        Long teamId = configurationDTO.getTeamId();
        ConfigurationDTO newConfigurationDTO = virtualMachineService.createConfiguration(teamId, configurationDTO);
        return new ResponseEntity<>(ModelHelper.enrich(newConfigurationDTO, teamId), HttpStatus.CREATED);
    }

    @Operation(summary = "update an existing configuration")
    @PutMapping("/{configurationId}")
    ResponseEntity<ConfigurationDTO> setConfiguration(@RequestBody @Valid ConfigurationDTO configurationDTO, @PathVariable @NotNull Long configurationId) {
        ConfigurationDTO configurationDTO1 = virtualMachineService.updateConfiguration(configurationId, configurationDTO);
        Long teamId = virtualMachineService.getTeamForConfiguration(configurationId).map(TeamDTO::getId).orElse(null);
        return new ResponseEntity<>(ModelHelper.enrich(configurationDTO1, teamId), HttpStatus.OK);
    }
}
