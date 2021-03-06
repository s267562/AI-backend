package it.polito.ai.backend.services.vm;

public class VirtualMachineNotFoundException extends VirtualMachineServiceNotFoundException {
    public VirtualMachineNotFoundException(String id) {
        super(String.format("virtual machine %s not found", id));
    }
}
