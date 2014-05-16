package de.komoot.photon.importer;

import org.elasticsearch.client.Client;
import com.beust.jcommander.*;


import de.komoot.photon.importer.elasticsearch.Server;
import de.komoot.photon.importer.nominatim.Exporter;



public class App {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(App.class);
    public static void main(String[] args)
    {

        JCommanderStdin jct = new JCommanderStdin();
        new JCommander(jct, args);

        Server esNode = new Server("photon");
        esNode.start();


        Client esNodeClient = esNode.getClient();

        if(jct.isIndexer())
        {
            Exporter exporter = new Exporter(jct.getHost(), jct.getPort(), jct.getDatabase(), jct.getUser(), jct.getPassword());
            exporter.export();
        }

        esNode.shutdown();

    }


}
