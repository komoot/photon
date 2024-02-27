#!/bin/bash -e

# For Nominatim < 3.7 set this to the Nominatim build directory.
# For newer versions, this must be the project directory of your import.
: ${NOMINATIM_DIR:=.}
# Path to Photon Jar file
: ${PHOTON_JAR:=photon.jar}
# Name of Nominatim database.
: ${PHOTON_DB_NAME:=nominatim}
# PostgreSQL user name to use for update in Photon
: ${PHOTON_DB_USER:=nominatim}
# Password for PostgreSQL user
: ${PHOTON_DB_PASSWORD:=}


while true
do
    starttime=`date +%s`

    # First consume updates in the Nominatim database.
    # The important part here is to leave out the indexing step. This
    # will be handled by Photon.

    # For Nominatim versions < 3.7 use the following:
    # ./utils/update.php --import-osmosis
    nominatim replication --project-dir $NOMINATIM_DIR  --once

    # Now tell Photon to finish the updates and copy the new data into its
    # own database.
    java -jar $PHOTON_JAR -database $PHOTON_DB_NAME -user $PHOTON_DB_USER -password $PHOTON_DB_PASSWORD -nominatim-update

    # Sleep a bit if updates take less than a minute.
    # If you consume hourly or daily diffs adapt the period accordingly.
    endtime=`date +%s`
    elapsed=$((endtime - starttime))
    if [[ $elapsed -lt 60 ]]
    then
        sleepy=$((60 - $elapsed))
        echo "Sleeping for ${sleepy}s..."
        sleep $sleepy
    fi
done
