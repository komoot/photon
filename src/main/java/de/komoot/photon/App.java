package de.komoot.photon;

import de.komoot.photon.cli.Commands;
import de.komoot.photon.cli.PhotonCli;
import de.komoot.photon.config.*;
import de.komoot.photon.json.JsonDumper;
import de.komoot.photon.json.JsonReader;
import de.komoot.photon.metrics.MetricsConfig;
import de.komoot.photon.nominatim.ImportThread;
import de.komoot.photon.nominatim.NominatimImporter;
import de.komoot.photon.nominatim.NominatimUpdater;
import de.komoot.photon.query.*;
import de.komoot.photon.searcher.GeocodeJsonFormatter;
import de.komoot.photon.searcher.TagFilter;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Envelope;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static de.komoot.photon.metrics.MetricsConfig.setupMetrics;

/**
 * Main Photon application.
 */
@NullMarked
public class App {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicReference<@Nullable Server> esServer = new AtomicReference<>();
    @Nullable private static Javalin photonServer;

    public static void main(String[] rawArgs) throws Exception {
        PhotonCli cli = new PhotonCli();
        var command = cli.parse(rawArgs);

        if (command == null) {
            System.exit(1);
        }

        try {
            if (runPhoton(command, cli)) {
                shutdown();
            }
        } catch (UsageException e) {
            LOGGER.error(e.getMessage());
            shutdown();
            System.exit(2);
        }
    }

    public static void shutdown() {
        LOGGER.info("Shutting down the service.");
        if (photonServer != null) {
            photonServer.stop();
            photonServer = null;
        }
        final Server temp = esServer.getAndSet(null);
        if (temp != null) {
            temp.shutdown();
        }
    }

    private static boolean runPhoton(Commands command, PhotonCli cli) throws IOException {
        if (command == Commands.CMD_JSON_DUMP) {
            startJsonDump(cli);
            return true;
        }

        if (command == Commands.CMD_UPDATE_INIT) {
            startNominatimUpdateInit(cli.getPostgresqlConfig(), cli.getUpdateInitConfig());
            return true;
        }

        LOGGER.info("Start up database cluster, this might take some time.");
        var server = new Server(cli.getPhotonDBConfig(), command == Commands.CMD_IMPORT);
        esServer.set(server);
        LOGGER.info("Database cluster is now ready.");

        if (command == Commands.CMD_IMPORT) {
            startNominatimImport(cli, server);
            return true;
        }

        if (command == Commands.CMD_UPDATE) {
            startNominatimUpdate(
                    setupNominatimUpdater(cli.getPostgresqlConfig(), cli.getImportFilterConfig(), server),
                    server);
            return true;
        }

        // No special action specified -> normal mode: start search API
        startApi(
                cli.getApiServerConfig(),
                server,
                cli.getApiServerConfig().isEnableUpdateApi() ? setupNominatimUpdater(cli.getPostgresqlConfig(), cli.getImportFilterConfig(), server) : null);

        return false;
    }

    /**
     * Take nominatim data and dump it to a Json file.
     */
    private static void startJsonDump(PhotonCli cli) {
        final var dbProps = cli.getImportFilterConfig().getDatabaseProperties();

        try {
            final var connector = new NominatimImporter(cli.getPostgresqlConfig(), dbProps);
            dbProps.setImportDate(connector.getLastImportDate());

            final String filename = cli.getExportDumpConfig().getExportFile();
            if (filename == null) {
                throw new UsageException("-export-file required for json export.");
            }
            final JsonDumper jsonDumper = new JsonDumper(filename, dbProps);
            jsonDumper.writeHeader(connector.loadCountryNames(dbProps.getLanguages()));

            final var importThread = new ImportThread(jsonDumper);
            try {
                importFromDatabase(cli.getPostgresqlConfig(),
                        cli.getGeneralConfig().getThreads(),
                        cli.getImportFilterConfig().getCountryCodes(),
                        importThread,
                        dbProps);
            } finally {
                importThread.finish();
            }
            LOGGER.info("Json dump was created: {}", filename);
        } catch (IOException e) {
            throw new UsageException("Cannot create dump: " + e.getMessage());
        }
    }


    /**
     * Read all data from a Nominatim database and import it into a Photon database.
     */
    private static void startNominatimImport(PhotonCli cli, Server esServer) {
        final var importFilterConfig = cli.getImportFilterConfig();
        final var dbProperties = importFilterConfig.getDatabaseProperties();

        try {
            LOGGER.info("Reinitializing database index with languages {}.", String.join(",", dbProperties.getLanguages()));
            esServer.recreateIndex(dbProperties);
        } catch (IOException ex) {
            LOGGER.error("Cannot initialize database", ex);
            return;
        }

        final var importThread = new ImportThread(esServer.createImporter(dbProperties));

        try {
            Date importDate;
            if (cli.getImportFileConfig().isEnabled()) {
                importDate = importFromFile(cli.getImportFileConfig(), importFilterConfig, importThread);
            } else {
                importDate = importFromDatabase(
                        cli.getPostgresqlConfig(),
                        cli.getGeneralConfig().getThreads(),
                        importFilterConfig.getCountryCodes(),
                        importThread,
                        dbProperties);
            }
            dbProperties.setImportDate(importDate);
            esServer.saveToDatabase(dbProperties);
        } catch (IOException ex) {
            LOGGER.error("IO error while importing", ex);
            return;
        } finally {
            importThread.finish();
        }

        LOGGER.info("Database has been successfully set up with the following properties:\n{}", dbProperties);
    }

