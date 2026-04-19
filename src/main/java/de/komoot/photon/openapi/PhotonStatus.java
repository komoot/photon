package de.komoot.photon.openapi;

import io.javalin.openapi.OpenApiByFields;
import io.javalin.openapi.Visibility;

/**
 * Response body for the /status endpoint.
 */
@OpenApiByFields(Visibility.PUBLIC)
public class PhotonStatus {
    /** Always "Ok" when the server is running. */
    public String status;

    /** ISO 8601 timestamp of the last Nominatim import, or empty string if unknown. */
    public String import_date;

    /** Application version from the jar manifest. */
    public String version;

    /** Short git commit hash from the jar manifest. */
    public String git_commit;
}
