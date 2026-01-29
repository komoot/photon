package de.komoot.photon.config;

import com.beust.jcommander.IParametersValidator;
import com.beust.jcommander.ParameterException;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

import static java.lang.Boolean.TRUE;

@NullMarked
public class CorsMutuallyExclusiveValidator implements IParametersValidator {
    @Override
    public void validate(Map<String, Object> parameters) throws ParameterException {
        if (parameters.get("-cors-any") == TRUE && parameters.get("-cors-origin") != null) {
            throw new ParameterException("Use only one cors configuration type.");
        }
    }
}
