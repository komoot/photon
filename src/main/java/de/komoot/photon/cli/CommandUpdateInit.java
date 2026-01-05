package de.komoot.photon.cli;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import de.komoot.photon.config.PostgresqlConfig;
import de.komoot.photon.config.UpdateInitConfig;

@Parameters(commandDescription = """
        Set up a Nominatim database for updating a Photon database.
        
        In order to be able to track changes, the Photon process needs write
        access to a single table in the Nominatim database which records the
        changed data.
        
        This initialisation process creates the tracking table and sets the
        appropriate access rights. Therefore this command needs to be run
        with a Postgres user with rights to create tables. The updates can
        later be run with a different user with read-access only. The
        initalisation will give this user the delete right for the tracking table
        it needs in order to mark data as processed.
        """)
public class CommandUpdateInit {
    public CommandUpdateInit(UpdateInitConfig cfg, PostgresqlConfig pgCfg) {
        updateInitConfig = cfg;
        postgresqlConfig = pgCfg;
    }

    @ParametersDelegate
    private final UpdateInitConfig updateInitConfig;

    @ParametersDelegate
    private final PostgresqlConfig postgresqlConfig;
}
