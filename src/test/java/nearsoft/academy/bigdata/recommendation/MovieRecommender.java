package nearsoft.academy.bigdata.recommendation;

// Fix wild imports.
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.ArrayList;
import java.util.List;
import java.io.PrintWriter;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

public class MovieRecommender {
  public int totalProducts;
  public int totalUsers;
  public int totalReviews;
  public Map<String,Integer> productsMap = new HashMap<String,Integer>();
  public Map<String,Integer> usersMap = new HashMap<String,Integer>();

  public static void main(String[] args) {
    // new MovieRecommender();
  }

  public MovieRecommender(String dataFilePath) {
    buildDataModelFile(dataFilePath);
  }

  public void buildDataModelFile(String path) {
    String line;
    try {
      BufferedReader buffered;
      ArrayList<String> products = new ArrayList<String>();
      ArrayList<String> users = new ArrayList<String>();
      ArrayList<Double> ratings = new ArrayList<Double>();

      buffered = readGzippedFile(path);
      while((line = buffered.readLine()) != null) {
        if(line.contains("product/productId: ")) {
          String[] split = line.split("product/productId: ");
          products.add(split[1]);
        } else if(line.contains("review/userId: ")) {
          String[] split = line.split("review/userId: ");
          users.add(split[1]);
        }else if(line.contains("review/score: ")) {
          String[] split = line.split("review/score: ");
          ratings.add(Double.parseDouble(split[1]));
          this.totalReviews++;
        }
      }
      this.productsMap = buildAndSetMap(products);
      this.usersMap = buildAndSetMap(users);
      this.totalProducts = productsMap.size();
      this.totalUsers = usersMap.size();

      writeToDataModelFile("./dataModel.csv", products, users, ratings);
    } catch(IOException e) {
      System.out.println("Exception reading line.");
    }
  }

  public static BufferedReader readGzippedFile(String filePath) throws IOException, FileNotFoundException {
    InputStream fileStream = new FileInputStream(filePath);
    InputStream gzipStream = new GZIPInputStream(fileStream);
    Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
    BufferedReader buffered = new BufferedReader(decoder);
    return buffered;
  }

  public Map<String,Integer> buildAndSetMap(ArrayList<String> list) {
    Map<String,Integer> map = new HashMap<String,Integer>();
    int id = 1;
    for(String s:list) {
      if(!map.containsKey(s)) {
        map.put(s, id);
        id++;
      }
    }
    return map;
  }

  public void writeToDataModelFile(String filePath, ArrayList<String>prod, ArrayList<String> usr, ArrayList<Double> rat) {
    try {
      PrintWriter pw = new PrintWriter(new File(filePath));

      for(int i = 0; i < prod.size(); i++) {
        // String lineToPrint = String.format("%s,%s,%s", usr.get(i), productsMap.get(prod.get(i)), rat.get(i));
        // pw.write(lineToPrint);
        pw.write(usersMap.get(usr.get(i))+","+productsMap.get(prod.get(i))+","+rat.get(i));
        pw.println();
      }

      pw.close();
    } catch(IOException e) {

    }
  }

  public int getTotalReviews() {
    return this.totalReviews;
  }

  public int getTotalProducts() {
    return this.totalProducts;
  }

  public int getTotalUsers() {
    return this.totalUsers;
  }

  public List<String> getRecommendationsForUser(String userId) {
    List<String> recs = new ArrayList<String>();
    DataModel model = null;
  	UserSimilarity similarity = null;
  	List<RecommendedItem> recommendations = null;

    try {
      model = new FileDataModel(new File("dataModel.csv"));
    } catch(IOException e) {

    }

    try {
      similarity = new PearsonCorrelationSimilarity(model);
    } catch(TasteException e) {

    }

    UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);
    UserBasedRecommender recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);

    try {
      recommendations = recommender.recommend(usersMap.get(userId), 3);
    } catch(TasteException e) {

    }

    for(RecommendedItem recommendation : recommendations) {
      String rec = getKeyFromProdMap((int) recommendation.getItemID());
      recs.add(rec);
    }

    return recs;

  }

  public String getKeyFromProdMap(int value) {
    for(String key: productsMap.keySet()) {
      if(productsMap.get(key) == value) {
        return key;
      }
    }
    return null;
  }
}
