package Shared.multipart;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Some weird task thingy that returns XML after it has been fully processed.
 */
public class Task implements Serializable {

    private static final long serialVersionUID = -1150200993464407844L;

    private int file_id;
    public int getFileId(){
        return file_id;
    }

    private boolean doWordCount;
    private boolean doFreqWord;
    private boolean doAvgWordLen;

    private boolean complete = false;

    public int wordCount;
    public String freqWord;
    public int avgWordLen;

    public HashMap<String, Integer> wordCountRank = new HashMap<>();

    private String result;

    public String getXML(){
        return result;
    }

    public boolean isComplete(){
        return complete;
    }

    public Task(int file_id, boolean doWordCount, boolean doFreqWord, boolean doAvgWordLen) {
        this.file_id=file_id;
        this.doWordCount = doWordCount;
        this.doFreqWord = doFreqWord;
        this.doAvgWordLen = doAvgWordLen;
    }

    public void doAllRegistredProcessing(String data) {
        String s = "<output>";
        if (doWordCount) {
            String[] split = data.split(" ");
            wordCount = split.length;// sdf sdf sdf - 3
            s += "<WordCount>" + wordCount + "</WordCount>";
        }
        if (doFreqWord) {
            String mostRepeated = "";
            int count = 0;
            String[] split = data.split(" ");
            for (String word : split)
                if (wordCountRank.containsKey(word))
                    wordCountRank.replace(word, wordCountRank.get(word) + 1);
                else
                    wordCountRank.put(word, 1);
            for (String iter : wordCountRank.keySet())
                if (wordCountRank.get(iter) > count) {
                    count = wordCountRank.get(iter);
                    mostRepeated = iter;
                }
            freqWord = mostRepeated;
            s += "<WordCount>" + mostRepeated + "</WordCount>";
        }
        if (doAvgWordLen) {
            int count = 0;
            int sum = 0;
            String[] split = data.split(" ");
            for (String word : split) {
                sum += word.length();
                count++;
            }
            avgWordLen = sum / count;
            s += "<WordCount>" + avgWordLen + "</WordCount>";
        }
        s += "</output>";
        complete = true;
        result = s;
    }

}