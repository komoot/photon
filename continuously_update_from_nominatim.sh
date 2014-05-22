#!/bin/bash -e

while true
do
    starttime=`date +%s`

    cd $NOMINATIM_DIR
    ./utils/update.php --no-npi --import-osmosis --no-index

    curl http://localhost:2322/nominatim-update

    # sleep a bit if updates take less than a minute
    endtime=`date +%s`
    elapsed=$((endtime - starttime))
    if [[ $elapsed -lt 60 ]]
    then
        sleepy=$((60 - $elapsed))
        echo "Sleeping for ${sleepy}s..."
        sleep $sleepy
    fi
done