package de.komoot.photon.config;

import com.beust.jcommander.IParametersValidator;
import com.beust.jcommander.ParameterException;

import java.util.List;
import java.util.Map;

import static java.lang.Boolean.TRUE;

public class LegacyMutuallyExclusiveValidator implements IParametersValidator {
    @Override
    public void validate(Map<String, Object> parameters) throws ParameterException {
        int numCommands = 0;
        for (String param : List.of("-nominatim-import", "-nominatim-update")) {
            if (parameters.get(param) == TRUE) {
                numCommands += 1;
            }
        }
        if (parameters.get("-nominatim-update-init-for") != null) {
            numCommands += 1;
        }

        if (numCommands > 1) {
            throw new ParameterException("Only one of -nominatim-import, -nominatim-update-init-for, -nominatim-update may be used.");
        }
    }
}