    @Nullable
    private static Date importFromDatabase(PostgresqlConfig postgresqlConfig, int numThreads, String[] filterCountries,
                                           ImportThread importThread, DatabaseProperties dbProperties) {
        LOGGER.info("Connecting to {}", postgresqlConfig.toString());
        final var connector = new NominatimImporter(postgresqlConfig, dbProperties);
        connector.prepareDatabase();
        connector.loadCountryNames(dbProperties.getLanguages());

        String[] countries;

        if (filterCountries.length == 0) {
            countries = connector.getCountriesFromDatabase();
        } else {
            countries = Arrays.stream(filterCountries)
                    .map(String::trim)
                    .filter(s -> !s.isBlank()).toArray(String[]::new);
        }

        if (numThreads == 1) {
            for (var country : countries) {
                connector.readCountry(country, importThread);
            }
        } else {
            final Queue<String> todolist = new ConcurrentLinkedQueue<>(List.of(countries));

            final List<Thread> readerThreads = new ArrayList<>(numThreads);

            for (int i = 0; i < numThreads; ++i) {
                final NominatimImporter threadConnector;
                if (i > 0) {
                    threadConnector = new NominatimImporter(postgresqlConfig, dbProperties);
                    threadConnector.loadCountryNames(dbProperties.getLanguages());
                } else {
                    threadConnector = connector;
                }
                final int threadno = i;
                Runnable runner = () -> {
                    String nextCc = todolist.poll();
                    while (nextCc != null) {
                        LOGGER.info("Thread {}: reading country '{}'", threadno, nextCc);
                        threadConnector.readCountry(nextCc, importThread);
                        nextCc = todolist.poll();
                    }
                };
                Thread thread = new Thread(runner);
                thread.start();
                readerThreads.add(thread);
            }
            readerThreads.forEach(t -> {
                while (true) {
                    try {
                        t.join();
                        break;
                    } catch (InterruptedException e) {
                        LOGGER.warn("Thread interrupted:", e);
                        // Restore interrupted state.
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
        return connector.getLastImportDate();
    }

    @Nullable
    private static Date importFromFile(ImportFileConfig importFileConfig, ImportFilterConfig args, ImportThread importerThread) throws IOException {
        JsonReader reader;
        final String importFile = importFileConfig.getImportFile();
        if ("-".equals(importFile)) {
            reader = new JsonReader(System.in);
        } else {
            reader = new JsonReader(new File(importFile));
        }

        reader.setUseFullGeometries(args.getImportGeometryColumn());
        reader.setExtraTags(args.getExtraTags());
        reader.setCountryFilter(args.getCountryCodes());
        reader.setLanguages(args.getLanguages());

        reader.readHeader();
        final var importDate = reader.getImportDate();

        reader.readFile(importerThread);

        return importDate;
    }


    private static void startNominatimUpdateInit(PostgresqlConfig pgConfig, UpdateInitConfig initConfig) {
        NominatimUpdater nominatimUpdater = new NominatimUpdater(pgConfig, new DatabaseProperties());
        if (initConfig.getImportUser() == null) {
            throw new UsageException("-import-user required for update initialisation");
        }
        nominatimUpdater.initUpdates(initConfig.getImportUser());
    }

    private static void startNominatimUpdate(NominatimUpdater nominatimUpdater, Server esServer) {
        nominatimUpdater.update();

        try {
            DatabaseProperties dbProperties = esServer.loadFromDatabase();
            Date importDate = nominatimUpdater.getLastImportDate();
            dbProperties.setImportDate(importDate);
            esServer.saveToDatabase(dbProperties);
        } catch (IOException e) {
            throw new UsageException("Cannot setup index, elastic search config files not readable");
        }
    }


    /**
     * Prepare Nominatim updater.
     */
    private static NominatimUpdater setupNominatimUpdater(PostgresqlConfig postgresqlConfig,
                                                          ImportFilterConfig importFilterConfig,
                                                          Server server) throws IOException {
        // Get database properties and ensure that the version is compatible.
        DatabaseProperties dbProperties = server.loadFromDatabase();

        if (importFilterConfig.isExtraTagsSet()) {
            dbProperties.putConfigExtraTags(importFilterConfig.getExtraTags());
        }

        NominatimUpdater nominatimUpdater = new NominatimUpdater(postgresqlConfig, dbProperties);
        nominatimUpdater.setUpdater(server.createUpdater(dbProperties));
        return nominatimUpdater;
    }

    /**
     * Start API server to accept search requests via http.
     */
    private static void startApi(ApiServerConfig args, Server server, @Nullable NominatimUpdater updater) throws IOException {
        // Get database properties and ensure that the version is compatible.
        DatabaseProperties dbProperties = server.loadFromDatabase();

        // Update the index settings in case there are any changes.
        server.updateIndexSettings(args.getSynonymFile());
        server.refreshIndexes();

        LOGGER.info("""
                        Starting API with the following settings:
                        
                         Languages: {}
                         Import Date: {}
                         Support Structured Queries: true
                         Support Geometries: {}""",
                dbProperties.getLanguages(), dbProperties.getImportDate(), dbProperties.getSupportGeometries());

        MetricsConfig metrics = setupMetrics(args.getMetrics(), server.getClient());

        photonServer = Javalin.create(config -> {
            config.router.ignoreTrailingSlashes = true;
            config.http.defaultContentType = ContentType.APPLICATION_JSON.toString();
            if (metrics.isEnabled()) {
                config.registerPlugin(metrics.getPlugin());
            }
            if (args.isCorsAnyOrigin() || args.getCorsOrigin().length > 0) {
                config.bundledPlugins.enableCors(cors -> {
                    if (args.isCorsAnyOrigin()) {
                        cors.addRule(it -> {
                            it.anyHost();
                            it.defaultScheme = "http";
                        });
                        cors.addRule(it -> {
                            it.anyHost();
                            it.defaultScheme = "https";
                        });
                    } else {
                        for (var host : args.getCorsOrigin()) {
                            LOGGER.info("Adding cors for {}", host);
                            if (host.startsWith("http")) {
                                cors.addRule(r -> r.allowHost(host));
                            } else {
                                cors.addRule(r -> r.allowHost("http://" + host, "https://" + host));
                            }
                        }
                    }
                });
            }
            config.validation.register(TagFilter.class, TagFilter::buildOsmTagFilter);
            config.validation.register(Envelope.class, BoundingBoxParamConverter::apply);
            config.validation.register(Boolean.class, b -> {
                if (b == null) {
                    return null;
                }
                final var lower = b.toLowerCase();
                return "1".equals(lower) || "yes".equals(lower) || "true".equals(lower);
            });
            config.validation.register(Double.class, s -> {
                if (s != null) {
                    final var d = Double.parseDouble(s);
                    if (Double.isNaN(d)) {
                        throw new NumberFormatException();
                    }
                    return d;
                }
                return null;
            });
        });


        final var formatter = new GeocodeJsonFormatter();

        photonServer.exception(Exception.class, (e, ctx) ->
                ctx.status(400)
                        .result(formatter.formatError(e.getMessage()))
        );
        photonServer.exception(BadRequestException.class, (e, ctx) ->
                ctx.status(e.getHttpStatus())
                        .result(formatter.formatError(e.getMessage()))
        );

        photonServer.events(event -> event.serverStopped(() -> {
            final Server temp = esServer.getAndSet(null);
            if (temp != null) {
                LOGGER.info("Server has been stopped. Shutting down node.");
                temp.shutdown();
            }
        }));

        photonServer.get("/status", new StatusRequestHandler(server));

        photonServer.get("/api", new GenericSearchHandler<>(
                new SimpleSearchRequestFactory(
                        Arrays.stream(dbProperties.getLanguages()).collect(Collectors.toList()),
                        args.getDefaultLanguage(),
                        args.getMaxResults(),
                        dbProperties.getSupportGeometries()),
                server.createSearchHandler(args.getQueryTimeout()),
                formatter));

        photonServer.get("/structured", new GenericSearchHandler<>(
                new StructuredSearchRequestFactory(
                        Arrays.stream(dbProperties.getLanguages()).collect(Collectors.toList()),
                        args.getDefaultLanguage(),
                        args.getMaxResults(),
                        dbProperties.getSupportGeometries()),
                server.createStructuredSearchHandler(args.getQueryTimeout()),
                formatter));

        photonServer.get("/reverse", new GenericSearchHandler<>(
                new ReverseRequestFactory(
                        Arrays.stream(dbProperties.getLanguages()).collect(Collectors.toList()),
                        args.getDefaultLanguage(),
                        args.getMaxReverseResults(),
                        dbProperties.getSupportGeometries()),
                server.createReverseHandler(args.getQueryTimeout()),
                formatter));


        if (updater != null) {
            // setup update API
            if (!updater.isSetUpForUpdates()) {
                throw new UsageException("Update API enabled, but Nominatim database is not prepared. Run -nominatim-update-init-for first.");
            }

            photonServer.get("/nominatim-update/status", ctx ->
                    ctx.status(200).json(updater.isBusy() ? "BUSY" : "OK")
            );
            photonServer.get("/nominatim-update", ctx -> {
                new Thread(() -> App.startNominatimUpdate(updater, server)).start();
                ctx.status(200).json("nominatim update started (more information in console output) ...");
            });
        }

        if (metrics.isEnabled()) {
            photonServer.get(metrics.getPath(), ctx -> {
                String contentType = "text/plain; version=0.0.4; charset=utf-8";
                ctx.contentType(contentType).result(metrics.getRegistry().scrape());
            });
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown requested.");
            shutdown();
        }));

        photonServer.start(args.getIp(), args.getPort());
    }
}
