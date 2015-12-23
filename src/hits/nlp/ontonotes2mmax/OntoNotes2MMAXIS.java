/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hits.nlp.ontonotes2mmax;

/**
 *
 * @author houyg
 */
import hits.nlp.ontonotes2mmax.ONFFilter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;

import org.archive.util.FileUtils;

public class OntoNotes2MMAXIS {

    /** The logger */
    protected static final Logger MAIN_LOGGER = Logger.getAnonymousLogger();
    // Directories
    private final File _sourceDir;
    private final File _outDir;
    private final File _targetDir;
    private final File _resourceDir;
    private final File _entityAnnotationDir;
    protected final File _baseDataDir;
    private final File _markableDir;
    // ONF files
    private final List<String> inONFfiles;
    private LinkedHashMap<Integer, String> _tokens;
    private LinkedHashMap<Integer, int[]> _sents;
    private LinkedHashMap<Integer, ArrayList<int[]>> _corefs;
    private int _corefId;
    protected String currentDocNameSpace;

    public OntoNotes2MMAXIS(File sourceDir, File targetDir, File resourceDir, File entityAnnotationDir) {
        this._sourceDir = sourceDir;
        this._resourceDir = resourceDir;
        this._entityAnnotationDir = entityAnnotationDir;

        this._outDir = new File(_sourceDir, "mmax");
        this._baseDataDir = new File(_outDir, "Basedata");
        this._markableDir = new File(_outDir, "markables");

        // reset previous imports
        if (_outDir.exists()) {
            FileUtils.deleteDir(_outDir);
        }
        _outDir.mkdirs();

        if (!_baseDataDir.exists()) {
            _baseDataDir.mkdirs();
        }

        if (!_markableDir.exists()) {
            _markableDir.mkdirs();
        }

        this._targetDir = targetDir;

        this.inONFfiles = new ArrayList<String>();

        // Initialize every layers
        _tokens = new LinkedHashMap<Integer, String>();
        _sents = new LinkedHashMap<Integer, int[]>();
        _corefs = new LinkedHashMap<Integer, ArrayList<int[]>>();

        loadFiles();
        run();
    }

