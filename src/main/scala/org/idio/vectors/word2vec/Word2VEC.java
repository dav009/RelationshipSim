package org.idio.vectors.word2vec; /**
 * Created with IntelliJ IDEA.
 * User: dav009
 * Date: 12/09/2014
 * Time: 13:56
 * To change this template use File | Settings | File Templates.
 */


import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class Word2VEC {

    public static void main(String[] args) throws IOException {
        Word2VEC vec = new Word2VEC();
        vec.loadModel("vectors.bin");
//	 System.out.println(vec.distance("ç”·äºº"));

        //ç”·äºº å›½çŽ‹ å¥³äºº
    }

    private HashMap<String, float[]> wordMap = new HashMap<String, float[]>();

    private int words;
    private int size;
    private int topNSize = 40;

    /**
     * åŠ è½½æ¨¡åž‹
     *
     * @param path
     *            æ¨¡åž‹çš„è·¯å¾„
     * @throws IOException
     */
    public void loadModel(String path) throws IOException {
        DataInputStream dis = null;
        BufferedInputStream bis = null;
        double len = 0;
        float vector = 0;
        try {
            bis = new BufferedInputStream(new FileInputStream(path));
            dis = new DataInputStream(bis);
            // //è¯»å–è¯æ•°
            words = Integer.parseInt(readString(dis));
            // //å¤§å°
            size = Integer.parseInt(readString(dis));

            String word;
            float[] vectors = null;
            for (int i = 0; i < words; i++) {
                word = readString(dis);
                vectors = new float[size];
                len = 0;
                for (int j = 0; j < size; j++) {
                    vector = readFloat(dis);
                    len += vector * vector;
                    vectors[j] = (float) vector;
                }
                len = Math.sqrt(len);

                for (int j = 0; j < vectors.length; j++) {
                    vectors[j] = (float) (vectors[j] / len);
                }
                wordMap.put(word, vectors);
                dis.read();
            }

        } finally {
            bis.close();
            dis.close();
        }
    }

    private static final int MAX_SIZE = 50;








    /**
     * å¾—åˆ°è¯å‘é‡
     *
     * @param word
     * @return
     */
    public float[] getWordVector(String word) {
        return wordMap.get(word);
    }

    public static float readFloat(InputStream is) throws IOException {
        byte[] bytes = new byte[4];
        is.read(bytes);
        return getFloat(bytes);
    }

    /**
     * è¯»å–ä¸€ä¸ªfloat
     *
     * @param b
     * @return
     */
    public static float getFloat(byte[] b) {
        int accum = 0;
        accum = accum | (b[0] & 0xff) << 0;
        accum = accum | (b[1] & 0xff) << 8;
        accum = accum | (b[2] & 0xff) << 16;
        accum = accum | (b[3] & 0xff) << 24;
        return Float.intBitsToFloat(accum);
    }

    /**
     * è¯»å–ä¸€ä¸ªå­—ç¬¦ä¸²
     *
     * @param dis
     * @return
     * @throws IOException
     */
    private static String readString(DataInputStream dis) throws IOException {
        // TODO Auto-generated method stub
        byte[] bytes = new byte[MAX_SIZE];
        byte b = dis.readByte();
        int i = -1;
        StringBuilder sb = new StringBuilder();
        while (b != 32 && b != 10) {
            i++;
            bytes[i] = b;
            b = dis.readByte();
            if (i == 49) {
                sb.append(new String(bytes));
                i = -1;
                bytes = new byte[MAX_SIZE];
            }
        }
        sb.append(new String(bytes, 0, i + 1));
        return sb.toString();
    }

    public int getTopNSize() {
        return topNSize;
    }

    public void setTopNSize(int topNSize) {
        this.topNSize = topNSize;
    }

    public HashMap<String, float[]> getWordMap() {
        return wordMap;
    }

    public int getWords() {
        return words;
    }

    public int getSize() {
        return size;
    }


}