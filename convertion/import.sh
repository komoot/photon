for file in x*                                                                                                                                                                                 │············
do                                                                                                                                                                                             │············
        echo $file...                                                                                                                                                                          │············
        curl --silent --show-error -XPOST localhost:9200/_bulk  --data-binary @$file > /dev/null                                                                                               │············
done

# snippet for splitting files:
# split -l 300000 solr-131012.json