    private void run() {
        int counter = 1;
        _corefId = -1; // starts a new index

        // Here we do the processing...
        try {
            for (String infile : inONFfiles) {
                this.currentDocNameSpace =
                        new File(infile).getName().substring(0, new File(infile).getName().lastIndexOf("."));

                MAIN_LOGGER.info(
                        new StringBuffer().append("Processing file ").
                        append(infile).
                        append(" --- File ").append(counter).append(" of ").
                        append(inONFfiles.size()).toString());

                _tokens = new LinkedHashMap<Integer, String>();
                _sents = new LinkedHashMap<Integer, int[]>();
                _corefs = new LinkedHashMap<Integer, ArrayList<int[]>>();
                parseText(infile);
                writeMMAXFile();
                writeBaseData();
                this.writeMarkables();
                counter++;
            }
            cpResources();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void parseText(String inFile) throws IOException {
        final BufferedReader textFileReader = new BufferedReader(new FileReader(_sourceDir + "/" + inFile));

        int currentSent = -1; // starts from 0
        int currentToken = -1; // starts from 0

        String leaveBlock = "";
        String corefBlock = "";

        boolean tokensTrigger = false;
        boolean corefTrigger = false;

        String line = "";
        while ((line = textFileReader.readLine()) != null) {
            // New sentence
            if (line.startsWith("Plain sentence:")) {
                currentSent++;
                corefTrigger = false; // End the coref block
                parseCorefBlock(corefBlock); // Parse the coref block
                corefBlock = ""; // renew the coref block
                continue;
            } else if (line.startsWith("Leaves:")) {
                tokensTrigger = true;
                continue;
            } else if (line.startsWith("Coreference chains")) {
                int[] partRange = new int[2];
                corefTrigger = true;
                continue;
            }


            if (tokensTrigger) {
                if (line.isEmpty()) {
                    int sentStart;
                    if (currentSent == 0) {
                        sentStart = 0;
                    } else {
                        sentStart = _sents.get(currentSent - 1)[1] + 1;
                    }
                    parseLeaveBlock(currentToken, leaveBlock, sentStart); // parse the last leave
                    int[] sentRange = new int[2];
                    if (currentSent == 0) {
                        sentRange[0] = 0;
                        sentRange[1] = _tokens.size() - 1;
                    } else {
                        sentRange[0] = _sents.get(currentSent - 1)[1] + 1;
                        sentRange[1] = _tokens.size() - 1;
                    }
                    _sents.put(currentSent, sentRange);
                    tokensTrigger = false;
                    leaveBlock = "";
                    continue;
                }
                if (line.matches("\\s+[0-9].*")) // starts a new token, Hence a new leave block
                {
                    int sentStart;
                    if (currentSent == 0) {
                        sentStart = 0;
                    } else {
                        sentStart = _sents.get(currentSent - 1)[1] + 1;
                    }
                    if (!leaveBlock.isEmpty()) {
                        parseLeaveBlock(currentToken, leaveBlock, sentStart); // parse the previous leave
                    }
                    currentToken = _tokens.size(); // then move to the next
                    leaveBlock = "";
                    leaveBlock = leaveBlock + line + "&#10;";
                } else if (!line.matches("-+")) {
                    leaveBlock = leaveBlock + line + "&#10;";
                }
                continue;
            } else if (corefTrigger) {
                if (line.isEmpty()
                        || line.matches("-+")) {
                    continue;
                } else {
                    corefBlock = corefBlock + line + "&#10;";
                }
            }



        }
        parseCorefBlock(corefBlock); // Parse the coref block
        textFileReader.close();
    }

    // Parse the coref block
    private void parseCorefBlock(String block) {
        String[] lines = block.split("&#10;");

        boolean apposTrigger = false;
        for (String line : lines) {
            if (line.matches("\\s+Chain.+APPOS\\)")) {
                apposTrigger = true;
            }

            if (line.matches("\\s+Chain.+IDENT\\)")) {
                _corefId++;
                apposTrigger = false;
                continue;
            }

            if (apposTrigger) {
                continue;
            }
            if (line.matches("\\s+[0-9]+\\.[0-9]+-[0-9]+.+")) {
                String tokenInfo = line.split("^\\s+")[1];
                tokenInfo = tokenInfo.split("\\s+")[0];
                int sent = Integer.valueOf(tokenInfo.split("\\.")[0]);
                int start = Integer.valueOf(tokenInfo.split("\\.")[1].split("-")[0]);
                int end = Integer.valueOf(tokenInfo.split("\\.")[1].split("-")[1]);
                int[] tokenRange = new int[2];
                tokenRange[0] = _sents.get(sent)[0] + start;
                tokenRange[1] = _sents.get(sent)[0] + end;
                if (_corefs.containsKey(_corefId)) {
                    _corefs.get(_corefId).add(tokenRange);
                } else {
                    ArrayList<int[]> tList = new ArrayList<int[]>();
                    tList.add(tokenRange);
                    _corefs.put(_corefId, tList);
                }

            }
        }
    }

    // All the layers of annotations are collected from this parser
    private void parseText1(String inFile) throws IOException {
        final BufferedReader textFileReader = new BufferedReader(new FileReader(_sourceDir + "/" + inFile));

        int currentSent = -1; // starts from 0
        int currentToken = -1; // starts from 0
        int currentPart = 0; // starts from 0

        String leaveBlock = "";

        boolean tokensTrigger = false;

        String line = "";
        while ((line = textFileReader.readLine()) != null) {
            // New sentence
            if (line.startsWith("Plain sentence:")) {
                currentSent++;
                continue;
            } else if (line.startsWith("Leaves:")) {
                tokensTrigger = true;
                continue;
            }

            if (tokensTrigger) {
                if (line.isEmpty()) {
                    int sentStart;
                    if (currentSent == 0) {
                        sentStart = 0;
                    } else {
                        sentStart = _sents.get(currentSent - 1)[1] + 1;
                    }
                    parseLeaveBlock(currentToken, leaveBlock, sentStart); // parse the last leave
                    int[] sentRange = new int[2];
                    if (currentSent == 0) {
                        sentRange[0] = 0;
                        sentRange[1] = _tokens.size() - 1;
                    } else {
                        sentRange[0] = _sents.get(currentSent - 1)[1] + 1;
                        sentRange[1] = _tokens.size() - 1;
                    }
                    _sents.put(currentSent, sentRange);
                    tokensTrigger = false;
                    leaveBlock = "";
                    continue;
                }
                if (line.matches("\\s+[0-9].*")) // starts a new token, Hence a new leave block
                {
                    int sentStart;
                    if (currentSent == 0) {
                        sentStart = 0;
                    } else {
                        sentStart = _sents.get(currentSent - 1)[1] + 1;
                    }
                    if (!leaveBlock.isEmpty()) {
                        parseLeaveBlock(currentToken, leaveBlock, sentStart); // parse the previous leave
                    }
                    currentToken = _tokens.size(); // then move to the next
                    leaveBlock = "";
                    leaveBlock = leaveBlock + line + "&#10;";
                } else if (!line.matches("-+")) {
                    leaveBlock = leaveBlock + line + "&#10;";
                }
                continue;
            }
        }
        textFileReader.close();
    }

    // Parse leave block
    private void parseLeaveBlock(int tokenID, String block, int currentSentStart) {
        String[] lines = block.split("&#10;");
        String token = "";
        for (String line : lines) {
            if (line.matches("\\s+[0-9]+\\s+.+")) {
                if (line.split("\\s+[0-9]+\\s+").length != 2) {
                    break;
                }
                if (line.split("[0-9]+")[0].length() > 4) {
                    int debug = 0;
                    break;
                }
                int extractedId = Integer.valueOf(line.split("^\\s+")[1].split("\\s+")[0]);
                int last_extracedId;
                if (_sents.size() == 0) {
                    last_extracedId = _tokens.size() - 1;
                } else {
                    last_extracedId = _tokens.size() - 1 - _sents.get(_sents.size() - 1)[1] - 1;
                }
                if (extractedId != 0 && (extractedId - 1) != last_extracedId) {
                    int debug = 0;
                    break;
                }
                token = line.split("\\s+[0-9]+\\s+")[1];
            }

        }
        token = token.replace("&", "&amp;");
        token = token.replace("<", "&lt;");
        token = token.replace(">", "&gt;");
        token = token.replace("/", "&#8260;");
        if (!token.isEmpty()) {
            _tokens.put(tokenID, token);
        }
    }

    public void writeMarkables() throws IOException {
        writeSentenceMarkables();
        writeCorefMarkables();
        cpEntityAnnotation();
    }

    private void writeSentenceMarkables() throws IOException {
        final FileWriter sentWriter = new FileWriter(new File(_markableDir, currentDocNameSpace + "_sentence_level.xml"));
        sentWriter.write(
                "<?xml version=\'1.0\'?>\n<!DOCTYPE markables SYSTEM \"markables.dtd\">\n<markables xmlns=\"www.eml.org/NameSpaces/sentence\">\n");

        for (int sentId : _sents.keySet()) {
            int sentStart = _sents.get(sentId)[0];
            int sentEnd = _sents.get(sentId)[1];
            String span = "";
            if (sentStart == sentEnd) {
                span = "word_" + (sentStart + 1);
            } else {
                span = "word_" + (sentStart + 1) + "..word_" + (sentEnd + 1);
            }

            String result = "<markable id=\"markable_" + sentId + "\" span=\"" + span + "\" ";

            result = result + "orderid=\"" + sentId + "\" ";
            result = result + "mmax_level=\"sentence\" />";
            sentWriter.write(result + "\n");
        }
        sentWriter.write("</markables>");

        sentWriter.flush();
        sentWriter.close();
    }

    private void writeCorefMarkables() throws IOException {
        final FileWriter corefWriter = new FileWriter(new File(_markableDir, currentDocNameSpace + "_coref_level.xml"));
        corefWriter.write(
                "<?xml version=\'1.0\'?>\n<!DOCTYPE markables SYSTEM \"markables.dtd\">\n<markables xmlns=\"www.eml.org/NameSpaces/coref\">\n");

        int mId = -1;
        for (int setId : _corefs.keySet()) {
            for (int[] menRange : _corefs.get(setId)) {
                mId++;
                int Start = menRange[0];
                int End = menRange[1];
                String span = "";
                if (Start == End) {
                    span = "word_" + (Start + 1);
                } else {
                    span = "word_" + (Start + 1) + "..word_" + (End + 1);
                }

                String result = "<markable id=\"markable_" + mId + "\" span=\"" + span + "\" ";

                result = result + "coref_set=\"set_" + (setId + 1) + "\" ";
                result = result + "mmax_level=\"coref\" />";
                corefWriter.write(result + "\n");
            }
        }
        corefWriter.write("</markables>");
        corefWriter.flush();
        corefWriter.close();
    }

    public void writeMMAXFile() throws IOException {

        final FileWriter mmaxWriter = new FileWriter(new File(_outDir, currentDocNameSpace + ".mmax"));
        mmaxWriter.write(
                "<?xml version=\"1.0\"?>\n<mmax_project>\n");

        mmaxWriter.write("<words>" + currentDocNameSpace + "_words.xml</words>\n");

        mmaxWriter.write("</mmax_project>\n");

        mmaxWriter.flush();
        mmaxWriter.close();

    }

    private void cpResources() throws IOException {
        FileUtils.copyFile(new File(_resourceDir, "common_paths.xml"), new File(_outDir,
                "common_paths.xml"));
        FileUtils.copyFile(new File(_resourceDir, "markables.dtd"), new File(_markableDir,
                "markables.dtd"));
        FileUtils.copyFile(new File(_resourceDir, "words.dtd"), new File(_baseDataDir,
                "words.dtd"));
        FileUtils.copyFiles(new File(_resourceDir, "common"), new File(_outDir, "common"));  //new File("mmax-common")

        if (_targetDir != null) {
            if (_targetDir.exists()) {
                FileUtils.deleteDir(_targetDir);
            }
            _targetDir.mkdirs();
            FileUtils.copyFiles(_outDir, _targetDir);
            FileUtils.deleteDir(_outDir);
        }
    }

    private void cpEntityAnnotation() throws IOException {
        FileUtils.copyFiles(this._entityAnnotationDir, this._markableDir);  //new File("mmax-common")

    }

    public void writeBaseData() throws IOException {

        if (!_baseDataDir.exists()) {
            _baseDataDir.mkdirs();
        }

        final FileWriter wordWriter = new FileWriter(new File(_baseDataDir, currentDocNameSpace + "_words.xml"));
        wordWriter.write(
                "<?xml version=\"1.0\"?>\n<!DOCTYPE words SYSTEM \"words.dtd\">\n<words>\n");

        for (int wId : _tokens.keySet()) {
            wordWriter.write("<word id=\"word_" + (wId + 1) + "\">" + _tokens.get(wId) + "</word>" + "\n");
        }
        wordWriter.write("</words>");
        wordWriter.flush();
        wordWriter.close();

    }

    private void loadFiles() {
        //Load the .onf files
        final File[] inputMmax = _sourceDir.listFiles(getONFFileFilter());
        for (File file : inputMmax) {
            inONFfiles.add(file.getName());
        }
    }

    public FileFilter getONFFileFilter() {
        return new ONFFilter();
    }

    public static void main(String[] args) {
        try {
            if (args.length == 4) {
                File file_ontonotesONFDir = new File(args[0]);
                File file_targetDir = new File(args[1]);
                File file_resourceDir = new File(args[2]);
                File file_ISAnnotationDir = new File(args[3]);
                new OntoNotes2MMAXIS(file_ontonotesONFDir, file_targetDir, file_resourceDir, file_ISAnnotationDir);
            } else {
                System.err.println("please specify correct path!");
            }

//            new OntoNotes2MMAXIS(
//                    new File("/data/nlp/houyg/corpora/ISRelease/OntoNotesOriginal"),
//                    new File("/data/nlp/houyg/corpora/ISRelease/withCoref"),
//                    new File("/data/nlp/houyg/corpora/ISRelease/resourceFile"),
//                    new File("/data/nlp/houyg/corpora/ISRelease/ISAnnotationWithTrace"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
