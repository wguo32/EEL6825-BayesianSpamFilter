import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class SpamFilter {

    HashMap<String, Integer> spamWords = new HashMap<String, Integer>();    //hashmap to keep track of the words and their occurrences in the spam training set
    HashMap<String, Integer> hamWords = new HashMap<String, Integer>();
    HashMap<String, Double> spamicity = new HashMap<String, Double>();
    List<String> stopWords = new ArrayList<String>();

    private int spamCount = 0;
    private int hamCount = 0;


    //train folder
    public void train(String fileFolder) {
        stopWords = loadStopWords();
        List<Comment> rtn = retriveCommentByFolder(fileFolder);
        for (Comment comment : rtn) {
            if (comment.spam) {
                spamCount++;
            } else {
                hamCount++;
            }
            List<String> list = segmentateComment(comment.content);
            ArrayList<String> usedWords = new ArrayList<String>();
            for (String text : list) {
                if (text.length() > 0) {
                    if (usedWords.contains(text) == false) {
                        usedWords.add(text);
                    }
                }
            }

            for (String word : usedWords) {
                insertIntoHashMap(word, comment.spam ? spamWords : hamWords);
            }

        }
        updateSpamliness();


    }

    //test folder
    public void test(String fileFolder) {
        List<Comment> rtn = retriveCommentByFolder(fileFolder);
        int tt = 0, ff = 0, tf = 0, ft = 0;
        for (Comment comment : rtn) {
            if (comment.content != null && !"".equals(comment.content)) {
                double spc = getSpamcity(comment);
                Boolean predict = isSpam(spc);
                Boolean actual = comment.spam;
                if (predict && actual) {
                    tt++;
                }
                if (!predict && !actual) {
                    ff++;
                }
                if (predict && !actual) {
                    tf++;
                }
                if (!predict && actual) {
                    ft++;
                }
                System.out.println(comment.comment_id + " pred:" + predict + " actual:" + comment.spam + "  spamicity=" + spc + "   " + comment.content);
            }

        }
        System.out.println("TT=" + tt + " TF=" + tf + " FF=" + ff + " FT=" + ft);

    }

    public void updateSpamliness() {

        for (Map.Entry<String, Integer> e : hamWords.entrySet()) {
            double probwh = (double) e.getValue() / hamCount;
            double probws = 0;
            if (spamWords.containsKey(e.getKey()))
                probws = (double) spamWords.get(e.getKey()) / spamCount;

            double probspam = probws / (probws + probwh);
            spamicity.put(e.getKey(), probspam);
        }

        for (Map.Entry<String, Integer> e : spamWords.entrySet()) {
            if (!spamicity.containsKey(e.getKey())) {
                double probws = (double) e.getValue() / spamCount;
                double probwh = 0;

                double probspam = probws / (probws + probwh);
                spamicity.put(e.getKey(), Math.abs(probspam));
            }
        }
    }

    //Takes a word and inserts it into one of the given training hashmaps
    public static void insertIntoHashMap(String word, HashMap<String, Integer> hmap) {
        int count = 0;
        if (hmap.get(word) != null) {
            count = hmap.get(word);
            count++;
            hmap.put(word, count);
        } else
            hmap.put(word, 1);
    }

    public List<Comment> retriveCommentByFolder(String folder) {
        List<Comment> rtn = new ArrayList<Comment>();
        File ffolder = new File(folder);
        if (ffolder.isDirectory()) {
            File[] files = ffolder.listFiles();
            for (File file : files) {
                rtn.addAll(retriveAll(file));
            }
        }
        return rtn;
    }

    public List<Comment> retriveAll(File csvFile) {

        List<Comment> rtn = new ArrayList<Comment>();
        BufferedReader br = null;
        String line = "";
        // use comma as separator
        String cvsSplitBy = ",";
        int iteration = 0;
        try {
            Comment comment;
            String[] obj = null;
            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {
                //Skip the first line
                if (iteration == 0) {
                    iteration++;
                    continue;
                }
                obj = line.split(cvsSplitBy);
                if (obj.length == 5) {
                    String b = obj[4];
                    comment = new Comment(obj[0], obj[1], obj[2], obj[3], "1".equals(b) ? Boolean.TRUE : Boolean.FALSE);
                    rtn.add(comment);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return rtn;
    }

    public boolean isSpam(double messageSpamicity) {
        if (messageSpamicity > 0.5) {
            return true;
        } else {
            return false;
        }
    }

    public double getSpamcity(Comment comment) {
        double n = 0;
        ArrayList<String> msgs = segmentateComment(comment.content);
        for (String msg : msgs) {
            if (spamicity.get(msg) != null) {
                double prob = (double) spamicity.get(msg);
                if (prob > 0 && prob < 1)
                    n += Math.log(1d - prob) - Math.log(prob);
            }
        }

        double messageSpamicity = (double) 1.0 / (1.0 + Math.exp(n));
        comment.spamicity = messageSpamicity;
        return messageSpamicity;
    }

    private ArrayList<String> segmentateComment(String text) {
        ArrayList<String> tokens = new ArrayList<String>();
        text = text.replaceAll("\\<.*?>", "");
        text = text.replaceAll("&amp", " ");
        text = text.replaceAll("&lt;", " ");
        text = text.replaceAll("&gt;", " ");
        text = text.replaceAll("[^a-zA-Z]", " ");

        for (String token : text.split("[ ,.-]+")) {
            tokens.add(token.toLowerCase());
        }
        tokens.removeAll(stopWords);
        return tokens;
    }

    private List<String> loadStopWords() {
        ArrayList<String> words = new ArrayList<String>();
        String path = this.getClass().getResource(".").getPath();
        File file = new File(path + "/stopwords_en.txt");
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                words.add(line);
            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return words;
    }


    public static void main(String[] args) {
        SpamFilter sf = new SpamFilter();
        sf.train("/Volumes/wspace/YouTube-Spam-Collection-v1/train");
        sf.test("/Volumes/wspace/YouTube-Spam-Collection-v1/test");
    }

}
