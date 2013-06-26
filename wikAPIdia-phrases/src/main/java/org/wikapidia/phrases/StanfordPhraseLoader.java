package org.wikapidia.phrases;

import org.apache.commons.lang3.math.Fraction;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;
import org.wikapidia.phrases.dao.PhraseAnalyzerDao;
import org.wikapidia.utils.CompressedFile;


import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.logging.Logger;

/**
 * Loads phrase to page files from Indexes files from
 * http://www-nlp.stanford.edu/pubs/crosswikis-data.tar.bz2/
 * into a PhraseAnalyzer
 *
 * These files capture anchor text associated with web pages that link to Wikipedia.
 * Note that the pages with anchor text are not (usually) Wikipedia pages themselves.
 */
public class StanfordPhraseLoader {
    private static final Logger LOG = Logger.getLogger(StanfordPhraseLoader.class.getName());
    private static final LanguageInfo EN = LanguageInfo.getByLangCode("en");

    private PhraseAnalyzerDao phraseDao;
    private LocalPageDao pageDao;

    public StanfordPhraseLoader(PhraseAnalyzerDao phraseDao, LocalPageDao pageDao) {
        this.phraseDao = phraseDao;
        this.pageDao = pageDao;
    }

    /**
     * Loads a single Stanford phrase file into the database.
     * This can safely be called for multiple files if it is chunked.
     * @param path
     * @throws IOException
     */
    public void load(File path) throws IOException {
        BufferedReader reader = CompressedFile.open(path);
        long numLines = 0;
        long numLinesRetained = 0;
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            if (++numLines % 100000 == 0) {
                double p = 100.0 * numLinesRetained / numLines;
                LOG.info("processing line: " + numLines +
                        ", retained " + numLinesRetained +
                        "(" + new DecimalFormat("#.#").format(p) + "%)");
            }
            try {
                Entry e = new Entry(line);
                LocalPage lp = pageDao.getByTitle(
                        EN.getLanguage(),
                        new Title(e.article, EN),
                        NameSpace.ARTICLE);
                if (lp != null) {
                    phraseDao.add(EN.getLanguage(), lp.getLocalId(), e.text, e.getNumEnglishLinks());
                    numLinesRetained++;
                }
            } catch (Exception e) {
                LOG.log(Level.FINEST, "Error parsing line " + line + ":", e);
            }
        }
    }


    /**
     * A single  entry corresponding to a line from a
     * dictionary.bz2 at http://www-nlp.stanford.edu/pubs/crosswikis-data.tar.bz2/.
     *
     * Major components of an entry are:
     * - textual phrase
     * - concept (a wikipedia article)
     * - A variety of flags
     */
    private static final Pattern MATCH_ENTRY = Pattern.compile("([^\t]*)\t([0-9.e-]+) ([^ ]*)(| (.*))$");
    class Entry {
        String text;
        float fraction;
        String article;
        String flags[];

        Entry(String line) {
            Matcher m = MATCH_ENTRY.matcher(line);
            if (!m.matches()) {
                throw new IllegalArgumentException("invalid concepts entry: '" + line + "'");
            }
            this.text = m.group(1);
            this.fraction = Float.valueOf(m.group(2));
            this.article = m.group(3);
            this.flags = m.group(4).trim().split(" ");
        }

        int getNumEnglishLinks() {
            for (String flag : flags) {
                if (flag.startsWith("W:")) {
                    return Fraction.getFraction(flag.substring(2)).getNumerator();
                }
            }
            return 0;
        }
    }
}
