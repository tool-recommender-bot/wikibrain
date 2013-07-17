package org.wikapidia.dao.load;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.RawPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.lucene.LuceneIndexer;
import org.wikapidia.lucene.LuceneOptions;
import org.wikapidia.utils.ParallelForEach;
import org.wikapidia.utils.Procedure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This loader indexes raw pages into the lucene index.
 * It should not be called sooner than the WikiTextLoader,
 * but where after that I am not sure.
 *
 * @author Ari Weiland
 *
 */
public class LuceneLoader {
    private static final Logger LOG = Logger.getLogger(LuceneLoader.class.getName());

    private final RawPageDao rawPageDao;
    private final LuceneIndexer luceneIndexer;
    private final Collection<NameSpace> namespaces;

    public LuceneLoader(RawPageDao rawPageDao, LuceneIndexer luceneIndexer, Collection<NameSpace> namespaces) {
        this.rawPageDao = rawPageDao;
        this.luceneIndexer = luceneIndexer;
        this.namespaces = namespaces;
    }

    public void load(Language language) throws WikapidiaException {
        try {
            int i = 0;
            Iterable<RawPage> rawPages = rawPageDao.get(new DaoFilter()
                    .setLanguages(language)
                    .setNameSpaces(namespaces)
                    .setRedirect(false));
            for (RawPage rawPage : rawPages) {
                luceneIndexer.indexPage(rawPage);
                i++;
                if (i%1000 == 0) LOG.log(Level.INFO, "RawPages indexed: " + i);
            }
        } catch (DaoException e) {
            throw new WikapidiaException(e);
        }
    }

    public void endLoad() {
        luceneIndexer.close();
    }

    public static void main(String args[]) throws ConfigurationException, WikapidiaException, IOException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("drop-indexes")
                        .withDescription("drop and recreate all indexes")
                        .create("d"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArgs()
                        .withValueSeparator(',')
                        .withLongOpt("namespaces")
                        .withDescription("the set of namespaces to index, separated by commas")
                        .create("p"));
        Env.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("LuceneLoader", options);
            return;
        }

        Env env = new Env(cmd);
        Configurator conf = env.getConfigurator();
        LuceneOptions luceneOptions = conf.get(LuceneOptions.class, "options");

        LanguageSet languages = env.getLanguages();
        Collection<NameSpace> namespaces = new ArrayList<NameSpace>();
        if (cmd.hasOption("p")) {
            String[] nsStrings = cmd.getOptionValues("p");
            for (String s : nsStrings) {
                namespaces.add(NameSpace.getNameSpaceByName(s));
            }
        } else {
            namespaces = luceneOptions.namespaces;
        }
        File luceneRoot = luceneOptions.luceneRoot;

        RawPageDao rawPageDao = conf.get(RawPageDao.class);
        LuceneIndexer luceneIndexer = new LuceneIndexer(languages, luceneRoot);

        final LuceneLoader loader = new LuceneLoader(rawPageDao, luceneIndexer, namespaces);

        if (cmd.hasOption("d")) {
            LOG.log(Level.INFO, "Dropping indexes");
            for (String langCode : languages.getLangCodes()) {
                File lang = new File(luceneRoot, langCode);
                if (lang.exists()) {
                    FileUtils.forceDelete(lang);
                }
            }
        }

        LOG.log(Level.INFO, "Begin indexing");

        // TODO: parallelize by some more efficient method?
        ParallelForEach.loop(
                languages.getLanguages(),
                env.getMaxThreads(),
                new Procedure<Language>() {
                    @Override
                    public void call(Language language) throws Exception {
                        loader.load(language);
                    }
                }
        );

        loader.endLoad();
        LOG.log(Level.INFO, "Done indexing");
    }
}
