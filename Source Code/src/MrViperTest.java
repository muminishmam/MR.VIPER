import org.junit.jupiter.api.Test;
import ca.pfv.spmf.patterns.itemset_array_integers_with_tids_bitset.Itemset;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MrViperTest {

        public static void main(String [] arg) throws IOException {
            //Input and output file paths
            String inputFilePath = "src/t25i10d10k.txt";
            String outputFilePath = "./output.txt";

            // the threshold that we will use:
            double minsup = 0.4;

            // Applying the APRIORI-Inverse algorithm to find sporadic itemsets
            MrViper apriori2 = new MrViper();
            // apply the algorithm
            apriori2.run(minsup, inputFilePath, outputFilePath);

        }

}